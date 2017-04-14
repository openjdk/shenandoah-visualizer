package org.openjdk.shenandoah;

import java.util.List;

public class Snapshot {

    private final long time;
    private final long regionSize;
    private final List<RegionStat> stats;
    private final boolean isMarking;
    private final boolean isEvacuating;
    private final boolean isUpdateRefs;

    public Snapshot(long time, long regionSize, List<RegionStat> stats, boolean isMarking, boolean isEvacuating, boolean isUpdateRefs) {
        this.time = time;
        this.regionSize = regionSize;
        this.stats = stats;
        this.isMarking = isMarking;
        this.isEvacuating = isEvacuating;
        this.isUpdateRefs = isUpdateRefs;
    }

    public boolean isMarking() {
        return isMarking;
    }

    public boolean isEvacuating() {
        return isEvacuating;
    }

    public boolean isUpdateRefs() {
        return isUpdateRefs;
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
        if (isMarking != snapshot.isMarking) return false;
        if (isEvacuating != snapshot.isEvacuating) return false;
        if (isUpdateRefs != snapshot.isUpdateRefs) return false;
        return stats != null ? stats.equals(snapshot.stats) : snapshot.stats == null;
    }

    @Override
    public int hashCode() {
        int result = (int) (time ^ (time >>> 32));
        result = 31 * result + (stats != null ? stats.hashCode() : 0);
        result = 31 * result + (isMarking ? 1 : 0);
        result = 31 * result + (isEvacuating ? 1 : 0);
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

    public long recentlyAllocated() {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.flags().contains(RegionFlag.RECENTLY_ALLOCATED)) {
                used += regionSize * rs.used();
            }
        }
        return used;
    }

    public long collectionSet() {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.flags().contains(RegionFlag.IN_COLLECTION_SET)) {
                used += regionSize * rs.used();
            }
        }
        return used;
    }

    public long humongous() {
        long used = 0L;
        for (RegionStat rs : stats) {
            if (rs.flags().contains(RegionFlag.HUMONGOUS)) {
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
