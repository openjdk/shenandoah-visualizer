/*
 * ====
 *     Copyright (c) 2021, Amazon.com, Inc. All rights reserved.
 *     DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     This code is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License version 2 only, as
 *     published by the Free Software Foundation.  Oracle designates this
 *     particular file as subject to the "Classpath" exception as provided
 *     by Oracle in the LICENSE file that accompanied this code.
 *
 *     This code is distributed in the hope that it will be useful, but WITHOUT
 *     ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *     FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *     version 2 for more details (a copy is included in the LICENSE file that
 *     accompanied this code).
 *
 *     You should have received a copy of the GNU General Public License version
 *     2 along with this work; if not, write to the Free Software Foundation,
 *     Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *     Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *     or visit www.oracle.com if you need additional information or have any
 *     questions.
 * ====
 *
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.io.*;
import java.lang.IllegalArgumentException;
import java.lang.System;
import java.util.concurrent.TimeUnit;

public class DataLogProvider {
    //default Snapshot as version 2
    private static final long LATEST_VERSION = 2;
    private static final Snapshot DISCONNECTED = new Snapshot(0, 1024, LATEST_VERSION, Collections.emptyList(), 0, null);

    private static final String START = "START";
    private static final String STOP = "STOP";
    private static final String CLEAR = "CLEAR";


    private List<Snapshot> snapshots;
    private HashMap<Long, Integer> snapshotsIndexByTime;
    public final Stopwatch stopwatch;
    private int snapshotsIndex = -1;
    private Snapshot currSnapshot = DISCONNECTED;


    public DataLogProvider(String path) throws IOException, NumberFormatException {
        String filePath = path;
        if (!isValidPath(filePath)) {
            throw new FileNotFoundException("Invalid file path supplied. Please try again.");
        }

        this.snapshots = new ArrayList<>();
        this.snapshotsIndexByTime = new HashMap<>();
        int index = 0;
        long protocolVersion = LATEST_VERSION;

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String metaDataLine = br.readLine(); // timestamp status numRegions regionSize

            while (metaDataLine != null && metaDataLine.trim().length() > 0) {
                long[] metaData = processLongData(metaDataLine);
                if (metaData.length != 5 && metaData.length != 4) {
                    throw new IllegalArgumentException(String.format("Metadata line has %d values. Expected 5 values.", metaData.length));
                } else if (metaData.length > 4) {
                    protocolVersion = metaData[4];
                }

                String regionDataLine = br.readLine();
                if (regionDataLine == null) {
                    throw new NullPointerException("Invalid file format: Missing region data.");
                }
                String[] regionData = regionDataLine.trim().split(" ");

                long tsMilli = TimeUnit.NANOSECONDS.toMillis(metaData[0]);
                snapshots.add(new Snapshot(tsMilli,
                        metaData[3], protocolVersion,
                        processRegionStats(regionData),
                        Math.toIntExact(metaData[1]),
                        null));
                snapshotsIndexByTime.put(tsMilli, index++);

                metaDataLine = br.readLine();
            }
        }

        if (snapshots.size() > 0) {
            snapshotsIndex = 0;
        }

        stopwatch = new Stopwatch();
    }

    public void controlStopwatch(String command) {
        if (STOP.equals(command)) {
            stopwatch.stop();
        } else if (START.equals(command)) {
            stopwatch.start();
        } else if (CLEAR.equals(command)) {
            stopwatch.clear();
        }
    }

    public void setStopwatchTime(long ns) {
        stopwatch.setElapsedTime(ns);
    }

    public void setSpeed(double speed) {
        stopwatch.setSpeedMultiplier(speed);
    }

    public boolean snapshotTimeHasOccurred(Snapshot s) {
        return s.time() <= stopwatch.getElapsedMilli();
    }

    public Snapshot getSnapshotAtTime(long ms) {
        return snapshots.get(snapshotsIndexByTime.get(ms));
    }

    private boolean isValidPath(String name) {
        return name != null && Files.isReadable(Paths.get(name));
    }

    private long[] processLongData(String data) throws NumberFormatException {
        String[] dataArray = data.trim().split(" ", 5000);
        long[] longArray = new long[dataArray.length];

        for (int i = 0; i < dataArray.length; i++) {
            longArray[i] = Long.parseLong(dataArray[i]);
        }

        return longArray;
    }

    private List<RegionStat> processRegionStats(String[] regionData) throws NumberFormatException {
        List<RegionStat> stats = new ArrayList<>(regionData.length);
        for (String d : regionData) {
            stats.add(new RegionStat(Long.parseLong(d)));
        }
        return stats;
    }

    public boolean isEndOfSnapshots() {
        return snapshotsIndex >= snapshots.size();
    }

    public Snapshot snapshot() {
        if (!stopwatch.isStarted()) {
            stopwatch.start();
        }
        if (snapshotsIndex == -1) {
            System.out.println("No Shenandoah snapshots in file. Choose valid log file.");
            return DISCONNECTED;
        } else if (snapshotsIndex >= 0 && snapshotsIndex < snapshots.size()) {
            Snapshot tempSnapshot = snapshots.get(snapshotsIndex);
            if (snapshotTimeHasOccurred(tempSnapshot)) {
                currSnapshot = tempSnapshot;
                snapshotsIndex++;
            }
            return currSnapshot;
        } else {
            return currSnapshot;
        }
    }

    public Snapshot getNextSnapshot() {
        if (snapshotsIndex == -1) {
            return DISCONNECTED;
        } else if (snapshotsIndex >= 0 && snapshotsIndex < snapshots.size()) {
            Snapshot tempSnapshot = snapshots.get(snapshotsIndex);
            currSnapshot = tempSnapshot;
            snapshotsIndex++;
        }
        return currSnapshot;
    }
}

