package org.openjdk.shenandoah;

import java.util.List;

public class Snapshot {

    private final long time;
    private final long regionSize;
    private final List<RegionStat> stats;
    private final Phase phase;

    public Snapshot(long time, long regionSize, List<RegionStat> stats, int status) {
        this.time = time;
        this.regionSize = regionSize;
        this.stats = stats;

        switch (status) {
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

    public long tlabAllocs() {
        long r = 0L;
        for (RegionStat rs : stats) {
            r += regionSize * rs.tlabAllocs();
        }
        return r;
    }

    public long gclabAllocs() {
        long r = 0L;
        for (RegionStat rs : stats) {
            r += regionSize * rs.gclabAllocs();
        }
        return r;
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
