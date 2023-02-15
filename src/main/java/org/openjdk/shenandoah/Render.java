package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

import static org.openjdk.shenandoah.RegionState.*;

public abstract class Render {
    public static final int LINE = 15;

    final JFrame frame;

    List<RegionPopUp> popups = new ArrayList<RegionPopUp>();

    int regionWidth, regionHeight;
    int graphWidth, graphHeight;
    final int STEP_X = 4;
    final int phaseLabelWidth = 50;

    final LinkedList<SnapshotView> lastSnapshots;
    final LinkedList<Snapshot> popupSnapshots;
    volatile Snapshot snapshot;

    public Render(JFrame frame) {
        this.frame = frame;
        this.lastSnapshots = new LinkedList<>();
        this.popupSnapshots = new LinkedList<>();
    }

    protected static Color getColor(SnapshotView s) {
        if (s.youngPhase() != Phase.IDLE) {
            return Colors.YOUNG[s.youngPhase().ordinal()];
        }
        if (s.globalPhase() != Phase.IDLE) {
            return Colors.GLOBAL[s.globalPhase().ordinal()];
        }
        if (s.oldPhase() != Phase.IDLE) {
            return Colors.OLD[s.oldPhase().ordinal()];
        }
        return Colors.TIMELINE_IDLE;
    }

    protected static Phase getPhase(SnapshotView s) {
        if (s.youngPhase() != Phase.IDLE) {
            return s.youngPhase();
        }
        if (s.globalPhase() != Phase.IDLE) {
            return s.globalPhase();
        }
        if (s.oldPhase() != Phase.IDLE) {
            return s.oldPhase();
        }
        return Phase.UNKNOWN;
    }

    public abstract void renderGraph(Graphics g);

    public synchronized void renderLegend(Graphics g) {
        final int sqSize = LINE;

        Map<String, RegionStat> items = new LinkedHashMap<>();

        items.put("Empty Uncommitted",
                new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_UNCOMMITTED));

        items.put("Empty Committed",
                new RegionStat(0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, EMPTY_COMMITTED));

        items.put("Trash",
                new RegionStat(1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, TRASH));

        items.put("TLAB Allocs",
                new RegionStat(1.0f, 1.0f, 1.0f, 0.0f, 0.0f, 0.0f, REGULAR));

        items.put("GCLAB Allocs",
                new RegionStat(1.0f, 1.0f, 0.0f, 1.0f, 0.0f, 0.0f, REGULAR));

