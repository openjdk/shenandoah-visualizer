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
import java.util.LinkedHashMap;
import java.util.Map;

import static org.openjdk.shenandoah.LayoutConstants.LINE;
import static org.openjdk.shenandoah.RegionState.*;

class LegendPanel extends JPanel {
    private final RenderRunner renderRunner;

    LegendPanel(RenderRunner renderRunner) {
        this.renderRunner = renderRunner;
    }

    @Override
    public void paint(Graphics g) {
        Snapshot snapshot = renderRunner.snapshot();
        final int sqSize = LINE;

        Map<String, RegionStat> items = new LinkedHashMap<>();

        items.put("Empty Uncommitted",
                new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_UNCOMMITTED));

        items.put("Empty Committed",
                new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_COMMITTED));

        items.put("Trash",
                new RegionStat(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, TRASH));

        items.put("TLAB Allocs",
                new RegionStat(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, REGULAR));

        items.put("GCLAB Allocs",
                new RegionStat(1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, REGULAR));

        items.put("PLAB Allocs",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, REGULAR));

        items.put("Shared Allocs",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, REGULAR));

        items.put("Humongous",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, HUMONGOUS));

        items.put("Humongous + Pinned",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, PINNED_HUMONGOUS));

        items.put("Collection Set",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, CSET));

        items.put("Pinned",
                new RegionStat(1.0f, 1.0f, 0.3f, 0.0f, 0.0f, 0.0f, PINNED));

        items.put("Pinned CSet",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, PINNED_CSET));

        items.put("Age [0, 3)", new RegionStat(REGULAR, 0));
        items.put("Age [3, 6)", new RegionStat(REGULAR, 3));
        items.put("Age [6, 9)", new RegionStat(REGULAR, 6));
        items.put("Age [9, 12)", new RegionStat(REGULAR, 9));
        items.put("Age [12, 15)", new RegionStat(REGULAR, 12));
        items.put("Age 15", new RegionStat(REGULAR, 15));

        Map<String, Integer> summaryNumbers = new LinkedHashMap<>();


        summaryNumbers.put("Empty Uncommitted", snapshot.getEmptyUncommittedCount());

        summaryNumbers.put("Empty Committed", snapshot.getEmptyCommittedCount());

        summaryNumbers.put("Trash", snapshot.getTrashCount());

        summaryNumbers.put("TLAB Allocs", snapshot.getTlabCount());

        summaryNumbers.put("GCLAB Allocs", snapshot.getGclabCount());

        summaryNumbers.put("PLAB Allocs", snapshot.getPlabCount());

        summaryNumbers.put("Shared Allocs", snapshot.getSharedCount());

        summaryNumbers.put("Humongous", snapshot.getHumongousCount());

        summaryNumbers.put("Humongous + Pinned", snapshot.getPinnedHumongousCount());

        summaryNumbers.put("Collection Set", snapshot.getCSetCount());

        summaryNumbers.put("Pinned", snapshot.getPinnedCount());


        summaryNumbers.put("Pinned CSet", snapshot.getPinnedCSetCount());

        summaryNumbers.put("Age [0, 3)", snapshot.getAge0Count());
        summaryNumbers.put("Age [3, 6)", snapshot.getAge3Count());
        summaryNumbers.put("Age [6, 9)", snapshot.getAge6Count());
        summaryNumbers.put("Age [9, 12)", snapshot.getAge9Count());
        summaryNumbers.put("Age [12, 15)", snapshot.getAge12Count());
        summaryNumbers.put("Age 15", snapshot.getAge15Count());
        int i = 0;
        for (String key : items.keySet()) {
            int y = (int) (i * sqSize * 1.5);
            items.get(key).render(g, 0, y, sqSize, sqSize);
            g.setColor(Color.BLACK);
            g.drawString(key + " total: " + summaryNumbers.get(key).toString(), (int) (sqSize * 1.5), y + sqSize);
            i++;
        }
    }
}
