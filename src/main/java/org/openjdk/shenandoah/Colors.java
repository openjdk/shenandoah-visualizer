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

import java.awt.*;

public class Colors {

    static final Color TIMELINE_IDLE        = Color.BLACK;
    static final Color TIMELINE_MARK        = new Color(100, 100, 0);
    static final Color TIMELINE_EVACUATING  = new Color(100, 0, 0);
    static final Color TIMELINE_UPDATEREFS  = new Color(0, 100, 100);

    static final Color SHARED_ALLOC           = new Color(0, 250, 250);
    static final Color SHARED_ALLOC_BORDER    = new Color(0, 191, 190);
    static final Color TLAB_ALLOC           = new Color(0, 200, 0);
    static final Color TLAB_ALLOC_BORDER    = new Color(0, 100, 0);
    static final Color GCLAB_ALLOC          = new Color(185, 0, 250);
    static final Color GCLAB_ALLOC_BORDER   = new Color(118, 0, 160);

    static final Color USED                 = new Color(220, 220, 220);

    static final Color LIVE_COMMITTED       = new Color(150, 150, 150);
    static final Color LIVE_REGULAR         = new Color(0, 200, 0);
    static final Color LIVE_HUMONGOUS       = new Color(250, 100, 0);
    static final Color LIVE_PINNED_HUMONGOUS = new Color(255, 0, 0);
    static final Color LIVE_CSET            = new Color(250, 250, 0);
    static final Color LIVE_TRASH           = new Color(100, 100, 100);
    static final Color LIVE_PINNED          = new Color(255, 0, 0);
    static final Color LIVE_PINNED_CSET     = new Color(255, 120, 0);
    static final Color LIVE_EMPTY           = new Color(255, 255, 255);

    static final Color LIVE_BORDER          = new Color(0, 100, 0);
    static final Color BORDER               = new Color(150, 150, 150);
}
