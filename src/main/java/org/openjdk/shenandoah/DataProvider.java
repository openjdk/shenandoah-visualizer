/*
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

import org.HdrHistogram.Histogram;
import sun.jvmstat.monitor.LongMonitor;
import sun.jvmstat.monitor.MonitorException;
import sun.jvmstat.monitor.MonitoredVm;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DataProvider {
    //default Snapshot as version 2
    private static final long ORIGINAL_VERSION = 1;
    private static final long LATEST_VERSION = 2;
    private static final Snapshot DISCONNECTED = new Snapshot(0, 1024, LATEST_VERSION, Collections.emptyList(), 0, new Histogram(2));
    private final DataConnector connector;

    private final SnapshotHistory history;

    private int maxRegions;
    private long protocolVersion;
    private long maxSize;
    private LongMonitor[] data;
    private LongMonitor status;



    public DataProvider(String id, SnapshotHistory history) {
        connector = new DataConnector(this::setMonitoredVm);
        connector.start();

        // TODO: Run a scheduled task to poll for new snapshots and add them to history.
        this.history = history;
    }

    private void setMonitoredVm(MonitoredVm vm) {
        LongMonitor max_regions_mon = getMonitor(vm,"sun.gc.shenandoah.regions.max_regions");
        maxRegions = (int) max_regions_mon.longValue();
        //Reads in the version of the garbage collector
        LongMonitor protocol_version_mon = getMonitor(vm, "sun.gc.shenandoah.regions.protocol_version");
        if (protocol_version_mon == null) {
            protocolVersion = ORIGINAL_VERSION;
        } else {
            protocolVersion = protocol_version_mon.longValue();
        }
        //System.out.println("The version of the Shenandoah is " + protocolVersion);
        LongMonitor max_size_mon = getMonitor(vm,"sun.gc.shenandoah.regions.region_size");
        maxSize = max_size_mon.longValue();
        status = getMonitor(vm, "sun.gc.shenandoah.regions.status");

        data = new LongMonitor[maxRegions];
        for (int i = 0; i < maxRegions; i++) {
            LongMonitor mon = getMonitor(vm,"sun.gc.shenandoah.regions.region." + i + ".data");
            if (mon != null) {
                data[i] = mon;
            } else {
                throw new IllegalStateException("Insufficient shared memory for all region counters. " +
                        "Try -XX:PerfDataMemorySize=512K or higher when running the monitored program.");
            }
        }
    }

    private static <T> T getMonitor(MonitoredVm vm, String key) {
        try {
            //noinspection unchecked
            return (T) vm.findByName(key);
        } catch (MonitorException e) {
            throw new IllegalStateException(e);
        }
    }

    public Snapshot snapshot() {
        if (!connector.isConnected()) {
            return DISCONNECTED;
        }

        List<RegionStat> stats = new ArrayList<>();
        for (int c = 0; c < maxRegions; c++) {
            stats.add(new RegionStat(data[c].longValue()));
        }

        // Cannot use timestamp value from the dataset itself, because statistics
        // is not reported continuously
        long time = System.currentTimeMillis();

        // These histograms are not thread safe so we pass a copy here. Also, if
        // we ever add a feature to 'replay' sessions, we'll not want these snapshots
        // sharing a histogram.
        //adding version to the Snapshot
        return new Snapshot(time, maxSize, protocolVersion, stats, (int) status.longValue(), connector.getPauseHistogram());
    }

    protected void stopConnector() {
        connector.stop();
    }

    public String status() {
        return connector.status();
    }
}
