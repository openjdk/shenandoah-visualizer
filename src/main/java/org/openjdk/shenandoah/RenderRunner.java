package org.openjdk.shenandoah;

import javax.swing.*;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RenderRunner implements Runnable {
    private long lastUpdateNanos;
    private EventLog<Snapshot> events;
    private boolean isPaused;
    private double playbackSpeed;

    private final Set<JFrame> frames;

    public RenderRunner(JFrame frame, EventLog<Snapshot> events) {
        this.frames = new HashSet<>();
        this.frames.add(frame);
        this.events = events;
        this.playbackSpeed = 1.0;
    }

    public synchronized void loadPlayback(String filePath) {
        EventLog<Snapshot> new_events = new EventLog<>(TimeUnit.MILLISECONDS);
        try {
            DataLogProvider.loadSnapshots(filePath, new_events);
            events = new_events;
            events.stepBy(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
        frames.forEach(JFrame::repaint);
    }

    public synchronized Snapshot snapshot() {
        return events.latest();
    }

    public void addPopup(JFrame popup) {
        frames.add(popup);
    }

    public void deletePopup(JFrame popup) {
        frames.remove(popup);
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
