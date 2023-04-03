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

import java.util.List;
import java.util.function.Function;

class Snapshot implements Timed {
    protected String collectionMode() {
        if (phase() == Phase.IDLE) {
            return "";
        }

        if (isFullActive()) {
            return "Full";
        }

        if (isDegenActive()) {
            return isYoungActive() ? "Degenerate Young" : "Degenerate Global";
        }

        return isYoungActive() ? "Young" : "Global";
    }

    enum Generation {
        YOUNG(4), OLD(2), GLOBAL(0);

        final int shift;

        Generation(int shift) {
            this.shift = shift;
        }

        Phase phase(long status) {
            int phase = (int) ((status >> shift) & 0x3);
            switch (phase) {
                case 0: return Phase.IDLE;
                case 1: return Phase.MARKING;
                case 2: return Phase.EVACUATING;
                case 3: return Phase.UPDATE_REFS;
                default:
                    throw new IllegalArgumentException("Unknown status: " + status);
            }
        }

        // Decode 3 bits for older versions of shenandoah collector
        Phase version1_phase(long status) {
            int phase = (int) status;
            switch (phase) {
                case 0: return Phase.IDLE;
                case 1: return Phase.MARKING;
                case 2: return Phase.EVACUATING;
                case 4: return Phase.UPDATE_REFS;
                default:
                    throw new IllegalArgumentException("Unknown status: " + status);
            }
        }
    }

    private final long time;
    private final long regionSize;
    private final List<RegionStat> stats;
    private final Phase globalPhase;
    private final Phase oldPhase;
    private final Phase youngPhase;
    private final boolean degenActive;
    private final boolean fullActive;
    private final Histogram histogram;

    private int emptyUncommittedCount;
    private int emptyCommittedCount;
    private int trashCount;
    private int tlabCount;
    private int gclabCount;
    private int plabCount;
    private int sharedCount;
    private int humongousCount;
    private int pinnedHumongousCount;
    private int cSetCount;
    private int pinnedCount;
    private int pinnedCSetCount;
    private int age0Count;
    private int age3Count;
    private int age6Count;
    private int age9Count;
    private int age12Count;
    private int age15Count;

    Snapshot(long time, long regionSize, long protocolVersion, List<RegionStat> stats, int status, Histogram histogram) {
        this.time = time;
        this.regionSize = regionSize;
        this.stats = stats;
        this.histogram = histogram;
        this.degenActive = ((status & 0x40) >> 6) == 1;
        this.fullActive  = ((status & 0x80) >> 7) == 1;
        // Decode differently according to different version value
        if (protocolVersion == 1) {
            this.globalPhase = Generation.GLOBAL.version1_phase(status);
            this.oldPhase = Phase.IDLE;
            this.youngPhase = Phase.IDLE;
        } else {
            this.globalPhase = Generation.GLOBAL.phase(status);
            this.oldPhase = Generation.OLD.phase(status);
            this.youngPhase = Generation.YOUNG.phase(status);
        }
        this.stateCounter();
    }

    Phase phase() {
        if (oldPhase != Phase.IDLE) {
            return oldPhase;
        }
        if (youngPhase != Phase.IDLE) {
            return youngPhase;
        }
        return globalPhase;
    }

    Phase getGlobalPhase() {
        return globalPhase;
    }

    Phase getYoungPhase() {
        return youngPhase;
    }

    Phase getOldPhase() {
        return oldPhase;
    }

    Histogram getSafepointTime() {
        return histogram;
    }

    boolean isYoungActive() {
        return youngPhase != Phase.IDLE;
    }

    boolean isDegenActive() {
        return degenActive;
    }

    boolean isFullActive() {
        return fullActive;
    }

    RegionStat get(int i) {
        return stats.get(i);
    }

    @Override
    public long time() {
        return time;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Snapshot snapshot = (Snapshot) o;

        if (time != snapshot.time) return false;
        if (!stats.equals(snapshot.stats)) return false;
        return youngPhase == snapshot.youngPhase
            && globalPhase == snapshot.globalPhase
            && oldPhase == snapshot.oldPhase;
    }

    @Override
    public int hashCode() {
        int result = (int) (time ^ (time >>> 32));
        result = 31 * result + stats.hashCode();
        result = 31 * result + youngPhase.hashCode();
        result = 31 * result + oldPhase.hashCode();
        result = 31 * result + globalPhase.hashCode();
        return result;
    }

    int regionCount() {
        return stats.size();
    }

    long total() {
        return regionSize * regionCount();
    }

    long used() {
        long used = 0L;
        for (RegionStat rs : stats) {
            used += regionSize * rs.used();
        }
        return used;
    }

