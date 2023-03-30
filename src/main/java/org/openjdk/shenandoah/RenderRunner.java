/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class RenderRunner implements Runnable {
    private final ScheduledExecutorService service;
    private long lastUpdateNanos;
    private volatile EventLog<Snapshot> events;
    private boolean isPaused;
    private boolean isLive;
    private double playbackSpeed;
    private String playbackStatus = "";

    private Runnable recordingLoaded;

    private final DataProvider liveData;

    private final Set<JFrame> frames;

    RenderRunner(JFrame frame) {
        this.frames = new HashSet<>();
        this.frames.add(frame);
        this.playbackSpeed = 1.0;
        this.liveData = new DataProvider();
        this.events = new EventLog<>(TimeUnit.MILLISECONDS, 1);
        this.service = Executors.newScheduledThreadPool(2);
        service.scheduleAtFixedRate(this, 0, 100, TimeUnit.MILLISECONDS);
    }

    void onRecordingLoaded(Runnable runnable) {
        this.recordingLoaded = runnable;
    }

    synchronized void loadPlayback(String filePath) {
        lastUpdateNanos = 0;
        liveData.stopConnector();
        playbackStatus = "Loading";
        service.submit(() -> {
            DataLogProvider.loadSnapshots(filePath, events);
            isLive = false;
            playbackStatus = "Recorded";
            if (recordingLoaded != null) {
                recordingLoaded.run();
            }
            System.out.println("Loaded event log: " + filePath);
        });
    }

    synchronized void loadLive(String vmIdentifier) {
        if (vmIdentifier != null) {
            liveData.setConnectionTarget(vmIdentifier);
        }

        lastUpdateNanos = 0;
        liveData.startConnector();
        events = new EventLog<>(TimeUnit.MILLISECONDS, 5_000);
        isLive = true;
    }

    public synchronized void run() {
        try {
            if (liveData.isConnected()) {
                Snapshot snapshot = liveData.snapshot();
                if (snapshot != null) {
                    events.add(snapshot);
                }
            }

            long now = System.nanoTime();
            if (lastUpdateNanos != 0) {
                if (!isPaused) {
                    long elapsed = (long)((now - lastUpdateNanos) * playbackSpeed);
                    events.advanceBy(elapsed, TimeUnit.NANOSECONDS);
                }
            }
            lastUpdateNanos = now;
            frames.forEach(JFrame::repaint);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    synchronized Snapshot snapshot() {
        Snapshot latest = events.current();
        return latest != null ? latest : DataProvider.DISCONNECTED;
    }

    void addPopup(JFrame popup) {
        frames.add(popup);
    }

    void deletePopup(JFrame popup) {
        frames.remove(popup);
    }

    List<Snapshot> snapshots() {
        return new ArrayList<>(events.inRange());
    }

    void setPlaybackSpeed(double speed) {
        playbackSpeed = speed;
    }

    boolean isPaused() {
        return isPaused;
    }

    boolean isLive() { return isLive; }

    void togglePlayback() {
        isPaused = !isPaused;
    }

    void stepBy(int value) {
        events.stepBy(value);
    }

    void stepToEnd() {
        events.stepToEnd();
    }

    double getPlaybackSpeed() {
        return playbackSpeed;
    }

    int snapshotCount() {
        return events.size();
    }

    void stepTo(int value) {
        events.stepTo(value);
    }

    int cursor() {
        return events.cursor();
    }

    String status() {
        return isLive ? liveData.status() : playbackStatus;
    }

    void shutdown() {
        service.shutdown();
        frames.forEach(Window::dispose);
        System.exit(0);
    }
}
