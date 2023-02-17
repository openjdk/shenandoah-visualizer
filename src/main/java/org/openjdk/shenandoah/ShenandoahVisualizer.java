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
 import java.awt.*;
 import java.awt.event.KeyAdapter;
 import java.awt.event.KeyEvent;
 import java.awt.event.WindowAdapter;
 import java.awt.event.WindowEvent;
 import java.util.concurrent.Executors;
 import java.util.concurrent.ScheduledExecutorService;
 import java.util.concurrent.TimeUnit;

 class ShenandoahVisualizer {

    private static final int INITIAL_WIDTH = 2000;
    private static final int INITIAL_HEIGHT = 1600;
    static final int KILO = 1024;
    private static final String PLAYBACK = "Playback";
    private static final String REALTIME = "Realtime";


    public static void main(String[] args) throws Exception {
        // Command line argument parsing
        String vmIdentifier = null;
        String filePath = null;

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
                    filePath = args[i++];
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

        JFrame frame = new JFrame();
        frame.setLayout(new GridBagLayout());
        frame.setTitle("Shenandoah GC Visualizer");
        frame.setSize(INITIAL_WIDTH, INITIAL_HEIGHT);
        final RenderRunner renderRunner;

        if (filePath != null) {
            renderRunner = new RenderRunner(frame);
            renderRunner.loadPlayback(filePath);
        } else {
            renderRunner = new RenderRunner(frame);
            renderRunner.loadLive();
        }


        KeyAdapter keyShortcutAdapter = new KeyboardShortcuts(renderRunner);
        ToolbarPanel toolbarPanel = new ToolbarPanel(renderRunner);

        // Executors
        ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        service.scheduleAtFixedRate(renderRunner,0, 100, TimeUnit.MILLISECONDS);

        JPanel legendPanel = new LegendPanel(renderRunner);

        JPanel statusPanel = new StatusPanel(renderRunner);

        JPanel graphPanel = new GraphPanel(renderRunner);

        JPanel regionsPanel = new RegionPanel(renderRunner, keyShortcutAdapter);

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
                service.shutdown();
                frame.dispose();
            }
        });
    }

     private static class KeyboardShortcuts extends KeyAdapter {
         private final RenderRunner renderRunner;

         public KeyboardShortcuts(RenderRunner renderRunner) {
             this.renderRunner = renderRunner;
         }

         @Override
         public void keyPressed(KeyEvent e) {
             super.keyPressed(e);
             if (e.getKeyCode() == KeyEvent.VK_LEFT) {
                 renderRunner.stepBy(-1);
             }
             if (e.getKeyCode() == KeyEvent.VK_DOWN) {
                 renderRunner.stepBy(-5);
             }
             if (e.getKeyCode() == KeyEvent.VK_SPACE) {
                 renderRunner.togglePlayback();
             }
             if (e.getKeyCode() == KeyEvent.VK_UP) {
                 renderRunner.stepBy(5);
             }
             if (e.getKeyCode() == KeyEvent.VK_RIGHT) {
                 renderRunner.stepBy(1);
             }
             if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                 renderRunner.stepToEnd();
             }
         }
     }
 }


