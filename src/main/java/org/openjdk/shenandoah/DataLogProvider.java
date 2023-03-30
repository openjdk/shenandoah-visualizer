/*
 * Copyright (c) 2023, Amazon.com, Inc. All rights reserved.
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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

class DataLogProvider {
    private static final long LATEST_VERSION = 2;

    static void loadSnapshots(String filePath, EventLog<Snapshot> eventLog) {
        if (!isValidPath(filePath)) {
            throw new IllegalArgumentException("Invalid file path supplied. Please try again.");
        }

        long protocolVersion = LATEST_VERSION;
        var events = new ArrayList<Snapshot>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String metaDataLine = br.readLine(); // timestamp status numRegions regionSize

            while (metaDataLine != null && metaDataLine.trim().length() > 0) {
                metaDataLine = processLoggingTag(metaDataLine);
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
                regionDataLine = processLoggingTag(regionDataLine);
                String[] regionData = regionDataLine.trim().split(" ");

                long tsMilli = TimeUnit.NANOSECONDS.toMillis(metaData[0]);
                long regionSize = metaData[3];
                int status = Math.toIntExact(metaData[1]);
                events.add(new Snapshot(tsMilli, regionSize, protocolVersion, processRegionStats(regionData), status, null));

                metaDataLine = br.readLine();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        eventLog.load(TimeUnit.MILLISECONDS, events);
    }

    private static boolean isValidPath(String name) {
        return name != null && Files.isReadable(Paths.get(name));
    }

    static String processLoggingTag(String data) {
        if (data.lastIndexOf("]") != -1) {
            int startIndex = data.lastIndexOf("]") + 2;
            return data.substring(startIndex);
        } else {
            return data;
        }
    }

    private static long[] processLongData(String data) throws NumberFormatException {
        String[] dataArray = data.trim().split(" ", 5000);
        long[] longArray = new long[dataArray.length];

        for (int i = 0; i < dataArray.length; i++) {
            longArray[i] = Long.parseLong(dataArray[i]);
        }

        return longArray;
    }

    private static List<RegionStat> processRegionStats(String[] regionData) throws NumberFormatException {
        List<RegionStat> stats = new ArrayList<>(regionData.length);
        for (String d : regionData) {
            stats.add(new RegionStat(Long.parseLong(d)));
        }
        return stats;
    }
}
