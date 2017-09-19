package org.openjdk.shenandoah;

public class SnapshotView {

    private final long time;
    private final Phase phase;
    private final long total;
    private final long committed;
    private final long used;
    private final long live;
    private final long humongous;
    private final long collectionSet;
    private final long trash;

    public SnapshotView(Snapshot s) {
        time = s.time();
        phase = s.phase();
        total = total();
        committed = s.committed();
        used = s.used();
        live = s.live();
        humongous = s.humongous();
        collectionSet = s.collectionSet();
        trash = s.trash();
    }

    public Phase phase() {
        return phase;
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

    public long collectionSet() {
        return collectionSet;
    }

    public long trash() {
        return trash;
    }

    public long committed() {
        return committed;
    }

    public long humongous() {
        return humongous;
    }

    public long live() {
        return live;
    }

}
