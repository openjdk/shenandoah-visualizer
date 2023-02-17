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

public class SnapshotView implements Timed {

    private final long time;
    private final Phase globalPhase;
    private final Phase oldPhase;
    private final Phase youngPhase;
    private final long total;
    private final long committed;
    private final long used;
    private final long live;
    private final long humongous;
    private final long collectionSet;
    private final long trash;
    private final boolean degenActive;
    private final boolean fullActive;
    private final double oldCsetPercentage;

    public SnapshotView(Snapshot s) {
        time = s.time();
        total = total();
        committed = s.committed();
        used = s.used();
        live = s.live();
        humongous = s.humongous();
        collectionSet = s.collectionSet();
        trash = s.trash();
        globalPhase = s.getGlobalPhase();
        youngPhase = s.getYoungPhase();
        oldPhase = s.getOldPhase();
        degenActive = s.isDegenActive();
        fullActive = s.isFullActive();
        oldCsetPercentage = s.percentageOfOldRegionsInCollectionSet();
    }

    public Phase globalPhase() {
        return globalPhase;
    }

    public Phase youngPhase() {
        return youngPhase;
    }

    public Phase oldPhase() {
        return oldPhase;
    }

    public long time() {
        return time;
    }

    public long total() {
        return total;
    }

    public long used() {
        return used;
    }

    public long collectionSet() {
        return collectionSet;
    }

    public double oldCsetPercentage() {
        return oldCsetPercentage;
    }

    public long trash() {
        return trash;
    }

    public long committed() {
        return committed;
    }

    public long humongous() {
        return humongous;
    }

    public long live() {
        return live;
    }

    public boolean isDegenActive() {
        return degenActive;
    }

    public boolean isFullActive() {
        return fullActive;
    }
}
