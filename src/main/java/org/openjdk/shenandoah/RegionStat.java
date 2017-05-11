package org.openjdk.shenandoah;

import java.awt.*;
import java.util.BitSet;
import java.util.EnumSet;

import static org.openjdk.shenandoah.Colors.*;

public class RegionStat {

    private static final int PERCENT_MASK = 0x7f;
    private static final int FLAGS_MASK   = 0x3f;

    private static final int USED_SHIFT   = 0;
    private static final int LIVE_SHIFT   = 7;
    private static final int TLAB_SHIFT   = 14;
    private static final int GCLAB_SHIFT  = 21;
    private static final int FLAGS_SHIFT  = 58;

    private final EnumSet<RegionFlag> flags;
    private final BitSet incoming;
    private final float liveLvl;
    private final float usedLvl;
    private final float tlabLvl;
    private final float gclabLvl;

    public RegionStat(float usedLvl, float liveLvl, float tlabLvl, float gclabLvl, EnumSet<RegionFlag> flags) {
        this.incoming = null;
        this.usedLvl = usedLvl;
        this.liveLvl = liveLvl;
        this.tlabLvl = tlabLvl;
        this.gclabLvl = gclabLvl;
        this.flags = flags;
    }

    public RegionStat(long data, String matrix) {
        usedLvl  = ((data >>> USED_SHIFT)  & PERCENT_MASK) / 100F;
        liveLvl  = ((data >>> LIVE_SHIFT)  & PERCENT_MASK) / 100F;
        tlabLvl  = ((data >>> TLAB_SHIFT)  & PERCENT_MASK) / 100F;
        gclabLvl = ((data >>> GCLAB_SHIFT) & PERCENT_MASK) / 100F;

        long stat = (data >>> FLAGS_SHIFT) & FLAGS_MASK;

        flags = EnumSet.noneOf(RegionFlag.class);

        if ((stat & 1) > 0) flags.add(RegionFlag.UNUSED);
        if ((stat & 2) > 0) flags.add(RegionFlag.IN_COLLECTION_SET);
        if ((stat & 4) > 0) flags.add(RegionFlag.HUMONGOUS);
        if ((stat & 8) > 0) flags.add(RegionFlag.PINNED);

        if (!matrix.isEmpty()) {
            this.incoming = new BitSet();
            int idx = 0;
            for (char c : matrix.toCharArray()) {
                c = (char) (c - 32);
                incoming.set(idx++, (c & (1 << 0)) > 0);
                incoming.set(idx++, (c & (1 << 1)) > 0);
                incoming.set(idx++, (c & (1 << 2)) > 0);
                incoming.set(idx++, (c & (1 << 3)) > 0);
                incoming.set(idx++, (c & (1 << 4)) > 0);
                incoming.set(idx++, (c & (1 << 5)) > 0);
            }
        } else {
            this.incoming = null;
        }
    }

    public void render(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, width, height);

        int usedWidth = (int) (width * usedLvl);
        g.setColor(USED);
        g.fillRect(x, y, usedWidth, height);

        if (gclabLvl > 0 || tlabLvl > 0) {
            int tlabWidth = (int) (width * tlabLvl);
            int gclabWidth = (int) (width * gclabLvl);
            g.setColor(TLAB_ALLOC);
            g.fillRect(x, y, tlabWidth, height);
            g.setColor(TLAB_ALLOC_BORDER);
            g.drawLine(x + tlabWidth, y, x + tlabWidth, y + height);

            int lx = x + tlabWidth;
            g.setColor(GCLAB_ALLOC);
            g.fillRect(lx, y, gclabWidth, height);
            g.setColor(GCLAB_ALLOC_BORDER);
            g.drawLine(lx + gclabWidth, y, lx + gclabWidth, y + height);
        } else {
            int liveWidth = (int) (width * liveLvl);
            g.setColor(LIVE);
            g.fillRect(x, y, liveWidth, height);

            g.setColor(LIVE_BORDER);
            g.drawLine(x + liveWidth, y, x + liveWidth, y + height);
        }

        if (flags.contains(RegionFlag.IN_COLLECTION_SET)) {
            g.setColor(Colors.CSET);
            g.fillRect(x, y, width, height / 3);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height / 3);
        }

        if (flags.contains(RegionFlag.HUMONGOUS)) {
            g.setColor(Colors.HUMONGOUS);
            g.fillRect(x, y, width, height / 3);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height / 3);
        }

        if (flags.contains(RegionFlag.UNUSED)) {
            g.setColor(Color.BLACK);
            g.drawLine(x, y, x + width, y + height);
            g.drawLine(x, y + height, x + width, y);
        }

        if (flags.contains(RegionFlag.PINNED)) {
            g.setColor(Color.RED);
            g.fillOval(x + width/2, y + height/2, width/4, height/4);
        }

        g.setColor(Color.BLACK);
        g.drawRect(x, y, width, height);
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
        if (!flags.equals(that.flags)) return false;
        return incoming != null ? incoming.equals(that.incoming) : that.incoming == null;
    }

    @Override
    public int hashCode() {
        int result = flags.hashCode();
        result = 31 * result + (incoming != null ? incoming.hashCode() : 0);
        result = 31 * result + (liveLvl != +0.0f ? Float.floatToIntBits(liveLvl) : 0);
        result = 31 * result + (usedLvl != +0.0f ? Float.floatToIntBits(usedLvl) : 0);
        result = 31 * result + (tlabLvl != +0.0f ? Float.floatToIntBits(tlabLvl) : 0);
        result = 31 * result + (gclabLvl != +0.0f ? Float.floatToIntBits(gclabLvl) : 0);
        return result;
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

    public EnumSet<RegionFlag> flags() {
        return flags;
    }

    public BitSet incoming() {
        return incoming;
    }

}
