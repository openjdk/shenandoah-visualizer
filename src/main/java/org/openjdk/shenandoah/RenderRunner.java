package org.openjdk.shenandoah;

import javax.swing.*;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class RenderRunner implements Runnable {
    final JFrame frame;
    private long lastUpdateNanos;
    private final EventLog<Snapshot> events;
    private boolean isPaused;
    private double playbackSpeed;

    public RenderRunner(DataProvider data, JFrame frame, EventLog<Snapshot> events) {
        this.frame = frame;
        this.events = events;
        this.playbackSpeed = 1.0;
    }

    public RenderRunner(DataLogProvider data, JFrame frame, EventLog<Snapshot> events) {
        this.frame = frame;
        this.events = events;
        this.playbackSpeed = 1.0;
    }

    public synchronized void loadPlayback(DataLogProvider data) {
    }

    public synchronized void loadLive(DataProvider data) {
    }

    public synchronized void run() {
        long now = System.nanoTime();
        if (lastUpdateNanos != 0) {
            if (!isPaused) {
                long elapsed = (long)((now - lastUpdateNanos) * playbackSpeed);
                events.advanceBy(elapsed, TimeUnit.NANOSECONDS);
            }
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
        playbackSpeed = speed;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void togglePlayback() {
        isPaused = !isPaused;
    }

    public void stepBy(int value) {
        events.stepBy(value);
    }

    public void stepToEnd() {
        events.stepToEnd();
    }

    public double getPlaybackSpeed() {
        return playbackSpeed;
    }

    public int snapshotCount() {
        return events.size();
    }

    public void stepTo(int value) {
        events.stepTo(value);
    }
}
