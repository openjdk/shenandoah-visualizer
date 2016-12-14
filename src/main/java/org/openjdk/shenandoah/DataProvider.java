package org.openjdk.shenandoah;

import sun.jvmstat.monitor.*;

public class DataProvider {

    private final int maxRegions;
    private final long maxSize;
    private final LongMonitor[] data;
    private final LongMonitor status;

    public DataProvider(String id) throws Exception {
        MonitoredHost host = MonitoredHost.getMonitoredHost(id);
        MonitoredVm vm = host.getMonitoredVm(new VmIdentifier(id));
        LongMonitor max_regions_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.max_regions");
        maxRegions = (int) max_regions_mon.longValue();
        LongMonitor max_size_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.region_size");
        maxSize = max_size_mon.longValue();
        status = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.status");

        data = new LongMonitor[maxRegions];
        for (int i = 0; i < maxRegions; i++) {
            LongMonitor mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.region." + i + ".data");
            if (mon != null) {
                data[i] = mon;
            } else {
                throw new IllegalStateException("Insufficient shared memory for all region counters. " +
                        "Try -XX:PerfDataMemorySize=512K or higher when running the monitored program.");
            }
        }
    }

    public int maxRegions() {
        return maxRegions;
    }

    public long status() {
        return status.longValue();
    }

    public RegionStat regionStat(int i) {
        return new RegionStat(maxSize, data[i].longValue());
    }
}
