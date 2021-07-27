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

import java.lang.System;

public class Stopwatch {
    private long startTime = 0;
//    private long stopTime = 0;
    private long elapsedTime = 0;
    private boolean isRunning = false;

    public void start() {
        if (!isRunning) {
            startTime = System.nanoTime();
            isRunning = true;
        } else {
            System.out.println("Already started stopwatch. Must STOP before START can be enabled again.");
        }

    }

    public void stop() {
        if (isRunning) {
//            stopTime = System.nanoTime();
//            elapsedTime += stopTime - startTime;
            elapsedTime += System.nanoTime() - startTime;
            isRunning = false;
        } else {
            System.out.println("Already stopped stopwatch. Must START before STOP can be enabled again.");
        }

    }

    public void setElapsedTime(long time) {
        if (time >= 0) {
            startTime = System.nanoTime();
            elapsedTime = time;
        } else {
            System.out.println("Cannot set elapsed time to negative value: " + Long.toString(time));
        }
    }

    public void clear() {
        startTime = 0;
//        stopTime = 0;
        elapsedTime = 0;
        isRunning = false;
    }

    public long getElapsedNano() {
        long currentTime = System.nanoTime();
        if (isRunning) {
            return elapsedTime + currentTime - startTime;
        } else {
            return elapsedTime;
        }
    }

    public long getElapsedMilli() {
        return getElapsedNano() / 1000000;
    }

    public long getCurrentMilli() {
        return System.nanoTime() / 1000000;
    }

    public boolean isStarted() {
        return isRunning;
    }
}
