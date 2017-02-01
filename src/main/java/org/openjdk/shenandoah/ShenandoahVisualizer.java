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
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static org.openjdk.shenandoah.RegionFlag.*;

class ShenandoahVisualizer {

    private static final int INITIAL_WIDTH = 1000;
    private static final int INITIAL_HEIGHT = 800;

    static volatile BufferedImage renderedImage;
    private static volatile int width;
    private static volatile int height;
    static final long START_TIME = System.currentTimeMillis();

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
        ScheduledFuture<?> f = service.scheduleAtFixedRate(() -> {
            Snapshot cur = data.snapshot();
            if (!cur.equals(lastSnapshot)) {
                renderedImage = render(cur, width, height);
                lastSnapshot = cur;
            }
            long max = cur.total() / 1024 / 1024;
            System.out.format("%d, %d, %d, %d, %d, %d, %n",
                    cur.time() - START_TIME,
                    max,
                    cur.isMarking() ? max : 0,
                    cur.isEvacuating() ? max : 0,
                    cur.live() / 1024 / 1024,
                    cur.used() / 1024 / 1024
            );
            frame.repaint();
        }, 0, 10, TimeUnit.MILLISECONDS);

        frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                service.shutdown();
                frame.dispose();
            }
        });

        f.get();
    }

    static volatile Snapshot lastSnapshot;

    public static BufferedImage render(Snapshot snapshot, int width, int height) {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

        Graphics g = img.getGraphics();

        final int PAD = 20;
        final int LINE = 20;
        final int PAD_TOP = 100;
        final int PAD_RIGHT = 300;

        int fieldWidth = img.getWidth() - (PAD + PAD_RIGHT);
        int fieldHeight = img.getHeight() - (PAD + PAD_TOP);
        int area = fieldWidth * fieldHeight;
        int sqSize = (int) Math.sqrt(1D * area / snapshot.regionCount());
        int cols = fieldWidth / sqSize;

        // Draw white background
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, width, height);

        // Draw extra data
        g.setColor(Color.BLACK);
        g.drawString("Time: " + (snapshot.time() - START_TIME) + " ms", PAD, PAD);

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

        g.drawString("Total: " + (snapshot.total() / 1024 / 1024) + " MB", PAD, PAD + 2 * LINE);
        g.drawString("Used: " + (snapshot.used() / 1024 / 1024) + " MB", PAD, PAD + 3 * LINE);
        g.drawString("Live: " + (snapshot.live() / 1024 / 1024) + " MB", PAD, PAD + 4 * LINE);

        // Draw legend
        final int LEGEND_X = PAD + fieldWidth + sqSize;
        final int LEGEND_Y = PAD_TOP;

        Map<String, RegionStat> items = new LinkedHashMap<>();

        items.put("Unused",
                new RegionStat(0.0, 0.0, EnumSet.of(UNUSED)));

        items.put("Empty",
                new RegionStat(0.0, 0.0, EnumSet.noneOf(RegionFlag.class)));

        items.put("1/2 Used",
                new RegionStat(0.5, 0.0,  EnumSet.noneOf(RegionFlag.class)));

        items.put("Fully Used",
                new RegionStat(1.0, 0.0,  EnumSet.noneOf(RegionFlag.class)));

        items.put("Fully Used, Recently Allocated",
                new RegionStat(1.0, 0.0,  EnumSet.of(RECENTLY_ALLOCATED)));

        items.put("Fully Live",
                new RegionStat(1.0, 1.0,  EnumSet.noneOf(RegionFlag.class)));

        items.put("Fully Live + Humongous",
                new RegionStat(1.0, 1.0, EnumSet.of(HUMONGOUS)));

        items.put("1/3 Live",
                new RegionStat(1.0, 0.3, EnumSet.noneOf(RegionFlag.class)));

        items.put("1/3 Live + In Collection Set",
                new RegionStat(1.0, 0.3, EnumSet.of(IN_COLLECTION_SET)));

        items.put("1/3 Live + Pinned",
                new RegionStat(1.0, 0.3, EnumSet.of(PINNED)));

        {
            int i = 1;
            for (String key : items.keySet()) {
                int y = (int) (LEGEND_Y + i * sqSize * 1.5);
                items.get(key).render(g, LEGEND_X, y, sqSize, sqSize);
                g.drawString(key, (int) (LEGEND_X + sqSize * 1.5), y + sqSize);
                i++;
            }
        }

        // Draw region field

        for (int i = 0; i < snapshot.regionCount(); i++) {
            int rectx = PAD + (i % cols) * sqSize;
            int recty = PAD + PAD_TOP + (i / cols) * sqSize;

            RegionStat s = snapshot.get(i);
            s.render(g, rectx, recty, sqSize, sqSize);
        }

        if (snapshot.isMarking()) {
            g.setColor(new Color(0, 0, 255, 30));
            g.fillRect(0, 0, width, height);
        }
        if (snapshot.isEvacuating()) {
            g.setColor(new Color(255, 0, 0, 30));
            g.fillRect(0, 0, width, height);
        }

        g.dispose();

        return img;
    }

}


