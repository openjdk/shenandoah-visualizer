/*
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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

public class Snapshot {
    public enum Generation {
        YOUNG(4), OLD(2), GLOBAL(0);

        public final int shift;

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

        //decodes for 3 bits older versions of shenandoah collector
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

    public Snapshot(long time, long regionSize, long protocolVersion, List<RegionStat> stats, int status, Histogram histogram) {
        this.time = time;
        this.regionSize = regionSize;
        this.stats = stats;
        this.histogram = histogram;
        this.degenActive = ((status & 0x40) >> 6) == 1;
        this.fullActive  = ((status & 0x80) >> 7) == 1;
        //decodes differently according to different version value
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

    public Phase phase() {
        if (oldPhase != Phase.IDLE) {
            return oldPhase;
        }
        if (youngPhase != Phase.IDLE) {
            return youngPhase;
        }
        return globalPhase;
    }

    public Phase getGlobalPhase() {
        return globalPhase;
    }

    public Phase getYoungPhase() {
        return youngPhase;
    }

    public Phase getOldPhase() {
        return oldPhase;
    }

    public Histogram getSafepointTime() {
        return histogram;
    }

    public boolean isYoungActive() {
        return youngPhase != Phase.IDLE;
    }

    public boolean isOldActive() {
        return oldPhase != Phase.IDLE;
    }

    public boolean isGlobalActive() {
        return globalPhase != Phase.IDLE;
    }

    public boolean isDegenActive() {
        return degenActive;
    }

    public boolean isFullActive() {
        return fullActive;
    }

    public RegionStat get(int i) {
        return stats.get(i);
    }

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

    public int regionCount() {
        return stats.size();
    }

    public long total() {
        return regionSize * regionCount();
    }

    public long used() {
        long used = 0L;
        for (RegionStat rs : stats) {
            used += regionSize * rs.used();
        }
        return used;
    }

    public long generationStat(RegionAffiliation affiliation, Function<RegionStat, Float> stat) {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.affiliation() == affiliation) {
                used += regionSize * stat.apply(rs);
            }
        }
        return used;
    }

    public long committed() {
        long r = 0L;
        for (RegionStat rs : stats) {
            r += (rs.state() == RegionState.EMPTY_UNCOMMITTED) ? 0 : regionSize * rs.used();
        }
        return r;
    }

    public long trash() {
        long r = 0L;
        for (RegionStat rs : stats) {
            r += (rs.state() == RegionState.TRASH) ? rs.used() : 0;
        }
        return r;
    }

    public long collectionSet() {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.state() == RegionState.CSET || rs.state() == RegionState.PINNED_CSET) {
                used += regionSize * rs.live();
            }
        }
        return used;
    }

    public long humongous() {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.state() == RegionState.HUMONGOUS || rs.state() == RegionState.PINNED_HUMONGOUS) {
                used += regionSize * rs.used();
            }
        }
        return used;
    }

    public long live() {
        long live = 0L;
        for (RegionStat rs : stats) {
            live += regionSize * rs.live();
        }
        return live;
    }

    public double percentageOfOldRegionsInCollectionSet() {
        long total_in_cset = 0, old_in_cset = 0, old = 0, total = 0;
        for (RegionStat rs : stats) {
            if (rs.state() == RegionState.CSET || rs.state() == RegionState.PINNED_CSET) {
                if (rs.affiliation() == RegionAffiliation.OLD) {
                    ++old_in_cset;
                }
                ++total_in_cset;
            }
            if (rs.affiliation() == RegionAffiliation.OLD) {
                ++old;
            }
            ++total;
        }
        System.out.printf("cset: %s old/ %s cset total: %s old/ %s total\n",
                old_in_cset, total_in_cset, old, total);
        return total_in_cset == 0 ? 0 : ((double) (old_in_cset)) / total_in_cset;
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
    public int getEmptyUncommittedCount() {
        return emptyUncommittedCount;
    }
    public int getEmptyCommittedCount() {
        return emptyCommittedCount;
    }
    public int getTrashCount() {
        return trashCount;
    }
    public int getTlabCount() {
        return tlabCount;
    }
    public int getGclabCount() {
        return gclabCount;
    }
    public int getPlabCount() {
        return plabCount;
    }
    public int getSharedCount() {
        return sharedCount;
    }
    public int getHumongousCount() {
        return humongousCount;
    }
    public int getPinnedHumongousCount() {
        return pinnedHumongousCount;
    }
    public int getCSetCount() {
        return cSetCount;
    }
    public int getPinnedCount() {
        return pinnedCount;
    }
    public int getPinnedCSetCount() {
        return pinnedCSetCount;
    }
    public int getAge0Count() {
        return age0Count;
    }
    public int getAge3Count() {
        return age3Count;
    }
    public int getAge6Count() {
        return age6Count;
    }
    public int getAge9Count() {
        return age9Count;
    }
    public int getAge12Count() {
        return age12Count;
    }
    public int getAge15Count() {
        return age15Count;
    }
    public int statsSize() {
        return stats.size();
    }
}
