/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Purpose of this class is to maintain a JMX connection to a JVM running
 * shenandoah region sampling.
 */
class DataConnector {
    private static final String LOCAL_CONNECTOR_ADDRESS_PROP = "com.sun.management.jmxremote.localConnectorAddress";
    private static final String SHENANDOAH_PAUSES_BEAN = "java.lang:name=Shenandoah Pauses,type=GarbageCollector";

    private final Recorder histogramRecorder;
    private final Histogram histogram;

    private final Consumer<MonitoredVm> monitoredVmConsumer;

    private volatile State status;
    private volatile boolean shouldRun;

    private final Executor executor;

    private volatile String targetVmIdentifier;
    private JMXConnector jmxConnector;

    enum State {
        SEARCHING, CONNECTING, CONNECTED, DISCONNECTING, DISCONNECTED
    }

    DataConnector(Consumer<MonitoredVm> monitoredVmConsumer) {
        this.monitoredVmConsumer = monitoredVmConsumer;
        this.histogramRecorder = new Recorder(2);
        this.histogram = new Histogram(2);
        this.shouldRun = true;
        this.status = State.DISCONNECTED;
        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            t.setName("JmxConnectionManager");
            return t;
        });
    }

    void connectTo(String id) {
        targetVmIdentifier = id;
    }

    private void searchForShenandoahVm() {
        while (shouldRun) {
            try {
                MonitoredVm vm = findShenandoahVm();
                if (vm != null) {
                    transitionTo(State.CONNECTING);
                    MBeanServerConnection server = createServiceConnection(vm);
                    subscribeToGarbageCollectorNotifications(server);
                    monitoredVmConsumer.accept(vm);
                    shouldRun = false;
                    transitionTo(State.CONNECTED);
                } else {
                    Thread.sleep(250);
                }
            } catch (InterruptedException e) {
                shouldRun = false;
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("Connection task completed: " + status);
    }

    boolean isConnected() {
        return status == State.CONNECTED;
    }

    private void transitionTo(State newState) {
        System.out.println("Change status to: " + newState);
        status = newState;
    }

    void start() {
        if (status == State.DISCONNECTED) {
            transitionTo(State.SEARCHING);
            shouldRun = true;
            executor.execute(this::searchForShenandoahVm);
        }
    }

    void stop() {
        shouldRun = false;
        if (status == State.CONNECTED) {
            executor.execute(() -> {
                try {
                    transitionTo(State.DISCONNECTING);
                    jmxConnector.close();
                    transitionTo(State.DISCONNECTED);
                } catch (IOException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        }
    }

    String status() {
        return status.toString();
    }

    Histogram getPauseHistogram() {
        Histogram temp = histogramRecorder.getIntervalHistogram();
        histogram.add(temp);
        return histogram.copy();
    }

    private MonitoredVm findShenandoahVm() throws Exception {
        HostIdentifier hostId = new HostIdentifier((String)null);
        MonitoredHost host = MonitoredHost.getMonitoredHost(hostId);

        if (targetVmIdentifier != null) {
            try {
                MonitoredVm vm = host.getMonitoredVm(new VmIdentifier(targetVmIdentifier));
                String jvmArgs = MonitoredVmUtil.jvmArgs(vm);
                if (jvmArgs.contains("ShenandoahRegionSampling")) {
                    System.out.println("Connecting to given vm: " + targetVmIdentifier);
                    return vm;
                } else {
                    System.out.println("Given identifier for vm " + targetVmIdentifier + " does not have ShenandoahRegionSampling enabled.");
                }
            } finally {
                targetVmIdentifier = null;
            }
        }

        for (Integer vmId: host.activeVms()) {
            MonitoredVm vm = host.getMonitoredVm(new VmIdentifier(String.valueOf(vmId)));
            String jvmArgs = MonitoredVmUtil.jvmArgs(vm);
            if (jvmArgs.contains("ShenandoahRegionSampling")) {
                System.out.println("Found vm running shenandoah region sampling: " + vm);
                return vm;
            }
        }
        System.out.println("Could not find a JVM running -XX:+ShenandoahRegionSampling!");
        return null;
    }

    private MBeanServerConnection createServiceConnection(MonitoredVm monitoredVm) throws AttachNotSupportedException, IOException {
        String localJmxAddress = getLocalJmxAddress(monitoredVm);
        JMXServiceURL url = new JMXServiceURL(localJmxAddress);
        jmxConnector = JMXConnectorFactory.connect(url);
        MBeanServerConnection server = jmxConnector.getMBeanServerConnection();
        jmxConnector.addConnectionNotificationListener(this::handleConnectionNotification, null, server);
        return server;
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

    private void handleConnectionNotification(Notification notification, Object serverConnection) {
        System.out.println(notification.getType() + ": " + notification.getMessage());
        if (JMXConnectionNotification.CLOSED.equals(notification.getType())) {
            transitionTo(State.DISCONNECTED);
        }
    }

    private void subscribeToGarbageCollectorNotifications(MBeanServerConnection server) throws Exception {
        GarbageCollectorMXBean collectorBean = ManagementFactory.newPlatformMXBeanProxy(server, SHENANDOAH_PAUSES_BEAN, GarbageCollectorMXBean.class);
        NotificationEmitter emitter = (NotificationEmitter)collectorBean;
        // do NOT install the filter here, it executes on the remote server
        // (and that server won't have access to these classes).
        emitter.addNotificationListener(this::handleNotification, null, collectorBean);
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

    private static boolean isGarbageCollectionNotification(Notification notification) {
        return GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION.equals(notification.getType());
    }

    private GarbageCollectionNotificationInfo asGcNotification(Notification notification) {
        return GarbageCollectionNotificationInfo.from((CompositeData) notification.getUserData());
    }
}
