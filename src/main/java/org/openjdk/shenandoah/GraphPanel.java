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
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class GraphPanel extends JPanel {
    public static final int PHASE_LABEL_WIDTH = 50;
    private final RenderRunner renderRunner;

    int graphWidth, graphHeight;

    public GraphPanel(RenderRunner renderRunner) {
        this.renderRunner = renderRunner;

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                graphWidth = ev.getComponent().getWidth();
                graphHeight = ev.getComponent().getHeight();
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        List<Snapshot> snapshots = renderRunner.snapshots();

        int pad = 30;
        int bandHeight = (graphHeight - pad) / 2;
        int bandWidth = graphWidth - PHASE_LABEL_WIDTH;
        int phaseHeight = bandHeight / 4;
        double stepY = 1D * bandHeight / renderRunner.snapshot().total();

        int startRaw = graphHeight - bandHeight - pad;

        int snapshotWidth = 1;
        int snapshotStartX = bandWidth;

        int oneFourth = bandWidth / 4;
        int oneHalf = oneFourth * 2;
        int threeFourths = oneFourth * 3;

        int timelineMarkStartY = bandHeight + 5;
        int timelineMarkEndY = bandHeight + pad - 5;
        int timelineMarkTextOffsetY = bandHeight + 20;

        int phaseLabelOffsetX = bandWidth + 10;
        int phaseLabelOffsetY = bandHeight + pad + 20;

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, bandWidth, graphHeight);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, bandWidth, bandHeight);
        g.fillRect(0, bandHeight + pad, bandWidth, bandHeight);

        for (int i = snapshots.size() - 1; i >= 0 && snapshotStartX >=0; --i) {
            Snapshot snapshot = snapshots.get(i);
            snapshotStartX -= snapshotWidth;

            if (snapshot.getOldPhase() == Phase.MARKING && snapshot.getGlobalPhase() == Phase.IDLE) {
                g.setColor(Colors.OLD[0]);
                g.drawRect(snapshotStartX, bandHeight + pad, snapshotWidth, phaseHeight);
            }

            if (snapshot.percentageOfOldRegionsInCollectionSet() > 0) {
                int height = (int) (bandHeight * snapshot.percentageOfOldRegionsInCollectionSet());
                g.setColor(Colors.OLD[0]);
                g.drawRect(snapshotStartX, 2 * bandHeight + pad - height, snapshotWidth, height);
            }

            g.setColor(getColor(snapshot));
            if (getPhase(snapshot) == Phase.MARKING) {
                g.drawRect(snapshotStartX, bandHeight + pad + phaseHeight, snapshotWidth, phaseHeight);
            }
            if (getPhase(snapshot) == Phase.EVACUATING) {
                g.drawRect(snapshotStartX, bandHeight + pad + 2 * phaseHeight, snapshotWidth, phaseHeight);
            }
            if (getPhase(snapshot) == Phase.UPDATE_REFS) {
                g.drawRect(snapshotStartX, bandHeight + pad + 3 * phaseHeight, snapshotWidth, phaseHeight);
            }

            if (snapshot.isFullActive()) {
                g.setColor(Colors.FULL);
                g.drawRect(snapshotStartX, bandHeight + pad, snapshotWidth, 10);
            } else if (snapshot.isDegenActive()) {
                g.setColor(Colors.DEGENERATE);
                g.drawRect(snapshotStartX, bandHeight + pad, snapshotWidth, 10);
            }

            // Draw these in the upper band.
            g.setColor(Colors.USED);
            g.drawRect(snapshotStartX, (int) Math.round(startRaw - snapshot.used() * stepY), 1, 1);
            g.setColor(Colors.LIVE_REGULAR);
            g.drawRect(snapshotStartX, (int) Math.round(startRaw - snapshot.live() * stepY), 1, 1);
            g.setColor(Colors.LIVE_CSET);
            g.drawRect(snapshotStartX, (int) Math.round(startRaw - snapshot.collectionSet() * stepY), 1, 1);

            g.setColor(Color.GRAY);
            g.drawString("OM", phaseLabelOffsetX, phaseLabelOffsetY);
            g.drawString("M", phaseLabelOffsetX, phaseLabelOffsetY + phaseHeight);
            g.drawString("E", phaseLabelOffsetX, phaseLabelOffsetY + 2 * phaseHeight );
            g.drawString("UR", phaseLabelOffsetX, phaseLabelOffsetY + 3 * phaseHeight );

            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));

            if (snapshotStartX == 0) {
                g2.drawLine(0, timelineMarkStartY, 0, timelineMarkEndY);
                g2.drawString(snapshot.time() + " ms", 0, timelineMarkTextOffsetY);
            } else if (oneFourth == snapshotStartX) {
                g2.drawLine(oneFourth, timelineMarkStartY, oneFourth, timelineMarkEndY);
                g2.drawString(snapshot.time() + " ms", oneFourth + 3, timelineMarkTextOffsetY);
            } else if (oneHalf == snapshotStartX) {
                g2.drawLine(oneHalf, timelineMarkStartY, oneHalf, timelineMarkEndY);
                g2.drawString(snapshot.time() + " ms", oneHalf + 3, timelineMarkTextOffsetY);
            } else if (threeFourths == snapshotStartX) {
                g2.drawLine(threeFourths, timelineMarkStartY, threeFourths, timelineMarkEndY);
                g2.drawString(snapshot.time() + " ms", threeFourths + 3, timelineMarkTextOffsetY);
            }
        }
    }

    protected static Color getColor(Snapshot s) {
        if (s.getYoungPhase() != Phase.IDLE) {
            return Colors.YOUNG[s.getYoungPhase().ordinal()];
        }
        if (s.getGlobalPhase() != Phase.IDLE) {
            return Colors.GLOBAL[s.getGlobalPhase().ordinal()];
        }
        if (s.getOldPhase() != Phase.IDLE) {
            return Colors.OLD[s.getOldPhase().ordinal()];
        }
        return Colors.TIMELINE_IDLE;
    }

    protected static Phase getPhase(Snapshot s) {
        if (s.getYoungPhase() != Phase.IDLE) {
            return s.getYoungPhase();
        }
        if (s.getGlobalPhase() != Phase.IDLE) {
            return s.getGlobalPhase();
        }
        if (s.getOldPhase() != Phase.IDLE) {
            return s.getOldPhase();
        }
        return Phase.UNKNOWN;
    }
}
