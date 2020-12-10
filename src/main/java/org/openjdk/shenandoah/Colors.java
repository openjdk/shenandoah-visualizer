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

    static final Color GLOBAL_TIMELINE_MARK = new Color(227, 196, 181);
    static final Color GLOBAL_TIMELINE_EVACUATING = new Color(203, 145, 115);
    static final Color GLOBAL_TIMELINE_UPDATEREFS = new Color(142, 68, 61);

    static final Color YOUNG_TIMELINE_MARK = new Color(207, 221, 231);
    static final Color YOUNG_TIMELINE_EVACUATING = new Color(168, 194, 86);
    static final Color YOUNG_TIMELINE_UPDATEREFS = new Color(51, 115, 87);

    static final Color DEGENERATE_YOUNG  = Color.ORANGE;
    static final Color DEGENERATE_GLOBAL = Color.MAGENTA;
    static final Color FULL              = Color.RED;
    
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

    static final Color AGE_0 = Color.LIGHT_GRAY;
    static final Color AGE_1 = Color.YELLOW;
    static final Color AGE_2 = Color.ORANGE;
    static final Color AGE_3 = Color.RED;
    static final Color AGE_4 = Color.BLUE;
    static final Color AGE_5 = Color.BLACK;

    static final Color[] AGE_COLORS = {AGE_0, AGE_1, AGE_2, AGE_3, AGE_4};
}
