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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;

import static org.openjdk.shenandoah.RegionState.*;

class ShenandoahVisualizer {

    private static final int INITIAL_WIDTH = 2000;
    private static final int INITIAL_HEIGHT = 1600;
    private static final int KILO = 1024;
    private static final String PLAYBACK = "Playback";
    private static final String REALTIME = "Realtime";

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

        if (isReplay) {
            DataLogProvider data = new DataLogProvider(filePath[0]);
            renderRunner = new RenderRunner(data, frame);
            toolbarPanel.setModeField(PLAYBACK);
            toolbarPanel.setEnabledRealtimeModeButton(true);
            toolbarPanel.setFileNameField(filePath[0]);
        } else {
            DataProvider data = new DataProvider(vmIdentifier);
            renderRunner = new RenderRunner(data, frame);
            toolbarPanel.setModeField(REALTIME);
            toolbarPanel.setEnabledRealtimeModeButton(false);
        }

        // Executors
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);;
        final ScheduledFuture<?>[] f = {service.scheduleAtFixedRate(renderRunner,
                0, renderRunner.isLive ? 100 : 1, TimeUnit.MILLISECONDS)};
        ;


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
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    filePath[0] = fc.getSelectedFile().getAbsolutePath();
                    try {
                        DataLogProvider data = new DataLogProvider(filePath[0]);
                        renderRunner.loadPlayback(data);
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
            }
        };
        toolbarPanel.setFileButtonListener(fileButtonListener);

        ActionListener playPauseButtonListener = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                if (renderRunner.playback.isPaused) {
                    renderRunner.playback.data.controlStopwatch("START");
                    toolbarPanel.setLastActionField("Play button pressed.");
                } else {
                    renderRunner.playback.data.controlStopwatch("STOP");
                    toolbarPanel.setLastActionField("Pause button pressed.");
                }
                renderRunner.playback.isPaused = !renderRunner.playback.isPaused;
            }
        };
        toolbarPanel.setPlayPauseButtonListener(playPauseButtonListener);

        // Step back/forward button listeners
        toolbarPanel.setBackButton_1_Listener((ae) -> renderRunner.playback.stepBackSnapshots(1));

        toolbarPanel.setBackButton_5_Listener((ae) -> renderRunner.playback.stepBackSnapshots(5));

        toolbarPanel.setForwardButton_1_Listener((ae) -> renderRunner.playback.stepForwardSnapshots(1));

        toolbarPanel.setForwardButton_5_Listener((ae) -> renderRunner.playback.stepForwardSnapshots(5));

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

        regionsPanel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                renderRunner.notifyRegionResized(ev.getComponent().getWidth(), ev.getComponent().getHeight());
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
            c.weighty = 2;
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

        int regionWidth, regionHeight;
        int graphWidth, graphHeight;
        final int STEP_X = 2;

        final LinkedList<SnapshotView> lastSnapshots;
        volatile Snapshot snapshot;

        public Render(JFrame frame) {
            this.frame = frame;
            this.lastSnapshots = new LinkedList<>();
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
    }

    public static class RenderLive extends Render {
        volatile DataProvider data;

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
            if (!cur.equals(snapshot)) {
                snapshot = cur;
                lastSnapshots.add(new SnapshotView(cur));
                if (lastSnapshots.size() > graphWidth / STEP_X) {
                    lastSnapshots.removeFirst();
                }
                frame.repaint();
            }
        }

        public synchronized void loadDataProvider(DataProvider data) {
            closeDataProvider();
            this.data = data;
            this.lastSnapshots.clear();
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

            int pad = 10;
            int bandHeight = (graphHeight - pad) / 2;
            double stepY = 1D * bandHeight / snapshot.total();

            int startDiff = graphHeight;
            int startRaw  = graphHeight - bandHeight - pad;

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, graphWidth, graphHeight);

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, graphWidth, bandHeight);
            g.fillRect(0, bandHeight + pad, graphWidth, bandHeight);

            long firstTime = lastSnapshots.getFirst().time();
            long lastTime = lastSnapshots.getLast().time();
            double stepX = (double) STEP_X * Math.min(lastSnapshots.size(), graphWidth) / (lastTime - firstTime);

            for (int i = 0; i < lastSnapshots.size(); i++) {
                SnapshotView s = lastSnapshots.get(i);
                int x = (int) Math.round((s.time() - firstTime) * stepX);

                if (s.globalPhase() == Phase.IDLE) {
                    if (s.oldPhase() == Phase.MARKING) {
                        g.setColor(Colors.OLD[0]);
                        g.drawRect(x, 0, 1, bandHeight);
                    } else if (s.oldPhase() == Phase.EVACUATING) {
                        g.setColor(Colors.OLD[1]);
                        g.drawRect(x, 0, 1, bandHeight);
                    }
                }

                g.setColor(getColor(s));
                g.drawRect(x, bandHeight + pad, 1, bandHeight);

                if (s.oldCsetPercentage() > 0) {
                    int height = (int) (bandHeight * s.oldCsetPercentage());
                    g.setColor(Colors.OLD[0]);
                    g.drawRect(x, 2*bandHeight + pad - height, 1, height);
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

                // Draw this in the lower band.
                final int smooth = Math.min(10, i + 1);
                final int mult = 50;

                SnapshotView ls = lastSnapshots.get(i - smooth + 1);

                g.setColor(Colors.USED);
                g.drawRect(x, (int) Math.round(startDiff - (s.used() - ls.used()) * stepY * mult / smooth), 1, 1);
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
            renderTimeLineLegendItem(g, LINE, Colors.OLD[1], ++line, "Old Marking");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[1], ++line, "Young Marking");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[2], ++line, "Young Evacuation");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[3], ++line, "Young Update References");

            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[1], ++line, "Global Marking");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[2], ++line, "Global Evacuation");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[3], ++line, "Global Update References");

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

        public RenderPlayback(JFrame frame) {
            super(frame);
            this.snapshot = null;
            this.isPaused = true;
        }

        public RenderPlayback(DataLogProvider data, JFrame frame) {
            super(frame);
            this.data = data;
            this.snapshot = data.snapshot();
            this.isPaused = false;
        }

        public synchronized void run() {
            if (!isPaused) {
                if (endSnapshotIndex < lastSnapshots.size()) {
                    int i = Math.max(endSnapshotIndex - 1, 0);
                    long time = lastSnapshots.get(i).time();
                    snapshot = data.getSnapshotAtTime(time);
                    if (data.snapshotTimeHasOccurred(snapshot)) {
                        endSnapshotIndex++;
                        frame.repaint();
                    }
                } else {
                    Snapshot cur = data.snapshot();
                    if (!cur.equals(snapshot)) {
                        snapshot = cur;
                        lastSnapshots.add(new SnapshotView(cur));
                        endSnapshotIndex = lastSnapshots.size();
                        if (lastSnapshots.size() - frontSnapshotIndex > graphWidth / STEP_X) {
                            frontSnapshotIndex++;
                        }
                        frame.repaint();
                    }
                }
                if (data.isEndOfSnapshots() && endSnapshotIndex >= lastSnapshots.size()) {
                    System.out.println("Should only enter here at end of snapshots.");
                    data.controlStopwatch("STOP");
                    isPaused = true;
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
            frame.repaint();
        }

        public synchronized void stepForwardSnapshots(int n) {
            if (lastSnapshots.size() == 0) return;

            for (int i = 0; i < n; i++) {
                if (endSnapshotIndex < lastSnapshots.size()) {
                    int index = Math.max(endSnapshotIndex - 1, 0);
                    long time = lastSnapshots.get(index).time();
                    snapshot = data.getSnapshotAtTime(time);
                } else {
                    // keep processing snapshots from logData until it reaches a diff snapshot from this.snapshot
                    Snapshot cur = data.getNextSnapshot();
                    while (cur == snapshot && !data.isEndOfSnapshots()) {
                        cur = data.getNextSnapshot();
                    }
                    if (data.isEndOfSnapshots()) break;

                    snapshot = cur;
                    lastSnapshots.add(new SnapshotView(cur));
                }
                data.setStopwatchTime(TimeUnit.MILLISECONDS.toNanos(snapshot.time()));
                endSnapshotIndex++;
            }

            while (endSnapshotIndex - frontSnapshotIndex > graphWidth / STEP_X) {
                frontSnapshotIndex++;
            }

            frame.repaint();
        }

        private synchronized void loadLogDataProvider(DataLogProvider data) {
            this.data = data;
            this.lastSnapshots.clear();
            this.snapshot = data.snapshot();
            this.isPaused = false;
            this.speed = 1.0;
            endSnapshotIndex = 0;
            frontSnapshotIndex = 0;
        }

        public synchronized void renderGraph(Graphics g) {
            if (endSnapshotIndex - frontSnapshotIndex < 2) return;

            int pad = 10;
            int bandHeight = (graphHeight - pad) / 2;
            double stepY = 1D * bandHeight / snapshot.total();

            int startDiff = graphHeight;
            int startRaw  = graphHeight - bandHeight - pad;

            g.setColor(Color.WHITE);
            g.fillRect(0, 0, graphWidth, graphHeight);

            g.setColor(Color.BLACK);
            g.fillRect(0, 0, graphWidth, bandHeight);
            g.fillRect(0, bandHeight + pad, graphWidth, bandHeight);

            long firstTime = lastSnapshots.get(frontSnapshotIndex).time();
            long lastTime = lastSnapshots.get(endSnapshotIndex - 1).time();
            double stepX = (double) STEP_X * Math.min(endSnapshotIndex - frontSnapshotIndex, graphWidth) / (lastTime - firstTime);

            for (int i = frontSnapshotIndex; i < endSnapshotIndex; i++) {
                SnapshotView s = lastSnapshots.get(i);
                int x = (int) Math.round((s.time() - firstTime) * stepX);

                if (s.oldPhase() == Phase.MARKING && s.globalPhase() == Phase.IDLE) {
                    g.setColor(Colors.OLD[0]);
                    g.drawRect(x, 0, 1, bandHeight);
                }

                g.setColor(getColor(s));
                g.drawRect(x, bandHeight + pad, 1, bandHeight);

                if (s.oldCsetPercentage() > 0) {
                    int height = (int) (bandHeight * s.oldCsetPercentage());
                    g.setColor(Colors.OLD[0]);
                    g.drawRect(x, 2*bandHeight + pad - height, 1, height);
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

                // Draw this in the lower band.
                final int smooth = Math.min(10, i + 1);
                final int mult = 50;

                SnapshotView ls = lastSnapshots.get(i - smooth + 1);

                g.setColor(Colors.USED);
                g.drawRect(x, (int) Math.round(startDiff - (s.used() - ls.used()) * stepY * mult / smooth), 1, 1);
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
            renderTimeLineLegendItem(g, LINE, Colors.OLD[1], ++line, "Old Marking");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[1], ++line, "Young Marking");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[2], ++line, "Young Evacuation");
            renderTimeLineLegendItem(g, LINE, Colors.YOUNG[3], ++line, "Young Update References");

            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[1], ++line, "Global Marking");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[2], ++line, "Global Evacuation");
            renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[3], ++line, "Global Update References");

            renderTimeLineLegendItem(g, LINE, Colors.DEGENERATE, ++line, "Degenerated Young");
            renderTimeLineLegendItem(g, LINE, Colors.FULL, ++line, "Full");
        }

    }

    public static class RenderRunner implements Runnable {
        final RenderLive live;
        final RenderPlayback playback;

        final JFrame frame;
        private boolean isLive;

        public RenderRunner(DataProvider data, JFrame frame) {
            this.frame = frame;
            live = new RenderLive(data, frame);
            playback = new RenderPlayback(frame);
            isLive = true;
        }

        public RenderRunner(DataLogProvider data, JFrame frame) {
            this.frame = frame;
            live = new RenderLive(frame);
            live.closeDataProvider();
            playback = new RenderPlayback(data, frame);
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
    }
}