    long generationStat(RegionAffiliation affiliation, Function<RegionStat, Float> stat) {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.affiliation() == affiliation) {
                used += regionSize * stat.apply(rs);
            }
        }
        return used;
    }

    long collectionSet() {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.state() == RegionState.CSET || rs.state() == RegionState.PINNED_CSET) {
                used += regionSize * rs.live();
            }
        }
        return used;
    }

    long live() {
        long live = 0L;
        for (RegionStat rs : stats) {
            live += regionSize * rs.live();
        }
        return live;
    }

    double percentageOfOldRegionsInCollectionSet() {
        long totalInCset = 0, oldInCset = 0;
        for (RegionStat rs : stats) {
            if (rs.state() == RegionState.CSET || rs.state() == RegionState.PINNED_CSET) {
                if (rs.affiliation() == RegionAffiliation.OLD) {
                    ++oldInCset;
                }
                ++totalInCset;
            }
        }
        return totalInCset == 0 ? 0 : ((double) (oldInCset)) / totalInCset;
    }

    private void stateCounter() {
        emptyUncommittedCount = 0;
        emptyCommittedCount = 0;
        trashCount = 0;
        tlabCount = 0;
        gclabCount = 0;
        plabCount = 0;
        sharedCount = 0;
        humongousCount = 0;
        pinnedHumongousCount = 0;
        cSetCount = 0;
        pinnedCount = 0;
        pinnedCSetCount = 0;
        age0Count = 0;
        age3Count = 0;
        age6Count = 0;
        age9Count = 0;
        age12Count = 0;
        age15Count = 0;
        for (RegionStat rs : stats) {
            switch (rs.state()) {
                case EMPTY_UNCOMMITTED:
                    emptyUncommittedCount++;
                    break;
                case EMPTY_COMMITTED:
                    emptyCommittedCount++;
                    break;
                case TRASH:
                    trashCount++;
                    break;
                case REGULAR:
                    if (rs.affiliation() == RegionAffiliation.YOUNG) {
                        if (rs.maxAllocsYoung() == rs.tlabAllocs()) {
                            tlabCount++;
                        }
                        if ((rs.maxAllocsYoung() == rs.gclabAllocs()) && (rs.maxAllocsYoung() > rs.tlabAllocs())) {
                            gclabCount++;
                        }
                        if (((rs.maxAllocsYoung() == rs.sharedAllocs()) && (rs.maxAllocsYoung() > rs.tlabAllocs()) && (rs.maxAllocsYoung() > rs.gclabAllocs()))) {
                            sharedCount++;
                        }
                    }
                    if (rs.affiliation() == RegionAffiliation.OLD) {
                        if (rs.maxAllocsOld() == rs.plabAllocs()) {
                            plabCount++;
                        }
                        if ((rs.maxAllocsOld() == rs.sharedAllocs()) && (rs.maxAllocsOld() > rs.plabAllocs())) {
                            sharedCount++;
                        }
                    }
                    break;
                case HUMONGOUS:
                    humongousCount++;
                    break;
                case PINNED_HUMONGOUS:
                    pinnedHumongousCount++;
                    break;
                case CSET:
                    cSetCount++;
                    break;
                case PINNED:
                    pinnedCount++;
                    break;
                case PINNED_CSET:
                    pinnedCSetCount++;
                    break;
            }
            if (rs.age() >= 0 && rs.age() < 3) {
                age0Count++;
            }
            if (rs.age() >= 3  && rs.age() < 6) {
                age3Count++;
            }
            if (rs.age() >= 6  && rs.age() < 9) {
                age6Count++;
            }
            if (rs.age() >= 9  && rs.age() < 12) {
                age9Count++;
            }
            if (rs.age() >= 12  && rs.age() < 15) {
                age12Count++;
            }
            if (rs.age() >= 15) {
                age15Count++;
            }
        }
    }
    int getEmptyUncommittedCount() {
        return emptyUncommittedCount;
    }
    int getEmptyCommittedCount() {
        return emptyCommittedCount;
    }
    int getTrashCount() {
        return trashCount;
    }
    int getTlabCount() {
        return tlabCount;
    }
    int getGclabCount() {
        return gclabCount;
    }
    int getPlabCount() {
        return plabCount;
    }
    int getSharedCount() {
        return sharedCount;
    }
    int getHumongousCount() {
        return humongousCount;
    }
    int getPinnedHumongousCount() {
        return pinnedHumongousCount;
    }
    int getCSetCount() {
        return cSetCount;
    }
    int getPinnedCount() {
        return pinnedCount;
    }
    int getPinnedCSetCount() {
        return pinnedCSetCount;
    }
    int getAge0Count() {
        return age0Count;
    }
    int getAge3Count() {
        return age3Count;
    }
    int getAge6Count() {
        return age6Count;
    }
    int getAge9Count() {
        return age9Count;
    }
    int getAge12Count() {
        return age12Count;
    }
    int getAge15Count() {
        return age15Count;
    }
    int statsSize() {
        return stats.size();
    }
}
