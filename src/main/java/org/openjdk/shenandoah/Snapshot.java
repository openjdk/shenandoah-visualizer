package org.openjdk.shenandoah;

import java.util.List;

public class Snapshot {

    private final long time;
    private final List<RegionStat> stats;
    private final boolean isMarking;
    private final boolean isEvacuating;

    public Snapshot(long time, List<RegionStat> stats, boolean isMarking, boolean isEvacuating) {
        this.time = time;
        this.stats = stats;
        this.isMarking = isMarking;
        this.isEvacuating = isEvacuating;
    }

    public boolean isMarking() {
        return isMarking;
    }

    public boolean isEvacuating() {
        return isEvacuating;
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

        if (isMarking != snapshot.isMarking) return false;
        if (isEvacuating != snapshot.isEvacuating) return false;
        return stats != null ? stats.equals(snapshot.stats) : snapshot.stats == null;
    }

    @Override
    public int hashCode() {
        int result = stats.hashCode();
        result = 31 * result + (isMarking ? 1 : 0);
        result = 31 * result + (isEvacuating ? 1 : 0);
        return result;
    }

    public int regionCount() {
        return stats.size();
    }
}
