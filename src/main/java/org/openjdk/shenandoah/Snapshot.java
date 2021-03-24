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

    public Snapshot(long time, long regionSize, List<RegionStat> stats, int status, Histogram histogram) {
        this.time = time;
        this.regionSize = regionSize;
        this.stats = stats;
        this.histogram = histogram;
        this.degenActive = ((status & 0x40) >> 6) == 1;
        this.fullActive  = ((status & 0x80) >> 7) == 1;
        this.globalPhase = Generation.GLOBAL.phase(status);
        this.oldPhase = Generation.OLD.phase(status);
        this.youngPhase = Generation.YOUNG.phase(status);
        System.out.printf("global=%s, old=%s, young=%s, degen=%s, full=%s\n",
                globalPhase, oldPhase, youngPhase, degenActive, fullActive);
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
        long total = 0, old = 0;
        for (RegionStat rs : stats) {
            if (rs.state() == RegionState.CSET || rs.state() == RegionState.PINNED_CSET) {
                if (rs.affiliation() == RegionAffiliation.OLD) {
                    ++old;
                }
                ++total;
            }
        }
        System.out.println("Old regions = " + old + ", total regions = " + total);
        return total == 0 ? 0 : ((double) (old)) / total;
    }
}
