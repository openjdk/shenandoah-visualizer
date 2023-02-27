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
import java.util.Arrays;
import java.util.Collections;

import static org.openjdk.shenandoah.Colors.*;

public class RegionStat {

    private static final int PERCENT_MASK      = 0x7f;
    private static final int AGE_MASK          = 0x0f;
    private static final int AFFILIATION_MASK  = 0x03;
    private static final int STATUS_MASK       = 0x3f;

    private static final int USED_SHIFT        = 0;
    private static final int LIVE_SHIFT        = 7;
    private static final int TLAB_SHIFT        = 14;
    private static final int GCLAB_SHIFT       = 21;
    private static final int SHARED_SHIFT      = 28;
    private static final int PLAB_SHIFT        = 35;
    private static final int AGE_SHIFT         = 51;
    private static final int AFFILIATION_SHIFT = 56;
    private static final int FLAGS_SHIFT       = 58;

    private final RegionState state;
    private final float liveLvl;
    private final float usedLvl;
    private final float tlabLvl;
    private final float gclabLvl;
    private final float plabLvl;
    private final float sharedLvl;
    private final long age;
    private final RegionAffiliation affiliation;
    private final boolean showLivenessDetail;

    private static final Stroke STROKE = new BasicStroke(2);

    // This constructor is for the legend.
    public RegionStat(float usedLvl, float liveLvl, float tlabLvl, float gclabLvl, float plabLvl, float sharedLvl, RegionState state) {
        this.usedLvl = usedLvl;
        this.liveLvl = liveLvl;
        this.tlabLvl = tlabLvl;
        this.gclabLvl = gclabLvl;
        this.plabLvl = plabLvl;
        this.sharedLvl = sharedLvl;
        this.state = state;
        this.age = -1;
        this.affiliation = RegionAffiliation.YOUNG;
        this.showLivenessDetail = Boolean.getBoolean("show.liveness");
    }
    //This constructor is for CounterTest
    public RegionStat(float usedLvl, float liveLvl, float tlabLvl, float gclabLvl, float plabLvl, float sharedLvl, RegionAffiliation affiliation,RegionState state) {
        this.usedLvl = usedLvl;
        this.liveLvl = liveLvl;
        this.tlabLvl = tlabLvl;
        this.gclabLvl = gclabLvl;
        this.plabLvl = plabLvl;
        this.sharedLvl = sharedLvl;
        this.state = state;
        this.age = -1;
        this.affiliation = affiliation;
        this.showLivenessDetail = Boolean.getBoolean("show.liveness");
    }

    // Also only used for the legend.
    public RegionStat(RegionState state, int age) {
        this.usedLvl = 0;
        this.liveLvl = 0;
        this.tlabLvl = 0;
        this.gclabLvl = 0;
        this.plabLvl = 0;
        this.sharedLvl = 0;
        this.state = state;
        this.age = age;
        this.affiliation = RegionAffiliation.YOUNG;
        this.showLivenessDetail = Boolean.getBoolean("show.liveness");
    }

    public RegionStat(long data) {
        this.showLivenessDetail = Boolean.getBoolean("show.liveness");

        usedLvl  = ((data >>> USED_SHIFT)  & PERCENT_MASK) / 100F;
        liveLvl  = ((data >>> LIVE_SHIFT)  & PERCENT_MASK) / 100F;
        tlabLvl  = ((data >>> TLAB_SHIFT)  & PERCENT_MASK) / 100F;
        gclabLvl = ((data >>> GCLAB_SHIFT) & PERCENT_MASK) / 100F;
        plabLvl =  ((data >>> PLAB_SHIFT)  & PERCENT_MASK) / 100F;
        sharedLvl = ((data >>> SHARED_SHIFT) & PERCENT_MASK) / 100F;

        age = ((data >>> AGE_SHIFT) & AGE_MASK);
        affiliation = RegionAffiliation.fromOrdinal((int) (data >>> AFFILIATION_SHIFT) & AFFILIATION_MASK);
        state = RegionState.fromOrdinal((int) (data >>> FLAGS_SHIFT) & STATUS_MASK);
    }

    private Color selectLive(RegionState s) {
        switch (s) {
            case CSET:
                return LIVE_CSET;
            case HUMONGOUS:
                return LIVE_HUMONGOUS;
            case PINNED_HUMONGOUS:
                return LIVE_PINNED_HUMONGOUS;
            case REGULAR:
                return LIVE_REGULAR;
            case TRASH:
                return LIVE_TRASH;
            case PINNED:
                return LIVE_PINNED;
            case PINNED_CSET:
                return LIVE_PINNED_CSET;
            case EMPTY_COMMITTED:
            case EMPTY_UNCOMMITTED:
                return LIVE_EMPTY;
            default:
                return Color.WHITE;
        }
    }

