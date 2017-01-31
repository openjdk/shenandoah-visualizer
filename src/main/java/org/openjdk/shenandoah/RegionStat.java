package org.openjdk.shenandoah;

import java.awt.*;

public class RegionStat {

    private static final int USED_MASK = 0x1fffffff;
    private static final int USED_SHIFT = 0;
    private static final int LIVE_MASK = 0x1fffffff;
    private static final int LIVE_SHIFT = 29;
    private static final int FLAGS_MASK = 0x3f;
    private static final int FLAGS_SHIFT = 58;

    private final boolean unused;
    private final boolean humongous;
    private final boolean inCset;
    private final boolean newlyAllocated;
    private final boolean pinned;
    private final double liveLvl;
    private final double usedLvl;
    private long ma;

    public RegionStat(double usedLvl, double liveLvl, boolean unused, boolean humongous, boolean inCset, boolean newlyAllocated) {
        this.usedLvl = usedLvl;
        this.liveLvl = liveLvl;
        this.unused = unused;
        this.humongous = humongous;
        this.inCset = inCset;
        this.newlyAllocated = newlyAllocated;
        this.pinned = false;
    }

    public RegionStat(long maxSize, long data) {
        long used = (data >>> USED_SHIFT) & USED_MASK;
        usedLvl = Math.min(1D, 1D * used / maxSize);

        long live = (data >>> LIVE_SHIFT) & LIVE_MASK;
        liveLvl = Math.min(1D, 1D * live / maxSize);

        long stat = (data >>> FLAGS_SHIFT) & FLAGS_MASK;
        unused = (stat & 1) > 0;
        inCset = (stat & 2) > 0;
        humongous = (stat & 4) > 0;
        newlyAllocated = (stat & 8) > 0;
        pinned = (stat & 16) > 0;
    }

    public void render(Graphics g, int x, int y, int width, int height) {
        g.setColor(Color.WHITE);
        g.fillRect(x, y, width, height);

        int usedWidth = (int) (width * usedLvl);
        g.setColor(
                newlyAllocated ?
                new Color(0, 250, 250) :
                new Color(150, 150, 150)
        );
        g.fillRect(x, y, usedWidth, height);

        if (!newlyAllocated) {
            int liveWidth = (int) (width * liveLvl);
            g.setColor(new Color(0, 200, 0));
            g.fillRect(x, y, liveWidth, height);

            g.setColor(new Color(0, 100, 0));
            g.drawLine(x + liveWidth, y, x + liveWidth, y + height);
        }

        if (inCset) {
            g.setColor(Color.YELLOW);
            g.fillRect(x, y, width, height / 3);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height / 3);
        }

        if (humongous) {
            g.setColor(Color.RED);
            g.fillRect(x, y, width, height / 3);
            g.setColor(Color.BLACK);
            g.drawRect(x, y, width, height / 3);
        }

        if (unused) {
            g.setColor(Color.BLACK);
            g.drawLine(x, y, x + width, y + height);
            g.drawLine(x, y + height, x + width, y);
        }

        if (pinned) {
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

        if (unused != that.unused) return false;
        if (humongous != that.humongous) return false;
        if (inCset != that.inCset) return false;
        if (Double.compare(that.liveLvl, liveLvl) != 0) return false;
        return Double.compare(that.usedLvl, usedLvl) == 0;
    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        result = (unused ? 1 : 0);
        result = 31 * result + (humongous ? 1 : 0);
        result = 31 * result + (inCset ? 1 : 0);
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
}
