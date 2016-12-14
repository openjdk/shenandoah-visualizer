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

import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import java.util.*;
import sun.jvmstat.monitor.*;

class ShenandoahVisualizer {

    private static final int USED_MASK = 0x3fffffff;
    private static final int USED_SHIFT = 0;
    private static final int LIVE_MASK = 0x3fffffff;
    private static final int LIVE_SHIFT = 30;
    private static final int FLAGS_MASK = 0xf;
    private static final int FLAGS_SHIFT = 60;

    private static final int WIDTH = 1000;
    private static final int HEIGHT = 800;
    static BufferedImage img;
    static boolean isMarking;
    static boolean isEvacuating;

    static boolean doRepaint = true;

    static class VisPanel extends JPanel {
        public void paint(Graphics g) {
            if (img != null) {
                synchronized (ShenandoahVisualizer.class) {
                    g.drawImage(img, 0, 0, this);
                }
            }
        }
    }

    static class StatusPanel extends JPanel {
        public void paint(Graphics g) {
            g.setColor(Color.BLACK);
            g.drawString("marking:", 0, 15);
            if (isMarking) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.GREEN);
            }
            g.fillRect(60, 0, 40, 20);

            g.setColor(Color.BLACK);
            g.drawString("evacuating:", 120, 15);
            if (isEvacuating) {
                g.setColor(Color.RED);
            } else {
                g.setColor(Color.GREEN);
            }
            g.fillRect(220, 0, 40, 20);
            
        }
    }

    static class VisPanelListener extends ComponentAdapter {
        public void componentResized(ComponentEvent ev) {
            // System.out.println("resizing to: " + ev.getComponent().getWidth() + "x" + ev.getComponent().getHeight());
            img = new BufferedImage(ev.getComponent().getWidth(), ev.getComponent().getHeight(), BufferedImage.TYPE_INT_RGB);
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("missing VM identifier");
            System.exit(-1);
        }
        MonitoredHost host = MonitoredHost.getMonitoredHost(args[0]);
        MonitoredVm vm = host.getMonitoredVm(new VmIdentifier(args[0]));
        LongMonitor max_regions_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.max_regions");
        int max_regions = (int) max_regions_mon.longValue();
        LongMonitor max_size_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.region_size");
        long max_size = max_size_mon.longValue();
        LongMonitor status_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.status");

        System.out.println("max_regions: " + max_regions);
        LongMonitor[] mons_data = new LongMonitor[max_regions];
        for (int i = 0; i < max_regions; i++) {
            mons_data[i] = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.region." + i + ".data");
            //System.out.println("region " + i + " used: " + mons[i].longValue());
        }

        img = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        VisPanel p = new VisPanel();
        p.addComponentListener(new VisPanelListener());

        StatusPanel statusPanel = new StatusPanel();
        statusPanel.setPreferredSize(new Dimension(220, 20));

        JFrame frame = new JFrame();
        frame.getContentPane().add(p, BorderLayout.CENTER);
        frame.getContentPane().add(statusPanel, BorderLayout.SOUTH);
        frame.setSize(WIDTH, HEIGHT);
        frame.setVisible(true);
        frame.addWindowListener(new WindowAdapter() {
                public void windowClosing(WindowEvent e) {
                    doRepaint = false;
                }
            });
        int cols = (int) Math.floor(Math.sqrt(max_regions));
        int rows = (int) Math.floor(max_regions / cols);
        while (doRepaint) {
            long start = System.currentTimeMillis();
            synchronized (ShenandoahVisualizer.class) {
                isMarking = (status_mon.longValue() & 0x1) > 0;
                isEvacuating = (status_mon.longValue() & 0x2) > 0;

                int rectWidth = img.getWidth() / cols;
                int rectHeight = img.getHeight() / rows;
                Graphics g = img.getGraphics();
                for (int i = 0; i < max_regions; i++) {
                    int rectx = (i % cols) * rectWidth;
                    int recty = (i / rows) * rectHeight;

                    if (mons_data[i] == null) {
                        System.err.println("Insufficient shared memory for all region counters. Try -XX:PerfDataMemorySize=512K or higher when running the monitored program.");
                        System.exit(-1);
                    }

                    long data = mons_data[i].longValue();
                    long used = (data >>> USED_SHIFT) & USED_MASK;
                    int usedLvl = Math.min(255, (int) (used * 255 / max_size));

                    long live = (data >>> LIVE_SHIFT) & LIVE_MASK;
                    int liveLvl = Math.min(255, (int) (live * 255 / max_size));

                    long stat = (data >>> FLAGS_SHIFT) & FLAGS_MASK;
                    boolean inCset = (stat & 0x1) > 0;
                    boolean humongous = (stat & 0x2) > 0;
                    boolean unused = (stat & 0x4) > 0;

                    g.setColor(Color.WHITE);
                    g.fillRect(rectx, recty, rectWidth, rectHeight);

                    g.setColor(new Color(150, 150, 150));
                    g.fillRect(rectx, recty, rectWidth * usedLvl / 255, rectHeight);

                    g.setColor(new Color(0, 200, 0));
                    g.fillRect(rectx, recty, rectWidth * liveLvl / 255, rectHeight);

                    g.setColor(new Color(0, 100, 0));
                    g.drawLine(rectx + rectWidth * liveLvl / 255, recty, rectx + rectWidth * liveLvl / 255, recty + rectHeight);

                    if (inCset) {
			g.setColor(new Color(255, 255, 0));
			g.fillRect(rectx, recty, rectWidth, rectHeight / 3);
                	g.setColor(Color.BLACK);
			g.drawRect(rectx, recty, rectWidth, rectHeight / 3);
                    }

                    if (humongous) {
			g.setColor(new Color(255, 0, 0));
			g.fillRect(rectx, recty, rectWidth, rectHeight / 3);
                	g.setColor(Color.BLACK);
			g.drawRect(rectx, recty, rectWidth, rectHeight / 3);
                    }

                    if (unused) {
			g.setColor(new Color(0, 0, 0));
                        g.drawLine(rectx, recty, rectx + rectWidth, recty + rectHeight);
                        g.drawLine(rectx, recty + rectHeight, rectx + rectWidth, recty);
                    }
                    g.setColor(Color.BLACK);
                    g.drawRect(rectx, recty, rectWidth, rectHeight);
                }
                g.dispose();
            }
            long duration = System.currentTimeMillis() - start;
            long sleep = 100 - duration;
            if (sleep > 0) {
                Thread.sleep(sleep);
            }
            frame.repaint();
        }
        frame.dispose();
    }
}

