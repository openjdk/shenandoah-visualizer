package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.List;

public class GraphPanel extends JPanel {
    private final RenderRunner renderRunner;

    int graphWidth, graphHeight;

    public GraphPanel(RenderRunner renderRunner) {
        this.renderRunner = renderRunner;

        this.addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                graphWidth = ev.getComponent().getWidth();
                graphHeight = ev.getComponent().getHeight();
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        List<Snapshot> snapshots = renderRunner.snapshots();

        int pad = 30;
        int bandHeight = (graphHeight - pad) / 2;
        int bandWidth = graphWidth - LayoutConstants.PHASE_LABEL_WIDTH;
        int phaseHeight = bandHeight / 4;
        double stepY = 1D * bandHeight / renderRunner.snapshot().total();

        int startRaw = graphHeight - bandHeight - pad;

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, bandWidth, graphHeight);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, bandWidth, bandHeight);
        g.fillRect(0, bandHeight + pad, bandWidth, bandHeight);

        int snapshotWidth = 1;
        int snapshotStartX = bandWidth;
        int oneFourth = bandWidth / 4;
        int oneHalf = oneFourth * 2;
        int threeFourths = oneFourth * 3;

        for (int i = snapshots.size() - 1; i >= 0 && snapshotStartX >=0; --i) {
            Snapshot snapshot = snapshots.get(i);
            snapshotStartX -= snapshotWidth;

            if (snapshot.getOldPhase() == Phase.MARKING && snapshot.getGlobalPhase() == Phase.IDLE) {
                g.setColor(Colors.OLD[0]);
                g.drawRect(snapshotStartX, bandHeight + pad, snapshotWidth, phaseHeight);
            }

            if (snapshot.percentageOfOldRegionsInCollectionSet() > 0) {
                int height = (int) (bandHeight * snapshot.percentageOfOldRegionsInCollectionSet());
                g.setColor(Colors.OLD[0]);
                g.drawRect(snapshotStartX, 2 * bandHeight + pad - height, snapshotWidth, height);
            }

            g.setColor(getColor(snapshot));
            if (getPhase(snapshot) == Phase.MARKING) {
                g.drawRect(snapshotStartX, bandHeight + pad + phaseHeight, snapshotWidth, phaseHeight);
            }
            if (getPhase(snapshot) == Phase.EVACUATING) {
                g.drawRect(snapshotStartX, bandHeight + pad + 2 * phaseHeight, snapshotWidth, phaseHeight);
            }
            if (getPhase(snapshot) == Phase.UPDATE_REFS) {
                g.drawRect(snapshotStartX, bandHeight + pad + 3 * phaseHeight, snapshotWidth, phaseHeight);
            }

            if (snapshot.isFullActive()) {
                g.setColor(Colors.FULL);
                g.drawRect(snapshotStartX, bandHeight + pad, snapshotWidth, 10);
            } else if (snapshot.isDegenActive()) {
                g.setColor(Colors.DEGENERATE);
                g.drawRect(snapshotStartX, bandHeight + pad, snapshotWidth, 10);
            }

            // Draw these in the upper band.
            g.setColor(Colors.USED);
            g.drawRect(snapshotStartX, (int) Math.round(startRaw - snapshot.used() * stepY), 1, 1);
            g.setColor(Colors.LIVE_REGULAR);
            g.drawRect(snapshotStartX, (int) Math.round(startRaw - snapshot.live() * stepY), 1, 1);
            g.setColor(Colors.LIVE_CSET);
            g.drawRect(snapshotStartX, (int) Math.round(startRaw - snapshot.collectionSet() * stepY), 1, 1);

            g.setColor(Color.GRAY);
            g.drawString("OM", bandWidth + 10, bandHeight + pad + 20);
            g.drawString("M", bandWidth + 10, bandHeight + phaseHeight + pad + 20);
            g.drawString("E", bandWidth + 10, bandHeight + 2 * phaseHeight + pad + 20);
            g.drawString("UR", bandWidth + 10, bandHeight + 3 * phaseHeight + pad + 20);


            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(0, bandHeight + 5, 0, bandHeight + pad - 5);
            g2.drawLine(bandWidth / 4, bandHeight + 5, bandWidth / 4, bandHeight + pad - 5);
            g2.drawLine(bandWidth / 2, bandHeight + 5, bandWidth / 2, bandHeight + pad - 5);
            g2.drawLine(bandWidth * 3 / 4, bandHeight + 5, bandWidth * 3 / 4, bandHeight + pad - 5);

            if (oneFourth == snapshotStartX) {
                g.drawString(snapshot.time() + " ms", oneFourth + 3, bandHeight + 20);
            } else if (oneHalf == snapshotStartX) {
                g.drawString(snapshot.time() + " ms", oneHalf + 3, bandHeight + 20);
            } else if (threeFourths == snapshotStartX) {
                g.drawString(snapshot.time() + " ms", threeFourths + 3, bandHeight + 20);
            }
        }
    }

    protected static Color getColor(Snapshot s) {
        if (s.getYoungPhase() != Phase.IDLE) {
            return Colors.YOUNG[s.getYoungPhase().ordinal()];
        }
        if (s.getGlobalPhase() != Phase.IDLE) {
            return Colors.GLOBAL[s.getGlobalPhase().ordinal()];
        }
        if (s.getOldPhase() != Phase.IDLE) {
            return Colors.OLD[s.getOldPhase().ordinal()];
        }
        return Colors.TIMELINE_IDLE;
    }

    protected static Phase getPhase(Snapshot s) {
        if (s.getYoungPhase() != Phase.IDLE) {
            return s.getYoungPhase();
        }
        if (s.getGlobalPhase() != Phase.IDLE) {
            return s.getGlobalPhase();
        }
        if (s.getOldPhase() != Phase.IDLE) {
            return s.getOldPhase();
        }
        return Phase.UNKNOWN;
    }
}
