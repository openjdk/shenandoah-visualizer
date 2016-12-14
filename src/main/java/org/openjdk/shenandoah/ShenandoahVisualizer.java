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
import java.awt.image.BufferedImage;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ShenandoahVisualizer {

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

        DataProvider data = new DataProvider(args[0]);

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

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(() -> {
            render(data);
            frame.repaint();
        }, 0, 100, TimeUnit.MILLISECONDS);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                service.shutdown();
                frame.dispose();
            }
        });
    }

    public static void render(DataProvider data) {
        int cols = (int) Math.floor(Math.sqrt(data.maxRegions()));
        int rows = (int) Math.floor(data.maxRegions() / cols);
        isMarking = (data.status() & 0x1) > 0;
        isEvacuating = (data.status() & 0x2) > 0;

        int rectWidth = img.getWidth() / cols;
        int rectHeight = img.getHeight() / rows;
        Graphics g = img.getGraphics();
        for (int i = 0; i < data.maxRegions(); i++) {
            int rectx = (i % cols) * rectWidth;
            int recty = (i / rows) * rectHeight;

            RegionStat s = data.regionStat(i);
            s.render(g, rectx, recty, rectWidth, rectHeight);
        }
        g.dispose();
    }

}


