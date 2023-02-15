package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;

import static org.openjdk.shenandoah.LayoutConstants.LINE;
import static org.openjdk.shenandoah.LayoutConstants.renderTimeLineLegendItem;

public class StatusPanel extends JPanel {
    private final SnapshotHistory history;

    public StatusPanel(SnapshotHistory history) {
        this.history = history;
    }

    @Override
    public void paint(Graphics g) {
        Snapshot snapshot = history.latest();
        String mode = snapshot.collectionMode();
        String status = getStatus(snapshot);

        int line = 0;

        g.setColor(Color.BLACK);
        g.drawString("GC: " + status, 0, ++line * LINE);
        g.drawString("Mode: " + mode, 0, ++line * LINE);
        g.drawString("Total: " + (snapshot.total() / ShenandoahVisualizer.KILO) + " MB", 0, ++line * LINE);
        g.drawString(usageStatusLine(snapshot), 0, ++line * LINE);
        g.drawString(liveStatusLine(snapshot), 0, ++line * LINE);

        renderTimeLineLegendItem(g, LINE, Colors.OLD[1], ++line, "Old Marking (OM)");
        renderTimeLineLegendItem(g, LINE, Colors.YOUNG[1], ++line, "Young Marking (M)");
        renderTimeLineLegendItem(g, LINE, Colors.YOUNG[2], ++line, "Young Evacuation (E)");
        renderTimeLineLegendItem(g, LINE, Colors.YOUNG[3], ++line, "Young Update References (UR)");

        renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[1], ++line, "Global Marking (M)");
        renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[2], ++line, "Global Evacuation (E)");
        renderTimeLineLegendItem(g, LINE, Colors.GLOBAL[3], ++line, "Global Update References (UR)");

        renderTimeLineLegendItem(g, LINE, Colors.DEGENERATE, ++line, "Degenerated Cycle");
        renderTimeLineLegendItem(g, LINE, Colors.FULL, ++line, "Full");
    }

    private static String getStatus(Snapshot snapshot) {
        switch (snapshot.phase()) {
            case IDLE:          return " Idle";
            case EVACUATING:    return " Evacuating";
            case UPDATE_REFS:   return " Updating Refs";
            case UNKNOWN:       return " Unknown";
            case MARKING:
                String status = " Marking";
                if (snapshot.getOldPhase() == Phase.MARKING) {
                    status += " (Old)";
                }
                return status;
            default:
                throw new IllegalArgumentException("Unknown phase: " + snapshot.phase());
        }
    }

    protected String liveStatusLine(Snapshot snapshot) {
        return "Live (Green): MB: T:" +
                snapshot.live() / ShenandoahVisualizer.KILO + " Y:" +
                snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::live) / ShenandoahVisualizer.KILO + " O:" +
                snapshot.generationStat(RegionAffiliation.OLD, RegionStat::live) / ShenandoahVisualizer.KILO;
    }

    protected String usageStatusLine(Snapshot snapshot) {
        return "Used (White): MB: T:" +
                snapshot.used() / ShenandoahVisualizer.KILO + " Y:" +
                snapshot.generationStat(RegionAffiliation.YOUNG, RegionStat::used) / ShenandoahVisualizer.KILO + " O:" +
                snapshot.generationStat(RegionAffiliation.OLD, RegionStat::used) / ShenandoahVisualizer.KILO;
    }
}
