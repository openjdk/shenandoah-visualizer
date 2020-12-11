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

import com.sun.management.GarbageCollectionNotificationInfo;
import com.sun.management.GcInfo;
import com.sun.tools.attach.AttachNotSupportedException;
import com.sun.tools.attach.VirtualMachine;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.Recorder;
import sun.jvmstat.monitor.*;

import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationEmitter;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.List;

public class DataProvider {

    private static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";
    private static final String SHENANDOAH_PAUSES_BEAN = "java.lang:name=Shenandoah Pauses,type=GarbageCollector";

    private final int maxRegions;
    private final long maxSize;
    private final LongMonitor[] data;
    private final StringMonitor[] matrix;
    private final LongMonitor status;

    private final Recorder histogramRecorder;
    private final Histogram histogram;

    public DataProvider(String id) throws Exception {
        MonitoredVm vm = getMonitoredVm(id);

        subscribeToGarbageCollectorNotifications(vm);

        LongMonitor max_regions_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.max_regions");
        maxRegions = (int) max_regions_mon.longValue();
        LongMonitor max_size_mon = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.region_size");
        maxSize = max_size_mon.longValue();
        status = (LongMonitor) vm.findByName("sun.gc.shenandoah.regions.status");

        histogramRecorder = new Recorder(2);
        histogram = new Histogram(2);

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

    private MonitoredVm getMonitoredVm(String id) throws Exception {
        if (id != null) {
            MonitoredHost host = MonitoredHost.getMonitoredHost(id);
            return host.getMonitoredVm(new VmIdentifier(id));
        }
        HostIdentifier hostId = new HostIdentifier((String)null);
        MonitoredHost host = MonitoredHost.getMonitoredHost(hostId);
        for (Integer vmId: host.activeVms()) {
            MonitoredVm vm = host.getMonitoredVm(new VmIdentifier(String.valueOf(vmId)));
            String jvmArgs = MonitoredVmUtil.jvmArgs(vm);
            if (jvmArgs.contains("ShenandoahRegionSampling")) {
                return vm;
            }
        }
        throw new IllegalStateException("Could not find a JVM running -XX:+ShenandoahRegionSampling!");
    }

    private void subscribeToGarbageCollectorNotifications(MonitoredVm monitoredVm) throws Exception {
        String localJmxAddress = getLocalJmxAddress(monitoredVm);
        JMXServiceURL url = new JMXServiceURL(localJmxAddress);
        JMXConnector connector = JMXConnectorFactory.connect(url);
        MBeanServerConnection server = connector.getMBeanServerConnection();
        GarbageCollectorMXBean collectorBean = ManagementFactory.newPlatformMXBeanProxy(server, SHENANDOAH_PAUSES_BEAN, GarbageCollectorMXBean.class);

        NotificationEmitter emitter = (NotificationEmitter)collectorBean;
        // do NOT install the filter here, it executes on the remote server
        // (and that server won't have access to these classes).
        emitter.addNotificationListener(this::handleNotification, null, collectorBean);
    }

    private String getLocalJmxAddress(MonitoredVm monitoredVm) throws AttachNotSupportedException, IOException {
        int pid = monitoredVm.getVmIdentifier().getLocalVmId();
        VirtualMachine vm = VirtualMachine.attach(String.valueOf(pid));
        String localJmxAddress = (String) vm.getAgentProperties().get(LOCAL_CONNECTOR_ADDRESS_PROP);
        if (localJmxAddress == null) {
            System.out.println("Starting management agent in vm with pid: " + pid + ", patience...");
            vm.startLocalManagementAgent();
            localJmxAddress = (String) vm.getAgentProperties().get(LOCAL_CONNECTOR_ADDRESS_PROP);
        }

        if (localJmxAddress == null) {
            throw new IllegalStateException("Could not get local Jmx address");
        }

        return localJmxAddress;
    }

    private static boolean isGarbageCollectionNotification(Notification notification) {
        return GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType());
    }

    private void handleNotification(Notification notification, Object context) {
        if (!isGarbageCollectionNotification(notification)) {
            return;
        }

        GarbageCollectionNotificationInfo info = asGcNotification(notification);
        GcInfo gcInfo = info.getGcInfo();

        System.out.printf("Id=%s, Name=%s, Action=%s, Cause=%s, Duration=%s\n",
                gcInfo.getId(), info.getGcName(), info.getGcAction(), info.getGcCause(), gcInfo.getDuration());

        histogramRecorder.recordValue(gcInfo.getDuration());
    }

    private GarbageCollectionNotificationInfo asGcNotification(Notification notification) {
        return GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
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

        Histogram temp = histogramRecorder.getIntervalHistogram();
        histogram.add(temp);

        // These histograms are not thread safe so we pass a copy here. Also, if
        // we ever add a feature to 'replay' sessions, we'll not want these snapshots
        // sharing a histogram.
        return new Snapshot(time, maxSize, stats, (int) status.longValue(), histogram.copy());
    }

}
