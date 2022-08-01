/*
 * Copyright (c) 2022, Amazon.com, Inc. All rights reserved.
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

public class RegionPopUp extends JFrame {
    private int regionNumber;
    private float usedLvl;
    private float liveLvl;
    private float tlabLvl;
    private float gclabLvl;
    private float plabLvl;
    private float sharedLvl;
    private RegionState state;
    private long age;
    private RegionAffiliation affiliation;

    Snapshot snapshot;

    public RegionPopUp(Snapshot snapshot, int regionNumber) {
        this.snapshot = snapshot;
        this.regionNumber = regionNumber;
        JPanel detailedState = new JPanel() {
            public void paint(Graphics g) {
                renderDetailedRegion(g);
            }
        };
        this.add(detailedState);
//        System.out.println(regionNumber);
        setSnapshot(snapshot);


    }
    public synchronized void renderDetailedRegion(Graphics g) {
        g.setColor(Color.BLACK);
        g.drawString("Region index: " + regionNumber, 20, 30);
        g.drawString("Used Level: " + usedLvl + " %", 20, 50);
        g.drawString("Live Level: " + liveLvl + " %", 20, 70);
        g.drawString("TLAB Level: " + tlabLvl + " %", 20, 90);
        g.drawString("GCLAB Level: " + gclabLvl + " %", 20, 110);
        g.drawString("PLAB Level: " + plabLvl + " %", 20, 130);
        g.drawString("Shared Level: " + sharedLvl + " %", 20, 150);
        g.drawString("State: " + state, 20, 170);
        g.drawString("Age: " + age, 20, 190);
        g.drawString("Affiliation: " + affiliation, 20, 210);

    }

    public final void setSnapshot(Snapshot snapshot) {
        this.snapshot = snapshot;
        RegionStat regionData = snapshot.get(regionNumber);
        usedLvl = regionData.used() * 100f;
        liveLvl = regionData.live() * 100f;
        tlabLvl = regionData.tlabAllocs() * 100f;
        gclabLvl = regionData.gclabAllocs() * 100f;
        plabLvl = regionData.plabAllocs() * 100f;
        sharedLvl = regionData.sharedAllocs() * 100f;
        state = regionData.state();
        age = regionData.age();
        affiliation = regionData.affiliation();

    }
}

