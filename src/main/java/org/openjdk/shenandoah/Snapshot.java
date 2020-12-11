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

public class Snapshot {

    private final long time;
    private final long regionSize;
    private final List<RegionStat> stats;
    private final Phase phase;
    private final boolean youngActive;
    private final boolean degenActive;
    private final boolean fullActive;
    private final Histogram histogram;

    public Snapshot(long time, long regionSize, List<RegionStat> stats, int status, Histogram histogram) {
        this.time = time;
        this.regionSize = regionSize;
        this.stats = stats;
        this.histogram = histogram;

        this.youngActive = ((status & 0x8) >> 3) == 1;
        this.degenActive = ((status & 0x10) >> 4) == 1;
        this.fullActive  = ((status & 0x20) >> 5) == 1;

        switch (status & 0x7) {
            case 0x0:
                this.phase = Phase.IDLE;
                break;
            case 0x1:
                this.phase = Phase.MARKING;
                break;
            case 0x2:
                this.phase = Phase.EVACUATING;
                break;
            case 0x4:
                this.phase = Phase.UPDATE_REFS;
                break;
            default:
                this.phase = Phase.UNKNOWN;
                break;
        }
    }

    public Phase phase() {
        return phase;
    }

    public Histogram getSafepointTime() {
        return histogram;
    }

    public boolean isYoungActive() {
        return youngActive;
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
        return phase == snapshot.phase;
    }

    @Override
    public int hashCode() {
        int result = (int) (time ^ (time >>> 32));
        result = 31 * result + stats.hashCode();
        result = 31 * result + phase.hashCode();
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
}
