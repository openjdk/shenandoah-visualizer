/*
 * Copyright (c) 2016, Red Hat, Inc. and/or its affiliates.
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
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openjdk.shenandoah.RegionState.*;

class ShenandoahVisualizer {

    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 800;

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("missing VM identifier");
            System.exit(-1);
        }

        JFrame frame = new JFrame();
        frame.setLayout(new GridBagLayout());
        frame.setTitle("Shenandoah GC Visualizer");
        frame.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);

        DataProvider data = new DataProvider(args[0]);

        Render render = new Render(data, frame);

        JPanel regionsPanel = new JPanel() {
            public void paint(Graphics g) {
                render.renderRegions(g);
            }
        };

        JPanel legendPanel = new JPanel() {
            @Override
            public void paint(Graphics g) {
                render.renderLegend(g);
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
            frame.add(regionsPanel, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 1;
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
            frame.add(legendPanel, c);
        }

        frame.setVisible(true);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        ScheduledFuture<?> f = service.scheduleAtFixedRate(render,
                0, 100, TimeUnit.MILLISECONDS);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                service.shutdown();
                frame.dispose();
            }
        });

        f.get();
    }

    public static class Render implements Runnable {
        public static final int LINE = 20;

        final DataProvider data;
        final JFrame frame;

        int regionWidth, regionHeight;
        int graphWidth, graphHeight;

        final LinkedList<SnapshotView> lastSnapshots;
        volatile Snapshot snapshot;

        public Render(DataProvider data, JFrame frame) {
            this.data = data;
            this.frame = frame;
            this.lastSnapshots = new LinkedList<>();
            this.snapshot = data.snapshot();
        }

        @Override
        public synchronized void run() {
            Snapshot cur = data.snapshot();
            if (!cur.equals(snapshot)) {
                snapshot = cur;
                lastSnapshots.add(new SnapshotView(cur));
                if (lastSnapshots.size() > graphWidth) {
                    lastSnapshots.removeFirst();
                }
                frame.repaint();
            }
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
            double stepX = 1D * Math.min(lastSnapshots.size(), graphWidth) / (lastTime - firstTime);

            for (int i = 0; i < lastSnapshots.size(); i++) {
                SnapshotView s = lastSnapshots.get(i);
                int x = (int) Math.round((s.time() - firstTime) * stepX);

                switch (s.phase()) {
                    case IDLE:
                        g.setColor(Colors.TIMELINE_IDLE);
                        break;
                    case MARKING:
                        g.setColor(Colors.TIMELINE_MARK);
                        break;
                    case EVACUATING:
                        g.setColor(Colors.TIMELINE_EVACUATING);
                        break;
                    case UPDATE_REFS:
                        g.setColor(Colors.TIMELINE_UPDATEREFS);
                        break;
                    default:
                        g.setColor(Color.WHITE);
                }
                g.drawRect(x, 0, 1, bandHeight);
                g.drawRect(x, bandHeight + pad, 1, bandHeight);

                g.setColor(Colors.USED);
                g.drawRect(x, (int) Math.round(startRaw - s.used() * stepY), 1, 1);
                g.setColor(Colors.LIVE_REGULAR);
                g.drawRect(x, (int) Math.round(startRaw - s.live() * stepY), 1, 1);
                g.setColor(Colors.LIVE_CSET);
                g.drawRect(x, (int) Math.round(startRaw - s.collectionSet() * stepY), 1, 1);

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

            items.put("1/2 Used",
                    new RegionStat(0.5f, 0.0f, 0.0f, 0.0f, 0.0f, REGULAR));

            items.put("Fully Used",
                    new RegionStat(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, REGULAR));

            items.put("Fully Used, Trash",
                    new RegionStat(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, TRASH));

            items.put("Fully Live, 100% TLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, REGULAR));

            items.put("Fully Live, 100% GCLAB Allocs",
                    new RegionStat(1.0f, 1.0f, 0.0f, 1.0f, 0.0f, REGULAR));

            items.put("Fully Live, 100% Shared Allocs",
                    new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f, REGULAR));

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
                    new RegionStat(1.0f, 0.3f, 0.0f, 0.0f, 0.0f, PINNED));

            items.put("1/3 Live + Pinned CSet",
                    new RegionStat(1.0f, 0.3f, 0.0f, 0.0f, 0.0f, PINNED_CSET));

            int i = 1;
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

            Color BASE = new Color(0, 0, 0, 20);

            for (int f = 0; f < snapshot.regionCount(); f++) {
                RegionStat s = snapshot.get(f);
                BitSet bs = s.incoming();
                if (bs != null) {
                    for (int t = 0; t < snapshot.regionCount(); t++) {
                        if (bs.get(t)) {
                            int f_rectx = (int) ((f % cols + 0.5) * sqSize);
                            int f_recty = (int) ((f / cols + 0.5) * sqSize);
                            int t_rectx = (int) ((t % cols + 0.5) * sqSize);
                            int t_recty = (int) ((t / cols + 0.5) * sqSize);

                            g.setColor(BASE);
                            g.drawLine(f_rectx, f_recty, t_rectx, t_recty);
                        }
                    }
                }
            }
        }

        public synchronized void renderStats(Graphics g) {
            String status = "";
            switch (snapshot.phase()) {
                case IDLE:
                    status += " (idle)";
                    break;
                case MARKING:
                    status += " (marking)";
                    break;
                case EVACUATING:
                    status += " (evacuating)";
                    break;
                case UPDATE_REFS:
                    status += " (updating refs)";
                    break;
            }

            g.setColor(Color.BLACK);
            g.drawString("Time: " + (snapshot.time() / 1024 / 1024) + " ms", 0, 0 * LINE);
            g.drawString("Status: " + status, 0, 1 * LINE);
            g.drawString("Total: " + (snapshot.total()) + " KB", 0, 2 * LINE);
            g.drawString("Used: " + (snapshot.used()) + " KB", 0, 3 * LINE);
            g.drawString("Live: " + (snapshot.live()) + " KB", 0, 4 * LINE);
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


