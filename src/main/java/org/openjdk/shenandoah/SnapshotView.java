package org.openjdk.shenandoah;

public class SnapshotView {

    private final long time;
    private final Phase phase;
    private final long total;
    private final long used;
    private final long live;
    private final long tlabAllocs;
    private final long gclabAllocs;
    private final long humongous;
    private final long collectionSet;

    public SnapshotView(Snapshot s) {
        time = s.time();
        phase = s.phase();
        total = total();
        used = s.used();
        live = s.live();
        tlabAllocs = s.tlabAllocs();
        gclabAllocs = s.gclabAllocs();
        humongous = s.humongous();
        collectionSet = s.collectionSet();
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

    public long tlabAllocs() {
        return tlabAllocs;
    }

    public long gclabAllocs() {
        return gclabAllocs;
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
