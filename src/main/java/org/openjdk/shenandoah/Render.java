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
