package org.openjdk.shenandoah;

import java.util.List;

public class SnapshotView {

    private final long time;
    private final boolean isMarking;
    private final boolean isEvacuating;
    private final long total;
    private final long used;
    private final long live;
    private final long recentlyAllocated;
    private final long humongous;
    private final long collectionSet;

    public SnapshotView(Snapshot s) {
        this.time = s.time();
        this.isEvacuating = s.isEvacuating();
        this.isMarking = s.isMarking();
        total = total();
        used = s.used();
        live = s.live();
        recentlyAllocated = s.recentlyAllocated();
        humongous = s.humongous();
        collectionSet = s.collectionSet();
    }

    public boolean isMarking() {
        return isMarking;
    }

    public boolean isEvacuating() {
        return isEvacuating;
    }

    public long time() {
        return time;
    }

    public long total() {
        return total;
    }

    public long used() {
        return used;
    }

    public long recentlyAllocated() {
        return recentlyAllocated;
    }

    public long collectionSet() {
        return collectionSet;
    }

    public long humongous() {
        return humongous;
    }

    public long live() {
        return live;
    }
}
