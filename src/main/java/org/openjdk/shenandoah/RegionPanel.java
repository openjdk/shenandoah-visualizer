/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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
 */
package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegionPanel extends JPanel {
    private final RenderRunner renderRunner;

    int regionWidth, regionHeight;

    public RegionPanel(RenderRunner renderRunner, KeyAdapter keyboardShortCuts) {
        this.renderRunner = renderRunner;

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                regionWidth = ev.getComponent().getWidth();
                regionHeight = ev.getComponent().getHeight();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Snapshot snapshot = renderRunner.snapshot();
                int area = regionWidth * regionHeight;
                int sqSize = Math.max(1, (int) Math.sqrt(1D * area / snapshot.regionCount()));
                int cols = regionWidth / sqSize;
                int regionNumber = (e.getX() / sqSize) + ((e.getY() / sqSize) * cols) ;
                if (regionNumber >= 0 && regionNumber < snapshot.statsSize()) {
                    RegionPopUp popup = new RegionPopUp(regionNumber, renderRunner);
                    popup.setSize(450, 450);
                    popup.setLocation(e.getX(), e.getY());
                    popup.setVisible(true);
                    popup.addKeyListener(keyboardShortCuts);
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
    }

    @Override
    public void paint(Graphics g) {
        Snapshot snapshot = renderRunner.snapshot();
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
}