        items.put("PLAB Allocs",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 1.0f, 0.0f, REGULAR));

        items.put("Shared Allocs",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 1.0f, REGULAR));

        items.put("Humongous",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, HUMONGOUS));

        items.put("Humongous + Pinned",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, PINNED_HUMONGOUS));

        items.put("Collection Set",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, CSET));

        items.put("Pinned",
                new RegionStat(1.0f, 1.0f, 0.3f, 0.0f, 0.0f, 0.0f, PINNED));

        items.put("Pinned CSet",
                new RegionStat(1.0f, 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, PINNED_CSET));

        items.put("Age [0, 3)", new RegionStat(REGULAR, 0));
        items.put("Age [3, 6)", new RegionStat(REGULAR, 3));
        items.put("Age [6, 9)", new RegionStat(REGULAR, 6));
        items.put("Age [9, 12)", new RegionStat(REGULAR, 9));
        items.put("Age [12, 15)", new RegionStat(REGULAR, 12));
        items.put("Age 15", new RegionStat(REGULAR, 15));

        Map<String, Integer> summaryNumbers = new LinkedHashMap<>();


        summaryNumbers.put("Empty Uncommitted", snapshot.getEmptyUncommittedCount());

        summaryNumbers.put("Empty Committed", snapshot.getEmptyCommittedCount());

        summaryNumbers.put("Trash", snapshot.getTrashCount());

        summaryNumbers.put("TLAB Allocs", snapshot.getTlabCount());

        summaryNumbers.put("GCLAB Allocs", snapshot.getGclabCount());

        summaryNumbers.put("PLAB Allocs", snapshot.getPlabCount());

        summaryNumbers.put("Shared Allocs", snapshot.getSharedCount());

        summaryNumbers.put("Humongous", snapshot.getHumongousCount());

        summaryNumbers.put("Humongous + Pinned", snapshot.getPinnedHumongousCount());

        summaryNumbers.put("Collection Set", snapshot.getCSetCount());

        summaryNumbers.put("Pinned", snapshot.getPinnedCount());


        summaryNumbers.put("Pinned CSet", snapshot.getPinnedCSetCount());

        summaryNumbers.put("Age [0, 3)", snapshot.getAge0Count());
        summaryNumbers.put("Age [3, 6)", snapshot.getAge3Count());
        summaryNumbers.put("Age [6, 9)", snapshot.getAge6Count());
        summaryNumbers.put("Age [9, 12)", snapshot.getAge9Count());
        summaryNumbers.put("Age [12, 15)", snapshot.getAge12Count());
        summaryNumbers.put("Age 15", snapshot.getAge15Count());
        int i = 0;
        for (String key : items.keySet()) {
            int y = (int) (i * sqSize * 1.5);
            items.get(key).render(g, 0, y, sqSize, sqSize);
            g.setColor(Color.BLACK);
            g.drawString(key + " total: " + summaryNumbers.get(key).toString(), (int) (sqSize * 1.5), y + sqSize);
            i++;
        }
    }

    public synchronized void renderRegions(Graphics g) {
        int area = regionWidth * regionHeight;
        int sqSize = Math.max(1, (int) Math.sqrt(1D * area / snapshot.regionCount()));
        int cols = regionWidth / sqSize;
        int cellSize = sqSize - 2;
        for (int i = 0; i < snapshot.regionCount(); i++) {
            int rectx = (i % cols) * sqSize;
            int recty = (i / cols) * sqSize;
            RegionStat s = snapshot.get(i);
            s.render(g, rectx, recty, cellSize, cellSize);
        }
    }

    protected String liveStatusLine() {
        return "Live (Green): MB: T:" +
                snapshot.live() / ShenandoahVisualizer.KILO + " Y:" +
                snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::live) / ShenandoahVisualizer.KILO + " O:" +
                snapshot.generationStat(RegionAffiliation.OLD, RegionStat::live) / ShenandoahVisualizer.KILO;
    }

    protected String usageStatusLine() {
        return "Used (White): MB: T:" +
                snapshot.used() / ShenandoahVisualizer.KILO + " Y:" +
                snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::used) / ShenandoahVisualizer.KILO + " O:" +
                snapshot.generationStat(RegionAffiliation.OLD, RegionStat::used) / ShenandoahVisualizer.KILO;
    }

    protected void renderTimeLineLegendItem(Graphics g, int sqSize, Color color, int lineNumber, String label) {
        g.setColor(color);
        int y = (int) (lineNumber * LINE * 1.5);
        g.fillRect(0, y, sqSize, sqSize);
        g.setColor(Color.BLACK);
        g.drawString(label, (int) (sqSize * 1.5), y + sqSize);
    }

    public synchronized void notifyRegionResized(int width, int height) {
        this.regionWidth = width;
        this.regionHeight = height;
    }

    public synchronized void notifyGraphResized(int width, int height) {
        this.graphWidth = width;
        this.graphHeight = height;
    }

    public void addPopup(RegionPopUp popup) {
        popups.add(popup);
    }

    public void deletePopup(RegionPopUp popup) {
        popups.remove(popup);
    }

    public void repaintPopups() {
        if (popups != null) {
            for (RegionPopUp popup : popups) {
                popup.setSnapshots(popupSnapshots);
                popup.repaint();
            }
        }
    }
}
