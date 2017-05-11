package org.openjdk.shenandoah;

import java.awt.*;
import java.util.BitSet;
import java.util.EnumSet;

import static org.openjdk.shenandoah.Colors.*;

public class RegionStat {

    private static final int PERCENT_MASK = 0x7f;
    private static final int USED_SHIFT   = 0;
    private static final int LIVE_SHIFT   = 7;
    private static final int TLAB_SHIFT   = 14;
    private static final int GCLAB_SHIFT  = 21;
    private static final int FLAGS_MASK = 0x3f;
    private static final int FLAGS_SHIFT = 58;

    private final EnumSet<RegionFlag> flags;
    private final BitSet incoming;
    private final double liveLvl;
    private final double usedLvl;
    private final double tlabLvl;
    private final double gclabLvl;

    public RegionStat(double usedLvl, double liveLvl, double tlabLvl, double gclabLvl, EnumSet<RegionFlag> flags) {
        this.incoming = new BitSet();
        this.usedLvl = usedLvl;
        this.liveLvl = liveLvl;
        this.tlabLvl = tlabLvl;
        this.gclabLvl = gclabLvl;
        this.flags = flags;
    }

    public RegionStat(long maxSize, long data, String matrix) {
        usedLvl  = ((data >>> USED_SHIFT)  & PERCENT_MASK) / 100D;
        liveLvl  = ((data >>> LIVE_SHIFT)  & PERCENT_MASK) / 100D;
        tlabLvl  = ((data >>> TLAB_SHIFT)  & PERCENT_MASK) / 100D;
        gclabLvl = ((data >>> GCLAB_SHIFT) & PERCENT_MASK) / 100D;

        long stat = (data >>> FLAGS_SHIFT) & FLAGS_MASK;

        flags = EnumSet.noneOf(RegionFlag.class);

        if ((stat & 1) > 0) flags.add(RegionFlag.UNUSED);
        if ((stat & 2) > 0) flags.add(RegionFlag.IN_COLLECTION_SET);
        if ((stat & 4) > 0) flags.add(RegionFlag.HUMONGOUS);
        if ((stat & 8) > 0) flags.add(RegionFlag.PINNED);

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

        if (Double.compare(that.liveLvl, liveLvl) != 0) return false;
        if (Double.compare(that.usedLvl, usedLvl) != 0) return false;
        if (!flags.equals(that.flags)) return false;
        return incoming.equals(that.incoming);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = flags.hashCode();
        result = 31 * result + incoming.hashCode();
        temp = Double.doubleToLongBits(liveLvl);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(usedLvl);
        result = 31 * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    public double live() {
        return liveLvl;
    }

    public double used() {
        return usedLvl;
    }

    public double tlabAllocs() {
        return tlabLvl;
    }

    public double gclabAllocs() {
        return gclabLvl;
    }

    public EnumSet<RegionFlag> flags() {
        return flags;
    }

    public BitSet incoming() {
        return incoming;
    }

}
