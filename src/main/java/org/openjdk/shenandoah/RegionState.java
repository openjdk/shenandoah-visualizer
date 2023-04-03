/*
 * Copyright (c) 2023, Red Hat, Inc. All rights reserved.
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

enum RegionState {
    EMPTY_UNCOMMITTED,
    EMPTY_COMMITTED,
    REGULAR,
    HUMONGOUS,
    CSET,
    PINNED,
    TRASH,
    PINNED_CSET,
    PINNED_HUMONGOUS;

    static RegionState fromOrdinal(int idx) {
        switch (idx) {
            case 0: return EMPTY_UNCOMMITTED;
            case 1: return EMPTY_COMMITTED;
            case 2: return REGULAR;
            case 3: return HUMONGOUS;
            case 4: return HUMONGOUS;
            case 5: return CSET;
            case 6: return PINNED;
            case 7: return TRASH;
            case 8: return PINNED_CSET;
            case 9: return PINNED_HUMONGOUS;
            default:
                throw new IllegalStateException("Unhandled ordinal: " + idx);
        }
    }

}
