/*
 *     Copyright (c) 2023, Amazon.com, Inc. All rights reserved.
 *     DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     This code is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License version 2 only, as
 *     published by the Free Software Foundation.  Oracle designates this
 *     particular file as subject to the "Classpath" exception as provided
 *     by Oracle in the LICENSE file that accompanied this code.
 *
 *     This code is distributed in the hope that it will be useful, but WITHOUT
 *     ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *     FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *     version 2 for more details (a copy is included in the LICENSE file that
 *     accompanied this code).
 *
 *     You should have received a copy of the GNU General Public License version
 *     2 along with this work; if not, write to the Free Software Foundation,
 *     Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *     Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *     or visit www.oracle.com if you need additional information or have any
 *     questions.
 */
package org.openjdk.shenandoah;

import org.HdrHistogram.Histogram;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.openjdk.shenandoah.RegionAffiliation.OLD;
import static org.openjdk.shenandoah.RegionAffiliation.YOUNG;
import static org.openjdk.shenandoah.RegionState.*;

public class CounterTest {
    List<RegionStat> emptyUncommittedAge0 = new ArrayList<>();
    List<RegionStat> emptyCommittedAge3 = new ArrayList<>();
    List<RegionStat> trashAge6 = new ArrayList<>();
    List<RegionStat> humongousAge9 = new ArrayList<>();
    List<RegionStat> pinnedHumongousAge12 = new ArrayList<>();
    List<RegionStat> cSetAge15 = new ArrayList<>();
    List<RegionStat> pinned = new ArrayList<>();
    List<RegionStat> pinnedCSet = new ArrayList<>();
    List<RegionStat> tLab = new ArrayList<>();
    List<RegionStat> gcLab = new ArrayList<>();
    List<RegionStat> pLab = new ArrayList<>();
    List<RegionStat> sharedLabYoung = new ArrayList<>();
    List<RegionStat> sharedLabOld = new ArrayList<>();

    @Before
    public void setup() {
        for (int i = 0; i < 10; i++) {
            emptyUncommittedAge0.add(new RegionStat(EMPTY_UNCOMMITTED, 0));
            emptyCommittedAge3.add(new RegionStat(EMPTY_COMMITTED, 3));
            trashAge6.add(new RegionStat(TRASH, 6));
            humongousAge9.add(new RegionStat(HUMONGOUS, 9));
            pinnedHumongousAge12.add(new RegionStat(PINNED_HUMONGOUS, 12));
            cSetAge15.add(new RegionStat(CSET, 15));
            pinned.add(new RegionStat(PINNED, 0));
            pinnedCSet.add(new RegionStat(PINNED_CSET, 0));
            tLab.add(new RegionStat(1.0f, 1.0f, 0.3f, 0.3f, 0.5f, 0.3f, YOUNG, REGULAR));
            gcLab.add(new RegionStat(1.0f, 1.0f, 0.2f, 0.3f, 0.5f, 0.3f, YOUNG, REGULAR));
            pLab.add(new RegionStat(1.0f, 1.0f, 0.7f, 0.8f, 0.9f, 0.5f, OLD, REGULAR));
            sharedLabYoung.add(new RegionStat(1.0f, 1.0f, 0.2f, 0.3f, 0.5f, 0.7f, YOUNG, REGULAR));
            sharedLabOld.add(new RegionStat(1.0f, 1.0f, 0.2f, 0.9f, 0.5f, 0.7f, OLD, REGULAR));
        }
    }

    @Test
    public void emptyUncommittedCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, emptyUncommittedAge0, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getEmptyUncommittedCount(), 10);
    }
    @Test
    public void emptyCommittedCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, emptyCommittedAge3, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getEmptyCommittedCount(), 10);
    }
    @Test
    public void trashCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, trashAge6, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getTrashCount(), 10);
    }
    @Test
    public void humongousCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, humongousAge9, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getHumongousCount(), 10);
    }
    @Test
    public void pinnedHumongousCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, pinnedHumongousAge12, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getPinnedHumongousCount(), 10);
    }
    @Test
    public void cSetCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, cSetAge15, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getCSetCount(), 10);
    }
    @Test
    public void pinnedCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, pinned, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getPinnedCount(), 10);
    }
    @Test
    public void pinnedCSetCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, pinnedCSet, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getPinnedCSetCount(), 10);
    }
    @Test
    public void age0Counter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, emptyUncommittedAge0, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getAge0Count(), 10);
    }
    @Test
    public void age3Counter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, emptyCommittedAge3, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getAge3Count(), 10);
    }
    @Test
    public void age6Counter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, trashAge6, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getAge6Count(), 10);
    }
    @Test
    public void age9Counter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, humongousAge9, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getAge9Count(), 10);
    }
    @Test
    public void age12Counter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, pinnedHumongousAge12, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getAge12Count(), 10);
    }
    @Test
    public void age15Counter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, cSetAge15, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getAge15Count(), 10);
    }
    @Test
    public void tlabCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, tLab, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getTlabCount(), 10);
    }
    @Test
    public void gclabCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, gcLab, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getGclabCount(), 10);
    }
    @Test
    public void plabCounter_test() {
        Snapshot snapshot = new Snapshot(0, 1024, 1, pLab, 0, new Histogram(2));
        Assert.assertEquals(snapshot.getPlabCount(), 10);
    }
    @Test
    public void sharedCounter_test() {
        Snapshot snapshotYoung = new Snapshot(0, 1024, 1, sharedLabYoung, 0, new Histogram(2));
        Snapshot snapshotOld = new Snapshot(0, 1024, 1, sharedLabOld, 0, new Histogram(2));
        Assert.assertEquals(snapshotYoung.getSharedCount(), 10);
        Assert.assertEquals(snapshotOld.getSharedCount(), 10);
    }
}
