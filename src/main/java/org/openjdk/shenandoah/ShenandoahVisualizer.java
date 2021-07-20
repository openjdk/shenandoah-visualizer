/*
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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openjdk.shenandoah.RegionState.*;

class ShenandoahVisualizer {

    private static final int INITIAL_WIDTH = 2000;
    private static final int INITIAL_HEIGHT = 1600;
    private static final int KILO = 1024;

    public static void main(String[] args) throws Exception {
        String vmIdentifier = null;
        for (int i = 0; i < args.length; i++) {
            System.out.println(i + ": " + args[i]);
        }
        if (args.length == 1) {
            vmIdentifier = args[0];
        }
        boolean isReplay = false;
        final String[] filePath = {""};
        if (args.length > 0) {
            isReplay = args[0].equals("-playback");
            filePath[0] = args[1];
        }

        // Allow for log file flag
        // allow for vm argument
        // parse args
            // vm starts with local://

        JFrame frame = new JFrame();
        frame.setLayout(new GridBagLayout());
        frame.setTitle("Shenandoah GC Visualizer");
        frame.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        // Choose data based on playback vs. realtime
        Render tempRender = null;
        if (isReplay) {
            DataLogProvider logData = new DataLogProvider(filePath[0]);
            tempRender = new Render(logData, frame);
        } else {
            DataProvider data = new DataProvider(vmIdentifier);
            tempRender = new Render(data, frame);
        }
        final Render render = tempRender;

        JPanel regionsPanel = new JPanel() {
            public void paint(Graphics g) {
                render.renderRegions(g);
            }
        };

        JPanel legendPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                Render.renderLegend(g);
            }
        };

        JPanel statusPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                render.renderStats(g);
            }
        };

        JPanel graphPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                render.renderGraph(g);
            }
        };

        JPanel buttonPanel = new JPanel();

        JButton fileChooserButton = new JButton("Select File");
        fileChooserButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                JFileChooser fc = new JFileChooser();
                int returnValue = fc.showOpenDialog(null);
                if (returnValue == JFileChooser.APPROVE_OPTION) {
                    filePath[0] = fc.getSelectedFile().getAbsolutePath();
                    try {
                        DataLogProvider logData = new DataLogProvider(filePath[0]);
                        render.loadLogProvider(logData);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    System.out.println(filePath[0]);
                }
            }
        });
        buttonPanel.add(fileChooserButton);

        regionsPanel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                render.notifyRegionResized(ev.getComponent().getWidth(), ev.getComponent().getHeight());
            }
        });

        graphPanel.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                render.notifyGraphResized(ev.getComponent().getWidth(), ev.getComponent().getHeight());
            }
        });

        Insets pad = new Insets(10, 10, 10, 10);

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 3;
            c.weighty = 1;
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
            c.gridheight = 3;
            frame.add(regionsPanel, c);
        }

        {
            statusPanel.setPreferredSize(new Dimension(25, 175));
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 0.5;
            c.weighty = 0.2;
//            c.weighty = 0.5;
            c.insets = pad;
            frame.add(statusPanel, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 0.5;
            c.weighty = 0.75;
//            c.weighty = 0.5;
            c.insets = pad;
            frame.add(legendPanel, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 3;
            c.weightx = 0.5;
            c.weighty = 0.05;
            c.insets = pad;
            frame.add(buttonPanel, c);
        }

        frame.setVisible(true);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> f = service.scheduleAtFixedRate(render,
                0, 100, TimeUnit.MILLISECONDS);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                f.cancel(true);
                service.shutdown();
                frame.dispose();
            }
        });

        f.get();
    }

    public static class Render implements Runnable {
        public static final int LINE = 20;

        volatile DataProvider data;
        volatile DataLogProvider logData;
        final JFrame frame;
        volatile boolean isLog;

        int regionWidth, regionHeight;
        int graphWidth, graphHeight;
        final int STEP_X = 2;

        final LinkedList<SnapshotView> lastSnapshots;
        volatile Snapshot snapshot;

        public Render(DataProvider data, JFrame frame) {
            this.data = data;
            this.logData = null;
            this.isLog = false;
            this.frame = frame;
            this.lastSnapshots = new LinkedList<>();
            this.snapshot = data.snapshot();
        }

        public Render(DataLogProvider logData, JFrame frame) {
            this.data = null;
            this.logData = logData;
            this.isLog = true;
            this.frame = frame;
            this.lastSnapshots = new LinkedList<>();
            this.snapshot = logData.snapshot();
        }

        @Override
        public synchronized void run() {
            Snapshot cur = this.isLog ? this.logData.snapshot() : this.data.snapshot();
            if (!cur.equals(snapshot)) {
                snapshot = cur;
                lastSnapshots.add(new SnapshotView(cur));
                if (lastSnapshots.size() > graphWidth / STEP_X) {
                    lastSnapshots.removeFirst();
                }
                frame.repaint();
            }
        }

        private void loadLogProvider(DataLogProvider logData) {
            this.logData = logData;
            this.data = null;
            this.isLog  = true;
            this.lastSnapshots.clear();
            this.snapshot = logData.snapshot();
        }

        private static Color getColor(SnapshotView s) {
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

        public synchronized static void renderLegend(Graphics g) {
            final int sqSize = LINE;

            Map<String, RegionStat> items = new LinkedHashMap<>();

            items.put("Empty Uncommitted",
                    new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_UNCOMMITTED));

            items.put("Empty Committed",
                    new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_COMMITTED));

            items.put("Trash",
                    new RegionStat(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, TRASH));

            items.put("Fully Live, 100% TLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, REGULAR));

            items.put("0% Live, 100% TLAB Allocs",
                    new RegionStat(1.0f, 0.0f, 1.0f, 0.0f, 0.0f, REGULAR));

            items.put("Fully Live, 100% GCLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 0.0f, 1.0f, 0.0f, REGULAR));

            items.put("0% Live, 100% GCLAB Allocs",
                    new RegionStat(1.0f, 0.0f, 0.0f, 1.0f, 0.0f, REGULAR));

            items.put("Fully Live, 100% Shared Allocs",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f, REGULAR));

            items.put("0% Live, 100% Shared Allocs",
                    new RegionStat(1.0f, 0.0f, 0.0f, 0.0f, 1.0f, REGULAR));

            items.put("Fully Live, 50%/50% TLAB/GCLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 0.5f, 0.5f, 0.0f, REGULAR));

            items.put("Fully Live, 33%/33%/33% T/GC/S Allocs",
                    new RegionStat(1.0f, 1.0f, 1f/3, 1f/3, 1f/3, REGULAR));

            items.put("Fully Live Humongous",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, HUMONGOUS));

            items.put("Fully Live Humongous + Pinned",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, PINNED_HUMONGOUS));

            items.put("1/3 Live + Collection Set",
                    new RegionStat(1.0f, 0.3f, 0.0f, 0.0f, 0.0f, CSET));

            items.put("1/3 Live + Pinned",
                    new RegionStat(1.0f, 0.3f, 0.3f, 0.0f, 0.0f, PINNED));

            items.put("1/3 Live + Pinned CSet",
                    new RegionStat(1.0f, 0.3f, 0.0f, 0.0f, 0.0f, PINNED_CSET));

            items.put("Age 0+", new RegionStat(REGULAR, 0));
            items.put("Age 3+", new RegionStat(REGULAR, 3));
            items.put("Age 6+", new RegionStat(REGULAR, 6));
            items.put("Age 9+", new RegionStat(REGULAR, 9));
            items.put("Age 12+", new RegionStat(REGULAR, 12));
            items.put("Age 15+", new RegionStat(REGULAR, 15));

            int i = 0;
            for (String key : items.keySet()) {
                int y = (int) (i * sqSize * 1.5);
                items.get(key).render(g, 0, y, sqSize, sqSize);
                g.setColor(Color.BLACK);
                g.drawString(key, (int) (sqSize * 1.5), y + sqSize);
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

        private String collectionMode() {
            if (snapshot.isFullActive()) {
                return "Full";
            }
            if (snapshot.isDegenActive()) {
                return snapshot.isYoungActive() ? "Degenerate Young" : "Degenerate Global";
            }
            return snapshot.isYoungActive() ? "Young" : "Global";
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
            if (!isLog) {
                g.drawString("Status: " + data.status(), 0, ++line * LINE);
            }
            g.drawString("GC: " + status, 0, ++line * LINE);
            g.drawString("Mode: " + mode, 0, ++line * LINE);
            g.drawString("Total: " + (snapshot.total() / KILO) + " MB", 0, ++line * LINE);
            g.drawString(usageStatusLine(), 0, ++line * LINE);
            g.drawString(liveStatusLine(), 0, ++line * LINE);

            if (!this.isLog) {
                Histogram histogram = snapshot.getSafepointTime();
                String pausesText = String.format("GC Pauses: P100=%d, P95=%d, P90=%d",
                        histogram.getMaxValue(), histogram.getValueAtPercentile(95), histogram.getValueAtPercentile(90));
                g.drawString(pausesText, 0, ++line * LINE);
            }

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

        private String liveStatusLine() {
            return "Live (Green): MB: T:" +
                    snapshot.live() / ShenandoahVisualizer.KILO + " Y:" +
                    snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::live) / ShenandoahVisualizer.KILO + " O:" +
                    snapshot.generationStat(RegionAffiliation.OLD, RegionStat::live) / ShenandoahVisualizer.KILO;
        }

        private String usageStatusLine() {
            return "Used (White): MB: T:" +
                    snapshot.used() / ShenandoahVisualizer.KILO + " Y:" +
                    snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::used) / ShenandoahVisualizer.KILO + " O:" +
                    snapshot.generationStat(RegionAffiliation.OLD, RegionStat::used) / ShenandoahVisualizer.KILO;
        }

        private void renderTimeLineLegendItem(Graphics g, int sqSize, Color color, int lineNumber, String label) {
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
}


