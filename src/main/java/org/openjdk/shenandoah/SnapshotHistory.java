package org.openjdk.shenandoah;

import java.util.ArrayList;
import java.util.List;

public class SnapshotHistory {
    private final List<Snapshot> snapshots;

    public SnapshotHistory() {
        snapshots = new ArrayList<>();
    }

    public void add(Snapshot snapshot) {
        assert snapshots.isEmpty() || latest().time() < snapshot.time();
        snapshots.add(snapshot);
    }

    Snapshot latest() {
        return snapshots.get(snapshots.size() - 1);
    }
}
