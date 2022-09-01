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

import org.HdrHistogram.Histogram;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import static org.openjdk.shenandoah.RegionState.*;

class ShenandoahVisualizer {

    private static final int INITIAL_WIDTH = 2000;
    private static final int INITIAL_HEIGHT = 1600;
    private static final int KILO = 1024;
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

        final RenderRunner renderRunner;
        ToolbarPanel toolbarPanel = new ToolbarPanel(isReplay);
        int totalSnapshotSize = 0;

        if (isReplay) {
            DataLogProvider data = new DataLogProvider(filePath[0]);
            totalSnapshotSize = data.getSnapshotsSize();
            toolbarPanel.setSize(totalSnapshotSize);
            toolbarPanel.setSnapshots(data.getSnapshots());
            renderRunner = new RenderRunner(data, frame, toolbarPanel);
            toolbarPanel.setModeField(PLAYBACK);
            toolbarPanel.setEnabledRealtimeModeButton(true);
            toolbarPanel.setFileNameField(filePath[0]);
        } else {
            DataProvider data = new DataProvider(vmIdentifier);
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

        JPanel statusPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                renderRunner.renderStats(g);
            }
        };

        JPanel graphPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                renderRunner.renderGraph(g);
            }
        };

        ActionListener realtimeModeButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                DataProvider data = new DataProvider(null);
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
                        DataLogProvider data = new DataLogProvider(filePath[0]);
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

    public abstract static class Render {
        public static final int LINE = 15;

        final JFrame frame;

        List<RegionPopUp> popups = new ArrayList<RegionPopUp>();

        int regionWidth, regionHeight;
        int graphWidth, graphHeight;
        final int STEP_X = 4;
        final int phaseLabelWidth = 50;

        final LinkedList<SnapshotView> lastSnapshots;
        final LinkedList<Snapshot> popupSnapshots;
        volatile Snapshot snapshot;

        public Render(JFrame frame) {
            this.frame = frame;
            this.lastSnapshots = new LinkedList<>();
            this.popupSnapshots = new LinkedList<>();
        }

        protected static Color getColor(SnapshotView s) {
            if (s.youngPhase() != Phase.IDLE) {
                return Colors.YOUNG[s.youngPhase().ordinal()];
            }
            if (s.globalPhase() != Phase.IDLE) {
                return Colors.GLOBAL[s.globalPhase().ordinal()];
            }
            if (s.oldPhase() != Phase.IDLE) {
                return Colors.OLD[s.oldPhase().ordinal()];
            }
            return Colors.TIMELINE_IDLE;
        }
        protected static Phase getPhase(SnapshotView s) {
            if (s.youngPhase() != Phase.IDLE) {
                return s.youngPhase();
            }
            if (s.globalPhase() != Phase.IDLE) {
                return s.globalPhase();
            }
            if (s.oldPhase() != Phase.IDLE) {
                return s.oldPhase();
            }
            return Phase.UNKNOWN;
        }

        public abstract void renderGraph(Graphics g);

        public abstract void renderStats(Graphics g);

        public synchronized void renderLegend(Graphics g) {
            final int sqSize = LINE;

            Map<String, RegionStat> items = new LinkedHashMap<>();

            items.put("Empty Uncommitted",
                    new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_UNCOMMITTED));

            items.put("Empty Committed",
                    new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_COMMITTED));

            items.put("Trash",
                    new RegionStat(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, TRASH));

            items.put("TLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, REGULAR));

            items.put("GCLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, REGULAR));

            items.put("PLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, REGULAR));

            items.put("Shared Allocs",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, REGULAR));

            items.put("Humongous",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, HUMONGOUS));

            items.put("Humongous + Pinned",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, PINNED_HUMONGOUS));

            items.put("Collection Set",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, CSET));

            items.put("Pinned",
                    new RegionStat(1.0f, 1.0f, 0.3f, 0.0f, 0.0f, 0.0f, PINNED));

            items.put("Pinned CSet",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, PINNED_CSET));

            items.put("Age [0, 3)", new RegionStat(REGULAR, 0));
            items.put("Age [3, 6)", new RegionStat(REGULAR, 3));
            items.put("Age [6, 9)", new RegionStat(REGULAR, 6));
            items.put("Age [9, 12)", new RegionStat(REGULAR, 9));
            items.put("Age [12, 15)", new RegionStat(REGULAR, 12));
            items.put("Age 15", new RegionStat(REGULAR, 15));

            Map<String, Integer> summaryNumbers = new LinkedHashMap<>();


            summaryNumbers.put("Empty Uncommitted", snapshot.getEmptyUncommittedCount());

            summaryNumbers.put("Empty Committed", snapshot.getEmptyCommittedCount());

            summaryNumbers.put("Trash", snapshot.getTrashCount());

            summaryNumbers.put("TLAB Allocs", snapshot.getTlabCount());

            summaryNumbers.put("GCLAB Allocs", snapshot.getGclabCount());

            summaryNumbers.put("PLAB Allocs", snapshot.getPlabCount());

            summaryNumbers.put("Shared Allocs", snapshot.getSharedCount());

            summaryNumbers.put("Humongous", snapshot.getHumongousCount());

            summaryNumbers.put("Humongous + Pinned", snapshot.getPinnedHumongousCount());

            summaryNumbers.put("Collection Set", snapshot.getCSetCount());

            summaryNumbers.put("Pinned", snapshot.getPinnedCount());


            summaryNumbers.put("Pinned CSet", snapshot.getPinnedCSetCount());

            summaryNumbers.put("Age [0, 3)", snapshot.getAge0Count());
            summaryNumbers.put("Age [3, 6)", snapshot.getAge3Count());
            summaryNumbers.put("Age [6, 9)", snapshot.getAge6Count());
            summaryNumbers.put("Age [9, 12)", snapshot.getAge9Count());
            summaryNumbers.put("Age [12, 15)", snapshot.getAge12Count());
            summaryNumbers.put("Age 15", snapshot.getAge15Count());
            int i = 0;
            for (String key : items.keySet()) {
                int y = (int) (i * sqSize * 1.5);
                items.get(key).render(g, 0, y, sqSize, sqSize);
                g.setColor(Color.BLACK);
                g.drawString(key + " total: " + summaryNumbers.get(key).toString(), (int) (sqSize * 1.5), y + sqSize);
                i++;
            }
        }

        public synchronized void renderRegions(Graphics g) {
            int area = regionWidth * regionHeight;
            int sqSize = Math.max(1, (int) Math.sqrt(1D * area / snapshot.regionCount()));
            int cols = regionWidth / sqSize;
            int cellSize = sqSize - 2;
            for (int i = 0; i < snapshot.regionCount(); i++) {
                int rectx = (i % cols) * sqSize;
                int recty = (i / cols) * sqSize;
                RegionStat s = snapshot.get(i);
                s.render(g, rectx, recty, cellSize, cellSize);
            }
        }

        protected String collectionMode() {
            if (snapshot.isFullActive()) {
                return "Full";
            }
            if (snapshot.isDegenActive()) {
                return snapshot.isYoungActive() ? "Degenerate Young" : "Degenerate Global";
            }
            return snapshot.isYoungActive() ? "Young" : "Global";
        }

        protected String liveStatusLine() {
            return "Live (Green): MB: T:" +
                    snapshot.live() / ShenandoahVisualizer.KILO + " Y:" +
                    snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::live) / ShenandoahVisualizer.KILO + " O:" +
                    snapshot.generationStat(RegionAffiliation.OLD, RegionStat::live) / ShenandoahVisualizer.KILO;
        }

        protected String usageStatusLine() {
            return "Used (White): MB: T:" +
                    snapshot.used() / ShenandoahVisualizer.KILO + " Y:" +
                    snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::used) / ShenandoahVisualizer.KILO + " O:" +
                    snapshot.generationStat(RegionAffiliation.OLD, RegionStat::used) / ShenandoahVisualizer.KILO;
        }

        protected void renderTimeLineLegendItem(Graphics g, int sqSize, Color color, int lineNumber, String label) {
            g.setColor(color);
            int y = (int) (lineNumber * LINE * 1.5);
            g.fillRect(0, y, sqSize, sqSize);
            g.setColor(Color.BLACK);
            g.drawString(label, (int) (sqSize * 1.5), y + sqSize);
        }

        public synchronized void notifyRegionResized(int width, int height) {
            this.regionWidth = width;
            this.regionHeight = height;
        }

        public synchronized void notifyGraphResized(int width, int height) {
            this.graphWidth = width;
            this.graphHeight = height;
        }

        public void addPopup(RegionPopUp popup) {
            popups.add(popup);
        }
        public void deletePopup(RegionPopUp popup) {
            popups.remove(popup);
        }
        public void repaintPopups() {
            if (popups != null) {
                for (RegionPopUp popup : popups) {
                    popup.setSnapshots(popupSnapshots);
                    popup.repaint();
                }
            }
        }
    }

    public static class RenderLive extends Render {
        volatile DataProvider data;
        int oneFourthIndex = 0;
        int oneHalfIndex = 0;
        int threeFourthIndex = 0;

        public RenderLive(JFrame frame) {
            super(frame);
            this.data = new DataProvider(null);
            this.snapshot = data.snapshot();
        }

        public RenderLive(DataProvider data, JFrame frame) {
            super(frame);
            this.data = data;
            this.snapshot = data.snapshot();
        }

        public synchronized void run() {
            Snapshot cur = data.snapshot();
            int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
            if (!cur.equals(snapshot)) {
                snapshot = cur;
                lastSnapshots.add(new SnapshotView(cur));
                popupSnapshots.add(cur);

                if (lastSnapshots.size() > endBandIndex) {
                    lastSnapshots.removeFirst();
                    oneFourthIndex = lastSnapshots.size() / 4;
                    oneHalfIndex = lastSnapshots.size() / 2;
                    threeFourthIndex = lastSnapshots.size() * 3 / 4;

                } else {
                    if (lastSnapshots.size() == endBandIndex / 4) {
                        oneFourthIndex = lastSnapshots.size() - 1;
                    }
                    if (lastSnapshots.size() == endBandIndex / 2) {
                        oneHalfIndex = lastSnapshots.size() - 1;
                    }
                    if (lastSnapshots.size() == endBandIndex * 3 / 4) {
                        threeFourthIndex = lastSnapshots.size() - 1;
                    }
                }
                frame.repaint();
                repaintPopups();
            }
        }

        public synchronized void loadDataProvider(DataProvider data) {
            closeDataProvider();
            this.data = data;
            this.lastSnapshots.clear();
            this.popupSnapshots.clear();
            this.snapshot = data.snapshot();
        }

        public synchronized void closeDataProvider() {
            if (this.data != null) {
                data.stopConnector();
            }
            this.data = null;
        }

        public synchronized void renderGraph(Graphics g) {
            if (lastSnapshots.size() < 2) return;

            int pad = 30;
            int bandHeight = (graphHeight - pad) / 2;
            int bandWidth = graphWidth - phaseLabelWidth;
            int phaseHeight = bandHeight / 4;
            double stepY = 1D * bandHeight / snapshot.total();

            int startRaw  = graphHeight - bandHeight - pad;

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, bandWidth, graphHeight);

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, bandWidth, bandHeight);
            g.fillRect(0, bandHeight + pad, bandWidth, bandHeight);

            long firstTime = lastSnapshots.getFirst().time();
            long lastTime = lastSnapshots.getLast().time();
            double stepX = (double) STEP_X * Math.min(lastSnapshots.size(), bandWidth) / (lastTime - firstTime);

            for (int i = 0; i < lastSnapshots.size(); i++) {
                SnapshotView s = lastSnapshots.get(i);
                int x = (int) Math.round((s.time() - firstTime) * stepX);

                if (s.globalPhase() == Phase.IDLE) {
                    if (s.oldPhase() == Phase.MARKING) {
                        g.setColor(Colors.OLD[0]);
                        g.drawRect(x, bandHeight + pad, 1, phaseHeight);
                    } else if (s.oldPhase() == Phase.EVACUATING) {
                        g.setColor(Colors.OLD[1]);
                        g.drawRect(x, bandHeight + pad + 2*phaseHeight, 1, phaseHeight);
                    }
                }

                if (s.oldCsetPercentage() > 0) {
                    int height = (int) (bandHeight * s.oldCsetPercentage());
                    g.setColor(Colors.OLD[0]);
                    g.drawRect(x, 2*bandHeight + pad - height, 1, height);
                }

                g.setColor(getColor(s));
                if (getPhase(s) == Phase.MARKING) {
                    g.drawRect(x, bandHeight + pad + phaseHeight, 1, phaseHeight);
                }
                if (getPhase(s) == Phase.EVACUATING) {
                    g.drawRect(x, bandHeight + pad + 2*phaseHeight, 1, phaseHeight);
                }
                if (getPhase(s) == Phase.UPDATE_REFS) {
                    g.drawRect(x, bandHeight + pad + 3*phaseHeight, 1, phaseHeight);
                }



                if (s.isFullActive()) {
                    g.setColor(Colors.FULL);
                    g.drawRect(x, bandHeight + pad, 1, 10);
                } else if (s.isDegenActive()) {
                    g.setColor(Colors.DEGENERATE);
                    g.drawRect(x, bandHeight + pad, 1, 10);
                }

                // Draw these in the upper band.
                g.setColor(Colors.USED);
                g.drawRect(x, (int) Math.round(startRaw - s.used() * stepY), 1, 1);
                g.setColor(Colors.LIVE_REGULAR);
                g.drawRect(x, (int) Math.round(startRaw - s.live() * stepY), 1, 1);
                g.setColor(Colors.LIVE_CSET);
                g.drawRect(x, (int) Math.round(startRaw - s.collectionSet() * stepY), 1, 1);

                g.setColor(Color.GRAY);
                g.drawString("OM", bandWidth + 10, bandHeight + pad + 20);
                g.drawString("M", bandWidth + 10, bandHeight + phaseHeight + pad + 20);
                g.drawString("E", bandWidth + 10, bandHeight + 2*phaseHeight + pad + 20);
                g.drawString("UR", bandWidth + 10, bandHeight + 3*phaseHeight + pad + 20);

                renderTimestampLabel(g);
            }
        }
        public synchronized void renderTimestampLabel(Graphics g) {
            int pad = 30;
            int bandHeight = (graphHeight - pad) / 2;
            int bandWidth = graphWidth - phaseLabelWidth;
            int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(0, bandHeight + 5, 0, bandHeight + pad - 5);
            g2.drawLine(bandWidth / 4, bandHeight + 5, bandWidth / 4, bandHeight + pad - 5);
            g2.drawLine(bandWidth / 2, bandHeight + 5, bandWidth / 2, bandHeight + pad - 5);
            g2.drawLine(bandWidth * 3 / 4, bandHeight + 5, bandWidth * 3 / 4, bandHeight + pad - 5);

            g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1). time() - lastSnapshots.get(0).time()) + " ms", 3, bandHeight + 20);

            if (lastSnapshots.size() > endBandIndex / 4 ) {
                g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1). time() - lastSnapshots.get(oneFourthIndex).time()) + " ms", bandWidth / 4 + 3, bandHeight + 20);
            }
            if (lastSnapshots.size() > endBandIndex / 2) {
                g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1). time() - lastSnapshots.get(oneHalfIndex).time()) + " ms", bandWidth / 2 + 3, bandHeight + 20);
            }
            if (lastSnapshots.size() > endBandIndex * 3 / 4) {
                g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1). time() - lastSnapshots.get(threeFourthIndex).time()) + " ms", bandWidth * 3 / 4 + 3, bandHeight + 20);
            }
        }

        public synchronized void renderStats(Graphics g) {
            String mode = collectionMode();
            String status = "";
            switch (snapshot.phase()) {
                case IDLE:
                    status += " Idle";
                    mode = "";
                    break;
                case MARKING:
                    status += " Marking";
                    if (snapshot.getOldPhase() == Phase.MARKING) {
                        status += " (Old)";
                    }
                    break;
                case EVACUATING:
                    status += " Evacuating";
                    break;
                case UPDATE_REFS:
                    status += " Updating Refs";
                    break;
                case UNKNOWN:
                    status += " Unknown";
                    break;
            }

            int line = 0;

            g.setColor(Color.BLACK);
            g.drawString("Status: " + data.status(), 0, ++line * LINE);
            g.drawString("GC: " + status, 0, ++line * LINE);
            g.drawString("Mode: " + mode, 0, ++line * LINE);
            g.drawString("Total: " + (snapshot.total() / KILO) + " MB", 0, ++line * LINE);
            g.drawString(usageStatusLine(), 0, ++line * LINE);
            g.drawString(liveStatusLine(), 0, ++line * LINE);

            Histogram histogram = snapshot.getSafepointTime();
            String pausesText = String.format("GC Pauses: P100=%d, P95=%d, P90=%d",
                    histogram.getMaxValue(), histogram.getValueAtPercentile(95), histogram.getValueAtPercentile(90));
            g.drawString(pausesText, 0, ++line * LINE);

            line = 4;
            renderTimeLineLegendItem(g, LINE, Colors.OLD[1], ++line, "Old Marking (OM)");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[1], ++line, "Young Marking (M)");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[2], ++line, "Young Evacuation (E)");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[3], ++line, "Young Update References (UR)");

            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[1], ++line, "Global Marking (M)");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[2], ++line, "Global Evacuation (E)");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[3], ++line, "Global Update References (UR)");

            renderTimeLineLegendItem(g, LINE, Colors.DEGENERATE, ++line, "Degenerated Cycle");
            renderTimeLineLegendItem(g, LINE, Colors.FULL, ++line, "Full");
        }
    }

    public static class RenderPlayback extends Render {
        volatile DataLogProvider data;
        volatile boolean isPaused;

        volatile double speed = 1.0;
        volatile int frontSnapshotIndex = 0;
        volatile int endSnapshotIndex = 0;
        int oneFourthIndex = 0;
        int oneHalfIndex = 0;
        int threeFourthIndex = 0;

        ToolbarPanel toolbarPanel;

        public RenderPlayback(JFrame frame, ToolbarPanel toolbarPanel) {
            super(frame);
            this.toolbarPanel = toolbarPanel;
            this.snapshot = null;
            this.isPaused = true;
        }

        public RenderPlayback(DataLogProvider data, JFrame frame, ToolbarPanel toolbarPanel) {
            super(frame);
            this.toolbarPanel = toolbarPanel;
            this.data = data;
            this.snapshot = data.snapshot();
            this.isPaused = false;
        }
        public void updateTimestampLabelIndexes() {
            int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
            if (frontSnapshotIndex > 0) {
                oneFourthIndex = frontSnapshotIndex + (endSnapshotIndex - frontSnapshotIndex) / 4;
                oneHalfIndex = frontSnapshotIndex + (endSnapshotIndex - frontSnapshotIndex) / 2;
                threeFourthIndex = frontSnapshotIndex + (endSnapshotIndex - frontSnapshotIndex) * 3 / 4;
            } else {
                if (endSnapshotIndex == endBandIndex / 4) {
                    oneFourthIndex = endSnapshotIndex - 1;
                }
                if (endSnapshotIndex == endBandIndex / 2) {
                    oneHalfIndex = endSnapshotIndex - 1;
                }
                if (endSnapshotIndex == endBandIndex * 3 / 4) {
                    threeFourthIndex = endSnapshotIndex - 1;
                }
            }
        }

        public synchronized void run() {
            int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
            if (!isPaused) {
                if (!data.stopwatch.isStarted()) {
                    data.controlStopwatch("START");
                }
                if (endSnapshotIndex < lastSnapshots.size()) {
                    int i = Math.max(endSnapshotIndex, 0);
                    long time = lastSnapshots.get(i).time();
                    snapshot = data.getSnapshotAtTime(time);
                    if (data.snapshotTimeHasOccurred(snapshot)) {
                        popupSnapshots.add(snapshot);
                        toolbarPanel.setValue(popupSnapshots.size());
                        endSnapshotIndex++;
                        frame.repaint();
                        repaintPopups();
                    }
                    if (endSnapshotIndex - frontSnapshotIndex > endBandIndex) {
                        frontSnapshotIndex++;
                    }
                } else {
                    Snapshot cur = data.snapshot();
                    if (!cur.equals(snapshot)) {
                        snapshot = cur;
                        lastSnapshots.add(new SnapshotView(cur));
                        popupSnapshots.add(cur);
                        endSnapshotIndex = lastSnapshots.size();
                        if (lastSnapshots.size() - frontSnapshotIndex > endBandIndex) {
                            frontSnapshotIndex++;
                        }
                        toolbarPanel.setValue(popupSnapshots.size());
                        frame.repaint();
                        repaintPopups();
                    }
                }
                updateTimestampLabelIndexes();
                if (data.isEndOfSnapshots() && endSnapshotIndex >= lastSnapshots.size()) {
                    toolbarPanel.setValue(popupSnapshots.size());
                    System.out.println("Should only enter here at end of snapshots.");
                    data.controlStopwatch("STOP");
                    isPaused = true;
                }
            } else {
                updateTimestampLabelIndexes();
                repaintPopups();
                if (data.stopwatch.isStarted()) {
                    data.controlStopwatch("STOP");
                }
            }
        }

        public synchronized void stepBackSnapshots(int n) {
            if (lastSnapshots.size() == 0) return;

            frontSnapshotIndex = Math.max(frontSnapshotIndex - n, 0);
            endSnapshotIndex = Math.max(endSnapshotIndex - n, 0);

            int i = Math.max(endSnapshotIndex - 1, 0);
            long time = lastSnapshots.get(i).time();
            data.setStopwatchTime(TimeUnit.MILLISECONDS.toNanos(time));

            snapshot = data.getSnapshotAtTime(time);

            for (int j = 0; j < n; j++) {
                if (popupSnapshots.size() > 0) {
                    popupSnapshots.remove(popupSnapshots.size() - 1);
                }
            }
            updateTimestampLabelIndexes();
            toolbarPanel.setValue(popupSnapshots.size());

            frame.repaint();
            repaintPopups();
        }

        public synchronized void stepForwardSnapshots(int n) {
            if (lastSnapshots.size() == 0) return;

            int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
            for (int i = 0; i < n; i++) {
                if (endSnapshotIndex < lastSnapshots.size()) {
                    int index = Math.max(endSnapshotIndex, 0);
                    long time = lastSnapshots.get(index).time();
                    snapshot = data.getSnapshotAtTime(time);
                    popupSnapshots.add(snapshot);
                    toolbarPanel.setValue(popupSnapshots.size());
                } else {
                    // keep processing snapshots from logData until it reaches a diff snapshot from this.snapshot
                    Snapshot cur = data.getNextSnapshot();
                    while (cur == snapshot && !data.isEndOfSnapshots()) {
                        cur = data.getNextSnapshot();
                    }
                    if (data.isEndOfSnapshots()) break;

                    snapshot = cur;
                    lastSnapshots.add(new SnapshotView(cur));
                    popupSnapshots.add(cur);
                    toolbarPanel.setValue(popupSnapshots.size());
                }
                updateTimestampLabelIndexes();
                data.setStopwatchTime(TimeUnit.MILLISECONDS.toNanos(snapshot.time()));
                endSnapshotIndex++;
            }

            while (endSnapshotIndex - frontSnapshotIndex > endBandIndex) {
                frontSnapshotIndex++;
            }

            frame.repaint();
            repaintPopups();
        }

        private synchronized void loadLogDataProvider(DataLogProvider data) {
            this.data = data;
            this.lastSnapshots.clear();
            this.popupSnapshots.clear();
            this.snapshot = data.snapshot();
            this.isPaused = false;
            this.speed = 1.0;
            endSnapshotIndex = 0;
            frontSnapshotIndex = 0;
        }

        public synchronized void renderGraph(Graphics g) {
            if (endSnapshotIndex - frontSnapshotIndex < 2) return;

            int pad = 30;
            int bandHeight = (graphHeight - pad) / 2;
            int bandWidth  = graphWidth - phaseLabelWidth;
            int phaseHeight = bandHeight / 4;
            double stepY = 1D * bandHeight / snapshot.total();

            int startRaw  = graphHeight - bandHeight - pad;

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, bandWidth, graphHeight);

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, bandWidth, bandHeight);
            g.fillRect(0, bandHeight + pad, bandWidth, bandHeight);

            long firstTime = lastSnapshots.get(frontSnapshotIndex).time();
            long lastTime = lastSnapshots.get(endSnapshotIndex - 1).time();
            double stepX = (double) STEP_X * Math.min(endSnapshotIndex - frontSnapshotIndex, bandWidth) / (lastTime - firstTime);

            for (int i = frontSnapshotIndex; i < endSnapshotIndex; i++) {
                SnapshotView s = lastSnapshots.get(i);
                int x = (int) Math.round((s.time() - firstTime) * stepX);

                if (s.oldPhase() == Phase.MARKING && s.globalPhase() == Phase.IDLE) {
                    g.setColor(Colors.OLD[0]);
                    g.drawRect(x, bandHeight + pad, 1, phaseHeight);
                }

                if (s.oldCsetPercentage() > 0) {
                    int height = (int) (bandHeight * s.oldCsetPercentage());
                    g.setColor(Colors.OLD[0]);
                    g.drawRect(x, 2*bandHeight + pad - height, 1, height);
                }

                g.setColor(getColor(s));
                if (getPhase(s) == Phase.MARKING) {
                    g.drawRect(x, bandHeight + pad + phaseHeight, 1, phaseHeight);
                }
                if (getPhase(s) == Phase.EVACUATING) {
                    g.drawRect(x, bandHeight + pad + 2*phaseHeight, 1, phaseHeight);
                }
                if (getPhase(s) == Phase.UPDATE_REFS) {
                    g.drawRect(x, bandHeight + pad + 3*phaseHeight, 1, phaseHeight);
                }

                if (s.isFullActive()) {
                    g.setColor(Colors.FULL);
                    g.drawRect(x, bandHeight + pad, 1, 10);
                } else if (s.isDegenActive()) {
                    g.setColor(Colors.DEGENERATE);
                    g.drawRect(x, bandHeight + pad, 1, 10);
                }

                // Draw these in the upper band.
                g.setColor(Colors.USED);
                g.drawRect(x, (int) Math.round(startRaw - s.used() * stepY), 1, 1);
                g.setColor(Colors.LIVE_REGULAR);
                g.drawRect(x, (int) Math.round(startRaw - s.live() * stepY), 1, 1);
                g.setColor(Colors.LIVE_CSET);
                g.drawRect(x, (int) Math.round(startRaw - s.collectionSet() * stepY), 1, 1);

                g.setColor(Color.GRAY);
                g.drawString("OM", bandWidth + 10, bandHeight + pad + 20);
                g.drawString("M", bandWidth + 10, bandHeight + phaseHeight + pad + 20);
                g.drawString("E", bandWidth + 10, bandHeight + 2*phaseHeight + pad + 20);
                g.drawString("UR", bandWidth + 10, bandHeight + 3*phaseHeight + pad + 20);


                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(2));
                g2.drawLine(0, bandHeight + 5, 0, bandHeight + pad - 5);
                g2.drawLine(bandWidth / 4, bandHeight + 5, bandWidth / 4, bandHeight + pad - 5);
                g2.drawLine(bandWidth / 2, bandHeight + 5, bandWidth / 2, bandHeight + pad - 5);
                g2.drawLine(bandWidth * 3 / 4, bandHeight + 5, bandWidth * 3 / 4, bandHeight + pad - 5);

                g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() - popupSnapshots.get(frontSnapshotIndex).time()) + " ms", 3, bandHeight + 20);
                if (x >= bandWidth / 4 && popupSnapshots.size() > oneFourthIndex) {
                    g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() - popupSnapshots.get(oneFourthIndex).time()) + " ms", bandWidth / 4 + 3, bandHeight + 20);
                }
                if (x >= bandWidth / 2 && popupSnapshots.size() > oneHalfIndex) {
                    g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() -popupSnapshots.get(oneHalfIndex).time()) + " ms", bandWidth / 2 + 3, bandHeight + 20);
                }
                if (x >= bandWidth * 3 / 4 && popupSnapshots.size() > threeFourthIndex) {
                    g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() - popupSnapshots.get(threeFourthIndex).time()) + " ms", bandWidth * 3 / 4 + 3, bandHeight + 20);
                }

            }
        }

        public synchronized void renderStats(Graphics g) {
            String mode = collectionMode();
            String status = "";
            switch (snapshot.phase()) {
                case IDLE:
                    status += " Idle";
                    mode = "";
                    break;
                case MARKING:
                    status += " Marking";
                    if (snapshot.getOldPhase() == Phase.MARKING) {
                        status += " (Old)";
                    }
                    break;
                case EVACUATING:
                    status += " Evacuating";
                    break;
                case UPDATE_REFS:
                    status += " Updating Refs";
                    break;
                case UNKNOWN:
                    status += " Unknown";
                    break;
            }

            int line = 0;

            g.setColor(Color.BLACK);
            g.drawString("GC: " + status, 0, ++line * LINE);
            g.drawString("Mode: " + mode, 0, ++line * LINE);
            g.drawString("Total: " + (snapshot.total() / KILO) + " MB", 0, ++line * LINE);
            g.drawString(usageStatusLine(), 0, ++line * LINE);
            g.drawString(liveStatusLine(), 0, ++line * LINE);

            line = 4;
            renderTimeLineLegendItem(g, LINE, Colors.OLD[1], ++line, "Old Marking (OM)");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[1], ++line, "Young Marking (M)");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[2], ++line, "Young Evacuation (E)");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[3], ++line, "Young Update References (UR)");

            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[1], ++line, "Global Marking (M)");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[2], ++line, "Global Evacuation (E)");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[3], ++line, "Global Update References (UR)");

            renderTimeLineLegendItem(g, LINE, Colors.DEGENERATE, ++line, "Degenerated Young");
            renderTimeLineLegendItem(g, LINE, Colors.FULL, ++line, "Full");
        }

        public int getPopupSnapshotsSize() {
            return popupSnapshots.size();
        }

    }

    public static class RenderRunner implements Runnable {
        final RenderLive live;
        final RenderPlayback playback;

        final JFrame frame;
        private boolean isLive;

        public RenderRunner(DataProvider data, JFrame frame, ToolbarPanel toolbarPanel) {
            this.frame = frame;
            live = new RenderLive(data, frame);
            playback = new RenderPlayback(frame, toolbarPanel);
            isLive = true;
        }

        public RenderRunner(DataLogProvider data, JFrame frame, ToolbarPanel toolbarPanel) {
            this.frame = frame;
            live = new RenderLive(frame);
            live.closeDataProvider();
            playback = new RenderPlayback(data, frame, toolbarPanel);
            isLive = false;
        }

        public synchronized void loadPlayback(DataLogProvider data) {
            if (isLive) {
                live.closeDataProvider();
            }
            isLive = false;
            playback.loadLogDataProvider(data);
        }

        public synchronized void loadLive(DataProvider data) {
            if (!isLive) {
                isLive = true;
                live.loadDataProvider(data);
            }
        }

        public synchronized void run() {
            if (isLive) {
                live.run();
            } else {
                playback.run();
            }
        }

        public synchronized void renderGraph(Graphics g) {
            if (isLive) {
                live.renderGraph(g);
            } else {
                playback.renderGraph(g);
            }
        }

        public synchronized void renderStats(Graphics g) {
            if (isLive) {
                live.renderStats(g);
            } else {
                playback.renderStats(g);
            }
        }

        public synchronized void renderRegions(Graphics g) {
            if (isLive) {
                live.renderRegions(g);
            } else {
                playback.renderRegions(g);
            }
        }
        public synchronized void renderLegend(Graphics g) {
            if (isLive) {
                live.renderLegend(g);
            } else {
                playback.renderLegend(g);
            }
        }

        public synchronized void notifyRegionResized(int width, int height) {
            live.notifyRegionResized(width, height);
            playback.notifyRegionResized(width, height);
        }

        public synchronized void notifyGraphResized(int width, int height) {
            live.notifyGraphResized(width, height);
            playback.notifyGraphResized(width, height);
        }
        public void addPopup(RegionPopUp popup) {
            if (isLive) {
                this.live.addPopup(popup);
            } else {
                this.playback.addPopup(popup);
            }
        }
        public void deletePopup(RegionPopUp popup) {
            if (isLive) {
                this.live.deletePopup(popup);
            } else {
                this.playback.deletePopup(popup);
            }
        }
    }
}


