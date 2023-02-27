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
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class RenderRunner implements Runnable {
    private long lastUpdateNanos;
    private EventLog<Snapshot> events;
    private boolean isPaused;
    private double playbackSpeed;

    private final DataProvider liveData;

    private final Set<JFrame> frames;

    public RenderRunner(JFrame frame) {
        this.frames = new HashSet<>();
        this.frames.add(frame);
        this.playbackSpeed = 1.0;
        this.liveData = new DataProvider();
    }

    public synchronized void loadPlayback(String filePath) {
        EventLog<Snapshot> newEvents = new EventLog<>(TimeUnit.MILLISECONDS);
        try {
            liveData.stopConnector();
            DataLogProvider.loadSnapshots(filePath, newEvents);
            events = newEvents;
            events.stepBy(1);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void loadLive(String vmIdentifier) {
        if (vmIdentifier != null) {
            liveData.setConnectionTarget(vmIdentifier);
        }

        EventLog<Snapshot> newEvents = new EventLog<>(TimeUnit.MILLISECONDS);
        liveData.startConnector();
        newEvents.add(liveData.snapshot());
        newEvents.stepBy(1);
        events = newEvents;
    }

    public synchronized void run() {
        if (liveData.isConnected()) {
            events.add(liveData.snapshot());
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
        return new ArrayList<>(events.inRange());
    }

    public void setPlaybackSpeed(double speed) {
        playbackSpeed = speed;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public boolean isLive() { return liveData.isConnected(); }

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

    public int cursor() {
        return events.cursor();
    }
}
