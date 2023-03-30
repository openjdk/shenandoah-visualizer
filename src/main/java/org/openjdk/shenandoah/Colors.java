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

import java.awt.*;

class Colors {

    static final Color[] YOUNG = createColorFamily(Color.GREEN);
    static final Color[] OLD = createColorFamily(new Color(120 , 146, 210));
    static final Color[] GLOBAL = createColorFamily(Color.RED);

    private static Color[] createColorFamily(Color base) {
        Color[] colors = new Color[4];
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        colors[0] = base;
        colors[1] = Color.getHSBColor(hsb[0], 0.5f, 0.5f);
        colors[2] = Color.getHSBColor(hsb[0], 0.4f, 0.6f);
        colors[3] = Color.getHSBColor(hsb[0], 0.3f, 0.7f);
        return colors;
    }

    static final Color TIMELINE_IDLE        = Color.BLACK;

    static final Color DEGENERATE = Color.ORANGE;
    static final Color FULL       = Color.RED;

    static final Color SHARED_ALLOC         = new Color(0, 150, 250);
    static final Color TLAB_ALLOC           = new Color(0, 200, 0);
    static final Color GCLAB_ALLOC          = new Color(185, 0, 250);
    static final Color PLAB_ALLOC           = new Color(111, 78, 55);

    static final Color USED                 = new Color(220, 220, 220);
    static final Color LIVE_REGULAR         = new Color(0, 200, 0);
    static final Color LIVE_HUMONGOUS       = new Color(250, 100, 0);
    static final Color LIVE_PINNED_HUMONGOUS = new Color(255, 0, 0);
    static final Color LIVE_CSET            = new Color(250, 250, 0);
    static final Color LIVE_TRASH           = new Color(100, 100, 100);
    static final Color LIVE_PINNED          = new Color(252, 201, 156);
    static final Color LIVE_PINNED_CSET     = new Color(59, 88, 124);
    static final Color LIVE_EMPTY           = new Color(255, 255, 255);
    static final Color BORDER               = new Color(150, 150, 150);

    static final Color[] AGE_COLORS = {Color.LIGHT_GRAY, Color.YELLOW, Color.ORANGE, Color.RED, Color.BLUE};
}
