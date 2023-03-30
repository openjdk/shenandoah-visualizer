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

import org.HdrHistogram.Histogram;

import javax.swing.*;
import java.awt.*;

import static org.openjdk.shenandoah.LayoutConstants.LINE;

class StatusPanel extends JPanel {
    static final int KILO = 1024;
    private final RenderRunner renderRunner;

    StatusPanel(RenderRunner renderRunner) {
        this.renderRunner = renderRunner;
    }

    static void renderTimeLineLegendItem(Graphics g, Color color, int lineNumber, String label) {
        g.setColor(color);
        int y = (int) (lineNumber * LINE * 1.5);
        g.fillRect(0, y, LayoutConstants.LINE, LayoutConstants.LINE);
        g.setColor(Color.BLACK);
        g.drawString(label, (int) (LayoutConstants.LINE * 1.5), y + LayoutConstants.LINE);
    }

    @Override
    public void paint(Graphics g) {
        Snapshot snapshot = renderRunner.snapshot();
        String mode = snapshot.collectionMode();
        String status = getStatus(snapshot);

        int line = 0;

        g.setColor(Color.BLACK);
        g.drawString("GC: " + status, 0, ++line * LINE);
        g.drawString("Mode: " + mode, 0, ++line * LINE);
        g.drawString("Total: " + (snapshot.total() / KILO) + " MB", 0, ++line * LINE);
        g.drawString(usageStatusLine(snapshot), 0, ++line * LINE);
        g.drawString(liveStatusLine(snapshot), 0, ++line * LINE);

        Histogram histogram = snapshot.getSafepointTime();
        if (histogram != null) {
            String pausesText = String.format("GC Pauses: P100=%d, P95=%d, P90=%d",
                    histogram.getMaxValue(), histogram.getValueAtPercentile(95), histogram.getValueAtPercentile(90));
            g.drawString(pausesText, 0, ++line * LINE);
        }

        renderTimeLineLegendItem(g, Colors.OLD[1], ++line, "Old Marking (OM)");
        renderTimeLineLegendItem(g, Colors.YOUNG[1], ++line, "Young Marking (M)");
        renderTimeLineLegendItem(g, Colors.YOUNG[2], ++line, "Young Evacuation (E)");
        renderTimeLineLegendItem(g, Colors.YOUNG[3], ++line, "Young Update References (UR)");

        renderTimeLineLegendItem(g, Colors.GLOBAL[1], ++line, "Global Marking (M)");
        renderTimeLineLegendItem(g, Colors.GLOBAL[2], ++line, "Global Evacuation (E)");
        renderTimeLineLegendItem(g, Colors.GLOBAL[3], ++line, "Global Update References (UR)");

        renderTimeLineLegendItem(g, Colors.DEGENERATE, ++line, "Degenerated Cycle");
        renderTimeLineLegendItem(g, Colors.FULL, ++line, "Full");
    }

    private static String getStatus(Snapshot snapshot) {
        switch (snapshot.phase()) {
            case IDLE:
                return " Idle";
            case EVACUATING:
                return " Evacuating";
            case UPDATE_REFS:
                return " Updating Refs";
            case UNKNOWN:
                return " Unknown";
            case MARKING:
                String status = " Marking";
                if (snapshot.getOldPhase() == Phase.MARKING) {
                    status += " (Old)";
                }
                return status;
            default:
                throw new IllegalArgumentException("Unknown phase: " + snapshot.phase());
        }
    }

    protected String liveStatusLine(Snapshot snapshot) {
        return "Live (Green): MB: T:" +
                snapshot.live() / KILO + " Y:" +
                snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::live) / KILO + " O:" +
                snapshot.generationStat(RegionAffiliation.OLD, RegionStat::live) / KILO;
    }

    protected String usageStatusLine(Snapshot snapshot) {
        return "Used (White): MB: T:" +
                snapshot.used() / KILO + " Y:" +
                snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::used) / KILO + " O:" +
                snapshot.generationStat(RegionAffiliation.OLD, RegionStat::used) / KILO;
    }
}
