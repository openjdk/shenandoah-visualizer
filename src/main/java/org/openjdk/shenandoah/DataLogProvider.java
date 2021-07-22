/*
 * ====
 *     Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.*;
import java.lang.IllegalArgumentException;
import java.lang.System;

public class DataLogProvider {
    // take in file
    // parse snapshot header and data until reach EOL or next snapshot
    // create snapshot with data and header

    // metadata: timestamp status numRegions regionSize
    // data: region_data
    private static final Snapshot DISCONNECTED = new Snapshot(0, 1024, Collections.emptyList(), 0, null);

    private String filePath;
    private BufferedReader br;
    private List<Snapshot> snapshots;
    private int snapshotsIndex = -1;
    private long startTime = -1;
    private Snapshot currSnapshot = DISCONNECTED;

    public DataLogProvider(String path) throws IOException, NumberFormatException {
        filePath = path;
        if (!isValidPath(filePath)) {
            throw new FileNotFoundException("Invalid file path supplied. Please try again.");
        }

        this.snapshots = new ArrayList<>();

        br = new BufferedReader(new FileReader(filePath));
        String metaDataLine = br.readLine(); // timestamp status numRegions regionSize

        while (metaDataLine != null && metaDataLine.trim().length() > 0) {
            long[] metaData = processLongData(metaDataLine);
            if (metaData.length != 4) {
                throw new IllegalArgumentException(String.format("Metadata line has %d values. Expected 4 values.", metaData.length));
            }

            String regionDataLine = br.readLine();
            if (regionDataLine == null) {
                throw new NullPointerException("Invalid file format: Missing region data.");
            }
            String[] regionData = regionDataLine.trim().split(" ");

            snapshots.add(new Snapshot(metaData[0] / 1000000,
                                       metaData[3],
                                       processRegionStats(regionData),
                                       Math.toIntExact(metaData[1]),
                              null));

            metaDataLine = br.readLine();
        }
        if (snapshots.size() > 0) snapshotsIndex = 0;
    }

    private boolean isValidPath(String name) {
        if (name == null) {
            return false;
        }
        return true;
    }

    private long[] processLongData(String data) throws NumberFormatException {
        String[] dataArray = data.trim().split(" ");
        long[] longArray = new long[dataArray.length];

        for (int i = 0; i < dataArray.length; i++) {
            longArray[i] = Long.parseLong(dataArray[i]);
        }

        return longArray;
    }

    private List<RegionStat> processRegionStats(String[] regionData) throws NumberFormatException {
        List<RegionStat> stats = new ArrayList<>();
        for (String d : regionData) {
            stats.add(new RegionStat(Long.parseLong(d)));
        }
        return stats;
    }

    public Snapshot snapshot() {
        long currTime = System.nanoTime() / 1000000;
        if (startTime == -1) {
            startTime = currTime;
        }
        if (snapshotsIndex == -1) {
            System.out.println("No Shenandoah snapshots in file. Choose valid log file.");
            return DISCONNECTED;
        } else if (snapshotsIndex >= 0 && snapshotsIndex < snapshots.size()) {
            Snapshot tempSnapshot = snapshots.get(snapshotsIndex);
            if (tempSnapshot.time() <= currTime - startTime) {
                currSnapshot = tempSnapshot;
                snapshotsIndex++;
            }
            return currSnapshot;
        } else {
            return currSnapshot;
        }

    }
}

