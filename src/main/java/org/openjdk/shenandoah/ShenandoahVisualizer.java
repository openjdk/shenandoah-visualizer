 /*
 * Copyright (c) 2021, Amazon.com, Inc. All rights reserved.
 * Copyright (c) 2016, 2020, Red Hat, Inc. All rights reserved.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 *
 */
package org.openjdk.shenandoah;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.concurrent.*;

 class ShenandoahVisualizer {

    private static final int INITIAL_WIDTH = 2000;
    private static final int INITIAL_HEIGHT = 1600;
    static final int KILO = 1024;
    private static final String PLAYBACK = "Playback";
    private static final String REALTIME = "Realtime";

    static int value = 0;

    private static ScheduledFuture<?> changeScheduleInterval(int n, ScheduledExecutorService service, ScheduledFuture<?> f, Runnable task) {
        if (service == null || f == null) return null;
        if (n > 0) {
            boolean res = true;
            if (f != null) {
                res = f.cancel(true);
            }
            f = service.scheduleAtFixedRate(task, 0, n, TimeUnit.MILLISECONDS);
            return f;
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        // Command line argument parsing
        String vmIdentifier = null;
        boolean isReplay = false;
        final String[] filePath = {""};

        int i = 0;
        String arg;
        while (i < args.length && args[i].startsWith("-")) {
            arg = args[i++];
            if (arg.equals("-vm")) {
                if (i < args.length) {
                    vmIdentifier = args[i++];
                } else {
                    System.out.println("-vm requires a vm identifier");
                    return;
                }
            } else if (arg.equals("-logFile")) {
                if (i < args.length) {
                    isReplay = true;
                    filePath[0] = args[i++];
                } else {
                    System.out.println("-logFile requires a file path");
                    return;
                }
            } else {
                System.out.println("ShenandoahVisualizer: Illegal option " + arg);
                System.out.println("Usage: [-vm vmIdentifier] [-logFile filePath]");
                return;
            }
        }
        //

        JFrame frame = new JFrame();
        frame.setLayout(new GridBagLayout());
        frame.setTitle("Shenandoah GC Visualizer");
        frame.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        SnapshotHistory history = new SnapshotHistory();
        final RenderRunner renderRunner;
        ToolbarPanel toolbarPanel = new ToolbarPanel(isReplay);
        int totalSnapshotSize = 0;

        if (isReplay) {
            DataLogProvider data = new DataLogProvider(filePath[0], history);
            totalSnapshotSize = data.getSnapshotsSize();
            toolbarPanel.setSize(totalSnapshotSize);
            toolbarPanel.setSnapshots(data.getSnapshots());
            renderRunner = new RenderRunner(data, frame, toolbarPanel);
            toolbarPanel.setModeField(PLAYBACK);
            toolbarPanel.setEnabledRealtimeModeButton(true);
            toolbarPanel.setFileNameField(filePath[0]);
        } else {
            DataProvider data = new DataProvider(vmIdentifier, history);
            renderRunner = new RenderRunner(data, frame, toolbarPanel);
            toolbarPanel.setModeField(REALTIME);
            toolbarPanel.setEnabledRealtimeModeButton(false);
        }

        // Executors
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        final ScheduledFuture<?>[] f = {service.scheduleAtFixedRate(renderRunner,
                0, renderRunner.isLive ? 100 : 1, TimeUnit.MILLISECONDS)};


        JPanel regionsPanel = new JPanel() {
            public void paint(Graphics g) {
                renderRunner.renderRegions(g);
            }
        };

        JPanel legendPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                renderRunner.renderLegend(g);
            }
        };

        JPanel statusPanel = new StatusPanel(renderRunner);

        JPanel graphPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                renderRunner.renderGraph(g);
            }
        };

        ActionListener realtimeModeButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                DataProvider data = new DataProvider(null, history);
                renderRunner.loadLive(data);
                toolbarPanel.setFileNameField("");
                f[0] = changeScheduleInterval(100, service, f[0], renderRunner);
            }
        };
        toolbarPanel.setRealtimeModeButtonListener(realtimeModeButtonListener);

        ActionListener fileButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser();
                int returnValue = fc.showOpenDialog(null);
                int totalSnapshotSize = 0;
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    filePath[0] = fc.getSelectedFile().getAbsolutePath();
                    try {
                        DataLogProvider data = new DataLogProvider(filePath[0], null);
                        renderRunner.loadPlayback(data);
                        totalSnapshotSize = data.getSnapshotsSize();
                        toolbarPanel.setSize(totalSnapshotSize);
                        toolbarPanel.setSnapshots(data.getSnapshots());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    renderRunner.playback.speed = 1.0;
                    toolbarPanel.setSpeedValue(1.0);
                    toolbarPanel.setFileNameField(filePath[0]);
                    toolbarPanel.setLastActionField("File selected: " + filePath[0]);

                    System.out.println("Selected file: " + filePath[0]);
                    renderRunner.frame.repaint();

                    f[0] = changeScheduleInterval(1, service, f[0], renderRunner);
                }

                int lastSnapshotIndex = totalSnapshotSize - 1;
                toolbarPanel.setEndSnapshotButtonListener((e) -> {if (lastSnapshotIndex > 0) renderRunner.playback.stepForwardSnapshots(lastSnapshotIndex);});
            }
        };
        toolbarPanel.setFileButtonListener(fileButtonListener);

        ActionListener playPauseButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (renderRunner.playback.isPaused) {
                    toolbarPanel.setLastActionField("Play button pressed.");
                } else {
                    toolbarPanel.setLastActionField("Pause button pressed.");
                }
                renderRunner.playback.isPaused = !renderRunner.playback.isPaused;
            }
        };

        int lastSnapshotIndex = totalSnapshotSize - 1;
        KeyAdapter keyShortcutAdapter = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                    renderRunner.playback.stepBackSnapshots(1);
                }
                if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                    renderRunner.playback.stepBackSnapshots(5);
                }
                if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                    if (renderRunner.playback.isPaused) {
                        toolbarPanel.setLastActionField("Play button pressed.");
                    } else {
                        toolbarPanel.setLastActionField("Pause button pressed.");
                    }
                    renderRunner.playback.isPaused = !renderRunner.playback.isPaused;
                }
                if (e.getKeyCode() == KeyEvent.VK_UP) {
                    renderRunner.playback.stepForwardSnapshots(5);
                }
                if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                    renderRunner.playback.stepForwardSnapshots(1);
                }
                if (e.getKeyCode() == KeyEvent.VK_ENTER && lastSnapshotIndex > 0) {
                    renderRunner.playback.stepForwardSnapshots(lastSnapshotIndex);
                }
            }
        };
        toolbarPanel.setPlayPauseButtonListener(playPauseButtonListener);
        // Step back/forward button listeners
        toolbarPanel.setBackButton_1_Listener((ae) -> renderRunner.playback.stepBackSnapshots(1));

        toolbarPanel.setBackButton_5_Listener((ae) -> renderRunner.playback.stepBackSnapshots(5));

        toolbarPanel.setForwardButton_1_Listener((ae) -> renderRunner.playback.stepForwardSnapshots(1));

        toolbarPanel.setForwardButton_5_Listener((ae) -> renderRunner.playback.stepForwardSnapshots(5));

        toolbarPanel.setEndSnapshotButtonListener(e -> {if (lastSnapshotIndex > 0) renderRunner.playback.stepForwardSnapshots(lastSnapshotIndex);});

        ChangeListener sliderListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                int difference = toolbarPanel.currentSliderValue() - renderRunner.playback.getPopupSnapshotsSize();
                if (difference > 0) {
                    renderRunner.playback.stepForwardSnapshots(difference);
                }
                if (difference < 0) {
                    renderRunner.playback.stepBackSnapshots(Math.abs(difference));
                }

            }
        };
        toolbarPanel.setSliderListener(sliderListener);

        // Speed button listeners
        ChangeListener speedSpinnerListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (!toolbarPanel.speedButtonPressed) {
                    double speed = toolbarPanel.getSpeedValue();
                    if (speed != renderRunner.playback.speed) {
                        toolbarPanel.setLastActionField("Changed playback speed to: " + speed);
                        renderRunner.playback.data.setSpeed(speed);
                        renderRunner.playback.speed = speed;
                    }
                }
            }
        };
        toolbarPanel.setSpeedSpinnerListener(speedSpinnerListener);

        ActionListener speed_0_5_Listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toolbarPanel.speedButtonPressed = true;
                double speed = Math.max(0.1, renderRunner.playback.speed * 0.5);
                renderRunner.playback.data.setSpeed(speed);
                renderRunner.playback.speed = speed;
                toolbarPanel.setSpeedValue(speed);
                toolbarPanel.speedButtonPressed = false;
                toolbarPanel.setLastActionField("Multiplied speed by 0.5x. Min speed = 0.1");
            }
        };
        toolbarPanel.setSpeed_0_5_Listener(speed_0_5_Listener);

        ActionListener speed_2_Listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                toolbarPanel.speedButtonPressed = true;
                double speed = Math.min(10.0, renderRunner.playback.speed * 2);
                renderRunner.playback.data.setSpeed(speed);
                renderRunner.playback.speed = speed;
                toolbarPanel.setSpeedValue(speed);
                toolbarPanel.speedButtonPressed = false;
                toolbarPanel.setLastActionField("Multiplied speed by 2x. Max speed = 10.0");
            }
        };
        toolbarPanel.setSpeed_2_Listener(speed_2_Listener);

        ActionListener resetSpeedListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (renderRunner.playback.speed != 1) {
                    toolbarPanel.speedButtonPressed = true;
                    renderRunner.playback.data.setSpeed(1.0);
                    renderRunner.playback.speed = 1.0;
                    toolbarPanel.setSpeedValue(1.0);
                    toolbarPanel.speedButtonPressed = false;
                    toolbarPanel.setLastActionField("Speed reset to 1.0");
                }
            }
        };
        toolbarPanel.setResetSpeedListener(resetSpeedListener);
        final int[] regionWidth = new int[1];
        final int[] regionHeight = new int[1];
        regionsPanel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                regionWidth[0] = ev.getComponent().getWidth();
                regionHeight[0] = ev.getComponent().getHeight();
                renderRunner.notifyRegionResized(ev.getComponent().getWidth(), ev.getComponent().getHeight());
            }
        });
        final boolean isReplayFinal = isReplay;
        regionsPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Snapshot snapshot;
                if (isReplayFinal) {
                    snapshot = renderRunner.playback.snapshot;
                } else {
                    snapshot = renderRunner.live.snapshot;
                }
                int area = regionWidth[0] * regionHeight[0];
                int sqSize = Math.max(1, (int) Math.sqrt(1D * area / snapshot.regionCount()));
                int cols = regionWidth[0] / sqSize;
                int regionNumber = (e.getX() / sqSize) + ((e.getY() / sqSize) * cols) ;
                if (regionNumber >= 0 && regionNumber < snapshot.statsSize()) {
                    RegionPopUp popup = new RegionPopUp(regionNumber);
                    popup.setSize(450, 450);
                    popup.setLocation(e.getX(), e.getY());
                    popup.setVisible(true);
                    popup.addKeyListener(keyShortcutAdapter);
                    popup.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            super.windowClosing(e);
                            popup.setVisible(false);
                            popup.dispose();
                            renderRunner.deletePopup(popup);
                        }
                    });
                    renderRunner.addPopup(popup);
                }

            }

        });

        graphPanel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                renderRunner.notifyGraphResized(ev.getComponent().getWidth(), ev.getComponent().getHeight());
            }
        });

        Insets pad = new Insets(10, 10, 10, 10);

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 3;
            c.weighty = 3;
            c.insets = pad;
            frame.add(graphPanel, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 3;
            c.weighty = 4;
            c.insets = pad;
            c.gridheight = GridBagConstraints.RELATIVE;
            frame.add(regionsPanel, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 2;
            c.insets = pad;
            c.weightx = 3;
            frame.add(toolbarPanel, c);
        }

        {
            statusPanel.setPreferredSize(new Dimension(25, 175));
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 3.5;
            c.insets = pad;
            frame.add(statusPanel, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 1;
            c.weighty = 1;
            c.insets = pad;
            c.gridheight = GridBagConstraints.REMAINDER;
            frame.add(legendPanel, c);
        }

        toolbarPanel.addKeyListener(keyShortcutAdapter);
        toolbarPanel.setFocusable(true);
        toolbarPanel.requestFocusInWindow();

        frame.setVisible(true);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                f[0].cancel(true);
                service.shutdown();
                frame.dispose();
            }
        });
        f[0].get();
    }
 }


