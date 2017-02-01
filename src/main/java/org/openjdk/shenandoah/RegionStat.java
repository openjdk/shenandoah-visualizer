package org.openjdk.shenandoah;

import java.awt.*;
import java.util.EnumSet;

import static org.openjdk.shenandoah.Colors.*;

public class RegionStat {

    private static final int USED_MASK = 0x1fffffff;
    private static final int USED_SHIFT = 0;
    private static final int LIVE_MASK = 0x1fffffff;
    private static final int LIVE_SHIFT = 29;
    private static final int FLAGS_MASK = 0x3f;
    private static final int FLAGS_SHIFT = 58;

    private final EnumSet<RegionFlag> flags;
    private final double liveLvl;
    private final double usedLvl;

    public RegionStat(double usedLvl, double liveLvl, EnumSet<RegionFlag> flags) {
        this.usedLvl = usedLvl;
        this.liveLvl = liveLvl;
        this.flags = flags;
    }

    public RegionStat(long maxSize, long data) {
        long used = (data >>> USED_SHIFT) & USED_MASK;
        usedLvl = Math.min(1D, 1D * used / maxSize);

        long live = (data >>> LIVE_SHIFT) & LIVE_MASK;
        liveLvl = Math.min(1D, 1D * live / maxSize);

        long stat = (data >>> FLAGS_SHIFT) & FLAGS_MASK;

        flags = EnumSet.noneOf(RegionFlag.class);

        if ((stat & 1)  > 0) flags.add(RegionFlag.UNUSED);
        if ((stat & 2)  > 0) flags.add(RegionFlag.IN_COLLECTION_SET);
        if ((stat & 4)  > 0) flags.add(RegionFlag.HUMONGOUS);
        if ((stat & 8)  > 0) flags.add(RegionFlag.RECENTLY_ALLOCATED);
        if ((stat & 16) > 0) flags.add(RegionFlag.PINNED);
    }

    public void render(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, width, height);

        int usedWidth = (int) (width * usedLvl);
        g.setColor(
                flags.contains(RegionFlag.RECENTLY_ALLOCATED) ?
                        USED_ALLOC : USED
        );
        g.fillRect(x, y, usedWidth, height);

        if (!flags.contains(RegionFlag.RECENTLY_ALLOCATED)) {
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
        return flags.equals(that.flags);
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = flags.hashCode();
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

    public EnumSet<RegionFlag> flags() {
        return flags;
    }

}
