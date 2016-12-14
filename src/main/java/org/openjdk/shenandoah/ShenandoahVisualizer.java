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

    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 800;

    static volatile BufferedImage renderedImage;
    private static volatile int width;
    private static volatile int height;

    static class VisPanel extends JPanel {
        public void paint(Graphics g) {
            if (renderedImage != null) {
                g.drawImage(renderedImage, 0, 0, this);
            }
        }
    }

    static class VisPanelListener extends ComponentAdapter {
        public void componentResized(ComponentEvent ev) {
            width = ev.getComponent().getWidth();
            height = ev.getComponent().getHeight();
        }
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) {
            System.err.println("missing VM identifier");
            System.exit(-1);
        }

        DataProvider data = new DataProvider(args[0]);

        VisPanel p = new VisPanel();
        p.addComponentListener(new VisPanelListener());

        JFrame frame = new JFrame();
        frame.getContentPane().add(p, BorderLayout.CENTER);
        frame.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        frame.setVisible(true);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(() -> {
            Snapshot cur = data.snapshot();
            if (!cur.equals(lastSnapshot)) {
                renderedImage = render(cur, width, height);
                lastSnapshot = cur;
            }
            frame.repaint();
        }, 0, 100, TimeUnit.MILLISECONDS);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                service.shutdown();
                frame.dispose();
            }
        });
    }

    static volatile Snapshot lastSnapshot;

    public static BufferedImage render(Snapshot snapshot, int width, int height) {
        int cols = (int) Math.floor(Math.sqrt(snapshot.regionCount()));
        int rows = (int) Math.floor(snapshot.regionCount() / cols);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics g = img.getGraphics();

        final int PAD = 20;
        final int LINE = 30;
        final int PAD_TOP = 100;
        final int PAD_RIGHT = 300;

        int rectWidth = (img.getWidth() - (PAD + PAD_RIGHT)) / cols;
        int rectHeight = (img.getHeight() - (PAD + PAD_TOP)) / rows;

        // Draw white background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw time
        g.setColor(Color.BLACK);
        g.drawString(String.valueOf(snapshot.time()), PAD, PAD);

        // Draw legend

        final int LEGEND_X = width - PAD_RIGHT;
        final int LEGEND_Y = PAD_TOP;
        int LABEL_X = (int) (LEGEND_X + rectWidth * 1.5);

        new RegionStat(0.0, 0.0, true, false, false)
                .render(g, LEGEND_X, LEGEND_Y + 1 * LINE, rectWidth, rectHeight);
        g.drawString("Unused", LABEL_X, LEGEND_Y + 1 * LINE + rectHeight);

        new RegionStat(0.0, 0.0, false, false, false)
                .render(g, LEGEND_X, LEGEND_Y + 2 * LINE, rectWidth, rectHeight);
        g.drawString("Empty", LABEL_X, LEGEND_Y + 2 * LINE + rectHeight);

        new RegionStat(1.0, 1.0, false, false, false)
                .render(g, LEGEND_X, LEGEND_Y + 3 * LINE, rectWidth, rectHeight);
        g.drawString("Live", LABEL_X, LEGEND_Y + 3 * LINE + rectHeight);

        new RegionStat(1.0, 1.0, false, true, false)
                .render(g, LEGEND_X, LEGEND_Y + 4 * LINE, rectWidth, rectHeight);
        g.drawString("Live + Humongous", LABEL_X, LEGEND_Y + 4 * LINE + rectHeight);

        new RegionStat(1.0, 0.3, false, false, false)
                .render(g, LEGEND_X, LEGEND_Y + 5 * LINE, rectWidth, rectHeight);
        g.drawString("1/3 Live", LABEL_X, LEGEND_Y + 5 * LINE + rectHeight);

        new RegionStat(1.0, 0.3, false, false, true)
                .render(g, LEGEND_X, LEGEND_Y + 6 * LINE, rectWidth, rectHeight);
        g.drawString("1/3 Live + In Collection Set", LABEL_X, LEGEND_Y + 6 * LINE + rectHeight);

        // Draw status
        g.setColor(Color.BLACK);
        String status = "";
        if (snapshot.isMarking()) {
            status += " (marking)";
        }
        if (snapshot.isEvacuating()) {
            status += " (evacuating)";
        }
        if (status.isEmpty()) {
            status = " (idle)";
        }
        g.drawString("Status: " + status, PAD, PAD + 1 * LINE);

        // Draw region field

        for (int i = 0; i < snapshot.regionCount(); i++) {
            int rectx = PAD + (i % cols) * rectWidth;
            int recty = PAD + PAD_TOP + (i / rows) * rectHeight;

            RegionStat s = snapshot.get(i);
            s.render(g, rectx, recty, rectWidth, rectHeight);
        }
        g.dispose();

        return img;
    }

}


