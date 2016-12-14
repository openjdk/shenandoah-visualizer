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
                render(cur);
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

    public static void render(Snapshot snapshot) {
        int cols = (int) Math.floor(Math.sqrt(snapshot.regionCount()));
        int rows = (int) Math.floor(snapshot.regionCount() / cols);

        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics g = img.getGraphics();

        final int PAD = 20;
        final int LINE = 30;
        final int PAD_TOP = 100;

        // Draw white background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw time
        g.setColor(Color.BLACK);
        g.drawString(String.valueOf(snapshot.time()), PAD, PAD);

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
        int rectWidth = (img.getWidth() - 2*PAD) / cols;
        int rectHeight = (img.getHeight() - (PAD + PAD_TOP)) / rows;

        for (int i = 0; i < snapshot.regionCount(); i++) {
            int rectx = PAD + (i % cols) * rectWidth;
            int recty = PAD + PAD_TOP + (i / rows) * rectHeight;

            RegionStat s = snapshot.get(i);
            s.render(g, rectx, recty, rectWidth, rectHeight);
        }
        g.dispose();

        // publish
        renderedImage = img;
    }

}


