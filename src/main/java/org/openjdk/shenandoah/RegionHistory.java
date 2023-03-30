/*
 * Copyright (c) 2023, Amazon.com, Inc. All rights reserved.
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
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.util.List;

class RegionHistory extends JFrame implements DocumentListener {
    static final String DEFAULT_REGION_SELECTION = "0, 100 - 130, 2000";
    static final int MIN_REGION_HEIGHT = 5;
    private static final int MAX_REGION_HEIGHT = 25;

    private final RenderRunner renderRunner;

    private final List<Integer> regions;

    private final RegionSelectionParser parser;

    private final JLabel status;

    RegionHistory(RenderRunner renderRunner, KeyAdapter keyListener) {
        this.renderRunner = renderRunner;
        this.parser = new RegionSelectionParser();
        this.regions = parser.parse(DEFAULT_REGION_SELECTION);

        setSize(600, 500);
        setLayout(new BorderLayout());
        setTitle("Region History");

        Container content = getContentPane();
        var regionSelection = Box.createHorizontalBox();
        regionSelection.add(new JLabel("Regions"));
        JTextField regionInput = new JTextField(DEFAULT_REGION_SELECTION, 25);
        regionInput.getDocument().addDocumentListener(this);
        regionSelection.add(regionInput);
        var historyPanel = new RegionHistoryPanel();
        historyPanel.setFocusable(true);
        historyPanel.addKeyListener(keyListener);
        status = new JLabel();

        content.add(regionSelection, BorderLayout.NORTH);
        content.add(historyPanel, BorderLayout.CENTER);
        content.add(status, BorderLayout.SOUTH);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        onTextUpdated(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        onTextUpdated(e);
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    private void onTextUpdated(DocumentEvent e) {
        try {
            Document document = e.getDocument();
            updateRegionSection(document.getText(0, document.getLength()));
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void updateRegionSection(String expression) {
        try {
            List<Integer> selection = parser.parse(expression);
            regions.clear();
            regions.addAll(selection);
            status.setText("");
        } catch (Exception e) {
            status.setText(e.getMessage());
        }
    }

    private class RegionHistoryPanel extends JPanel {
        @Override
        public void paint(Graphics g) {
            // TODO: Scroll bars?
            // TODO: Tooltips for region detail?
            if (regions.isEmpty()) {
                g.drawString("No regions selected.", 10, 10);
                return;
            }

            Rectangle viewport = g.getClipBounds();
            int regionSquareSize = clamp(viewport.height / regions.size());
            renderRegionLabels(g, regionSquareSize);
            renderRegionHistory(g, viewport, regionSquareSize);
        }

        private void renderRegionHistory(Graphics g, Rectangle viewport, int regionSquareSize) {
            int x = 21;
            List<Snapshot> snapshots = renderRunner.snapshots();
            for (int i = snapshots.size() - 1; i >= 0; i--) {
                x += regionSquareSize;
                Snapshot snapshot = snapshots.get(i);
                int y = 1;
                for (var region : regions) {
                    RegionStat r = snapshot.get(region);
                    r.render(g, x, y, regionSquareSize, regionSquareSize);
                    y += regionSquareSize;
                    if (y > (viewport.height - regionSquareSize)) {
                        // Break a bit early to leave room for a row with timestamps
                        break;
                    }
                }

                y += regionSquareSize;
                if (i % 10 == 0) {
                    g.setColor(Color.BLACK);
                    g.drawString(snapshot.time() + "ms", x, y);
                }

                if (x > viewport.width) {
                    break;
                }
            }
        }

        private void renderRegionLabels(Graphics g, int regionSquareSize) {
            int x = 1;
            int labelStride = regionSquareSize < 10 ? 10 : 1;
            for (int i = 0; i < regions.size(); i += labelStride) {
                int region = regions.get(i);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(region), x, (i + 1) * regionSquareSize);
            }
        }
    }

    private static int clamp(int value) {
        return Math.min(Math.max(MIN_REGION_HEIGHT, value), MAX_REGION_HEIGHT);
    }
}