    private Color mixAlpha(Color c, float alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha * 100 + 55));
    }

    private void drawShape(Graphics2D g, int x, int y, int width, int height) {
        switch (affiliation) {
            case FREE:
                break;
            case YOUNG:
                g.drawRect(x, y, width, height);
                break;
            case OLD:
                g.drawOval(x, y, width, height);
                break;
        }
    }

    private void fillShape(Graphics2D g, int x, int y, int width, int height) {
        switch (affiliation) {
            case FREE:
                break;
            case YOUNG:
                g.fillRect(x, y, width, height);
                break;
            case OLD:
                g.fillOval(x, y, width, height);
                break;
        }
    }

    public void render(Graphics graphics, int x, int y, int width, int height) {
        Graphics2D g = (Graphics2D) graphics;
        g.setColor(Color.WHITE);
        fillShape(g, x, y, width, height);

        switch (state) {
            case REGULAR: {
                if (gclabLvl > 0 || tlabLvl > 0 || sharedLvl > 0 || plabLvl > 0) {
                    int sharedWidth = (int) (width * sharedLvl);
                    int tlabWidth = (int) (width * tlabLvl);
                    int gclabWidth = (int) (width * gclabLvl);
                    int plabWidth = (int) (width * plabLvl);

                    int lx = x;

                    if (tlabWidth > 0) {
                        g.setColor(mixAlpha(TLAB_ALLOC, liveLvl));
                        fillShape(g, lx, y, tlabWidth, height);
//                        g.setColor(TLAB_ALLOC_BORDER);
//                        drawShape(g, lx, y, tlabWidth, height);
                        lx += tlabWidth;
                    }

                    if (gclabWidth > 0) {
                        g.setColor(mixAlpha(GCLAB_ALLOC, liveLvl));
                        fillShape(g, lx, y, gclabWidth, height);
//                        g.setColor(GCLAB_ALLOC_BORDER);
//                        drawShape(g, lx, y, gclabWidth, height);
                        lx += gclabWidth;
                    }

                    if (sharedWidth > 0) {
                        g.setColor(mixAlpha(SHARED_ALLOC, liveLvl));
                        fillShape(g, lx, y, sharedWidth, height);
//                        g.setColor(SHARED_ALLOC_BORDER);
//                        drawShape(g, lx, y, sharedWidth, height);
                        lx += sharedWidth;
                    }

                    if (plabWidth > 0) {
                        g.setColor(mixAlpha(PLAB_ALLOC, liveLvl));
                        fillShape(g, lx, y, plabWidth, height);
//                        g.setColor(PLAB_ALLOC_BORDER);
//                        drawShape(g, lx, y, plabWidth, height);
                    }
                }
                break;
            }
            case PINNED: {
                int usedWidth = (int) (width * usedLvl);
                g.setColor(Colors.LIVE_PINNED);
                fillShape(g, x, y, usedWidth, height);
                break;
            }
            case CSET:
            case PINNED_CSET:
            case HUMONGOUS:
            case PINNED_HUMONGOUS: {
                int usedWidth = (int) (width * usedLvl);
                g.setColor(USED);
                fillShape(g, x, y, usedWidth, height);

                int liveWidth = (int) (width * liveLvl);
                g.setColor(selectLive(state));
                fillShape(g, x, y, liveWidth, height);

                g.setColor(selectLive(state));
                g.drawLine(x + liveWidth, y, x + liveWidth, y + height);
                break;
            }
            case EMPTY_COMMITTED:
            case EMPTY_UNCOMMITTED:
            case TRASH:
                break;
            default:
                throw new IllegalStateException("Unhandled region state: " + state);
        }

        if (age < 15) {
            if (state == RegionState.TRASH) {
                g.setColor(Color.BLACK);
                g.drawLine(x, y, x + width, y + height);
                g.drawLine(x, y + height, x + width, y);
            }

            if (state == RegionState.EMPTY_UNCOMMITTED) {
                g.setColor(Colors.BORDER);
                for (int t = 0; t < 3; t++) {
                    int off = width * t / 3;
                    g.drawLine(x, y + off, x + off, y);
                    g.drawLine(x + off, y + height, x + width, y + off);
                }
            }
            if (state == RegionState.EMPTY_COMMITTED) {
                g.setColor(Color.RED);
                g.drawLine(x, y, x + width, y + height);
            }
        }

        if (age > -1) {
            g.setColor(getColorForAge());
            g.setStroke(STROKE);
            drawShape(g, x, y, width, height);
            if (showLivenessDetail) {
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(liveLvl), x + 2, y + height - 2);
            }
        }
    }

    private Color getColorForAge() {
        final int THRESHOLD = 15;
        final int categorySize = THRESHOLD / AGE_COLORS.length;
        int category = (int) (age / categorySize);
        if (category < AGE_COLORS.length) {
            return AGE_COLORS[category];
        }
        return AGE_5;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        RegionStat that = (RegionStat) o;

        if (Float.compare(that.liveLvl, liveLvl) != 0) return false;
        if (Float.compare(that.usedLvl, usedLvl) != 0) return false;
        if (Float.compare(that.tlabLvl, tlabLvl) != 0) return false;
        if (Float.compare(that.gclabLvl, gclabLvl) != 0) return false;
        return state.equals(that.state);
    }

    @Override
    public int hashCode() {
        int result = state.hashCode();
        result = 31 * result + (liveLvl != +0.0f ? Float.floatToIntBits(liveLvl) : 0);
        result = 31 * result + (usedLvl != +0.0f ? Float.floatToIntBits(usedLvl) : 0);
        result = 31 * result + (tlabLvl != +0.0f ? Float.floatToIntBits(tlabLvl) : 0);
        result = 31 * result + (gclabLvl != +0.0f ? Float.floatToIntBits(gclabLvl) : 0);
        return result;
    }

    public RegionAffiliation affiliation() {
        return affiliation;
    }

    public float live() {
        return liveLvl;
    }

    public float used() {
        return usedLvl;
    }

    public float tlabAllocs() {
        return tlabLvl;
    }

    public float gclabAllocs() {
        return gclabLvl;
    }

    public float plabAllocs() {
        return plabLvl;
    }

    public float maxAllocsYoung() {
        return Collections.max(Arrays.asList(tlabLvl, gclabLvl, sharedLvl));
    }
    public float maxAllocsOld() {
        return Collections.max(Arrays.asList(plabLvl, sharedLvl));
    }

    public float sharedAllocs() {
        return sharedLvl;
    }

    public RegionState state() {
        return state;
    }
    public long age() { return age; }
}
