package org.openjdk.shenandoah;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RenderRunner implements Runnable {
    final JFrame frame;
    private long lastUpdateNanos;
    private final EventLog<Snapshot> events;
    private boolean isPaused;

    public RenderRunner(DataProvider data, JFrame frame, EventLog<Snapshot> events) {
        this.frame = frame;
        this.events = events;
    }

    public RenderRunner(DataLogProvider data, JFrame frame, EventLog<Snapshot> events) {
        this.frame = frame;
        this.events = events;
    }

    public synchronized void loadPlayback(DataLogProvider data) {
    }

    public synchronized void loadLive(DataProvider data) {
    }

    public synchronized void run() {
        long now = System.nanoTime();
        if (lastUpdateNanos != 0) {
            long elapsed = now - lastUpdateNanos;
            events.advanceBy(elapsed, TimeUnit.NANOSECONDS);
        }
        lastUpdateNanos = now;
        frame.repaint();
    }

    public synchronized Snapshot snapshot() {
        return events.latest();
    }

    public void addPopup(RegionPopUp popup) {
    }

    public void deletePopup(RegionPopUp popup) {
    }

    public List<Snapshot> snapshots() {
        return events.inRange();
    }

    public void setPlaybackSpeed(double speed) {
        // TODO: multiply playback speed.
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void togglePlayback() {
        isPaused = !isPaused;
    }

    public void step(int value) {
        events.step(value);
    }

    public void stepToEnd() {
        events.step(Integer.MAX_VALUE);
    }

    public double getPlaybackSpeed() {
        return 0;
    }
}
