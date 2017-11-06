package org.openjdk.shenandoah;

import sun.jvmstat.monitor.*;

import java.util.ArrayList;
import java.util.List;

public class DataProvider {

    private final int maxRegions;
    private final long maxSize;
    private final LongMonitor[] data;
    private final StringMonitor[] matrix;
    private final LongMonitor timestamp;
    private final LongMonitor status;

    public DataProvider(String id) throws Exception {
        MonitoredHost host = MonitoredHost.getMonitoredHost(id);
        MonitoredVm vm = host.getMonitoredVm(new VmIdentifier(id));
        timestamp = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.timestamp");
        LongMonitor max_regions_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.max_regions");
        maxRegions = (int) max_regions_mon.longValue();
        LongMonitor max_size_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.region_size");
        maxSize = max_size_mon.longValue();
        status = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.status");

        data = new LongMonitor[maxRegions];
        matrix = new StringMonitor[maxRegions];
        for (int i = 0; i < maxRegions; i++) {
            LongMonitor mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.region." + i + ".data");
            if (mon != null) {
                data[i] = mon;
            } else {
                throw new IllegalStateException("Insufficient shared memory for all region counters. " +
                        "Try -XX:PerfDataMemorySize=512K or higher when running the monitored program.");
            }

            StringMonitor mtrx = (StringMonitor) vm.findByName("sun.gc.shenandoah.regions.region." + i + ".matrix");
            if (mtrx != null) {
                matrix[i] = mtrx;
            }
        }
    }

    public Snapshot snapshot() {
        List<RegionStat> stats = new ArrayList<>();
        for (int c = 0; c < maxRegions; c++) {
            StringMonitor mtrx = matrix[c];
            stats.add(new RegionStat(data[c].longValue(), (mtrx == null ? "" : mtrx.stringValue())));
        }

        // Cannot use timestamp value from the dataset itself, because statistics
        // is not reported continuously
        long time = System.currentTimeMillis();
        return new Snapshot(time, maxSize, stats, (int) status.longValue());
    }

}
