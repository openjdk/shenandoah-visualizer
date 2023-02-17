package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;
import java.util.List;

public class RenderRunner implements Runnable {
    final RenderLive live;
    final RenderPlayback playback;

    final JFrame frame;
    boolean isLive;

    private long lastUpdateNanos;
    private final EventLog<Snapshot> events;

    public RenderRunner(DataProvider data, JFrame frame, ToolbarPanel toolbarPanel, EventLog<Snapshot> events) {
        this.frame = frame;
        live = new RenderLive(data, frame);
        playback = new RenderPlayback(frame, toolbarPanel);
        isLive = true;
        this.events = events;
    }

    public RenderRunner(DataLogProvider data, JFrame frame, ToolbarPanel toolbarPanel, EventLog<Snapshot> events) {
        this.frame = frame;
        live = new RenderLive(frame);
        this.events = events;
        live.closeDataProvider();
        playback = new RenderPlayback(data, frame, toolbarPanel);
        isLive = false;
    }

    public synchronized void loadPlayback(DataLogProvider data) {
        if (isLive) {
            live.closeDataProvider();
        }
        isLive = false;
        playback.loadLogDataProvider(data);
    }

    public synchronized void loadLive(DataProvider data) {
        if (!isLive) {
            isLive = true;
            live.loadDataProvider(data);
        }
    }

    public synchronized void run() {
        long now = System.nanoTime();
        if (lastUpdateNanos != 0) {
            long elapsed = now - lastUpdateNanos;
            events.advanceBy(elapsed, TimeUnit.NANOSECONDS);
        }
        lastUpdateNanos = now;

        if (isLive) {
            live.run();
        } else {
            playback.run();
        }
    }

    public synchronized Snapshot snapshot() {
        return events.latest();
    }

    public void addPopup(RegionPopUp popup) {
        if (isLive) {
            this.live.addPopup(popup);
        } else {
            this.playback.addPopup(popup);
        }
    }

    public void deletePopup(RegionPopUp popup) {
        if (isLive) {
            this.live.deletePopup(popup);
        } else {
            this.playback.deletePopup(popup);
        }
    }

    public List<Snapshot> snapshots() {
        return events.inRange();
    }
}
