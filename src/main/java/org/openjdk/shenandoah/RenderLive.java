package org.openjdk.shenandoah;

import org.HdrHistogram.Histogram;

import javax.swing.*;
import java.awt.*;

public class RenderLive extends Render {
    volatile DataProvider data;
    int oneFourthIndex = 0;
    int oneHalfIndex = 0;
    int threeFourthIndex = 0;

    public RenderLive(JFrame frame) {
        super(frame);
        this.data = new DataProvider(null, null);
        this.snapshot = data.snapshot();
    }

    public RenderLive(DataProvider data, JFrame frame) {
        super(frame);
        this.data = data;
        this.snapshot = data.snapshot();
    }

    public synchronized void run() {
        Snapshot cur = data.snapshot();
        int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
        if (!cur.equals(snapshot)) {
            snapshot = cur;
            lastSnapshots.add(new SnapshotView(cur));
            popupSnapshots.add(cur);

            if (lastSnapshots.size() > endBandIndex) {
                lastSnapshots.removeFirst();
                oneFourthIndex = lastSnapshots.size() / 4;
                oneHalfIndex = lastSnapshots.size() / 2;
                threeFourthIndex = lastSnapshots.size() * 3 / 4;

            } else {
                if (lastSnapshots.size() == endBandIndex / 4) {
                    oneFourthIndex = lastSnapshots.size() - 1;
                }
                if (lastSnapshots.size() == endBandIndex / 2) {
                    oneHalfIndex = lastSnapshots.size() - 1;
                }
                if (lastSnapshots.size() == endBandIndex * 3 / 4) {
                    threeFourthIndex = lastSnapshots.size() - 1;
                }
            }
            frame.repaint();
            repaintPopups();
        }
    }

    public synchronized void loadDataProvider(DataProvider data) {
        closeDataProvider();
        this.data = data;
        this.lastSnapshots.clear();
        this.popupSnapshots.clear();
        this.snapshot = data.snapshot();
    }

    public synchronized void closeDataProvider() {
        if (this.data != null) {
            data.stopConnector();
        }
        this.data = null;
    }

    public synchronized void renderGraph(Graphics g) {
        if (lastSnapshots.size() < 2) return;

        int pad = 30;
        int bandHeight = (graphHeight - pad) / 2;
        int bandWidth = graphWidth - phaseLabelWidth;
        int phaseHeight = bandHeight / 4;
        double stepY = 1D * bandHeight / snapshot.total();

        int startRaw = graphHeight - bandHeight - pad;

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, bandWidth, graphHeight);

        g.setColor(Color.BLACK);
        g.fillRect(0, 0, bandWidth, bandHeight);
        g.fillRect(0, bandHeight + pad, bandWidth, bandHeight);

        long firstTime = lastSnapshots.getFirst().time();
        long lastTime = lastSnapshots.getLast().time();
        double stepX = (double) STEP_X * Math.min(lastSnapshots.size(), bandWidth) / (lastTime - firstTime);

        for (int i = 0; i < lastSnapshots.size(); i++) {
            SnapshotView s = lastSnapshots.get(i);
            int x = (int) Math.round((s.time() - firstTime) * stepX);

            if (s.globalPhase() == Phase.IDLE) {
                if (s.oldPhase() == Phase.MARKING) {
                    g.setColor(Colors.OLD[0]);
                    g.drawRect(x, bandHeight + pad, 1, phaseHeight);
                } else if (s.oldPhase() == Phase.EVACUATING) {
                    g.setColor(Colors.OLD[1]);
                    g.drawRect(x, bandHeight + pad + 2 * phaseHeight, 1, phaseHeight);
                }
            }

            if (s.oldCsetPercentage() > 0) {
                int height = (int) (bandHeight * s.oldCsetPercentage());
                g.setColor(Colors.OLD[0]);
                g.drawRect(x, 2 * bandHeight + pad - height, 1, height);
            }

            g.setColor(getColor(s));
            if (getPhase(s) == Phase.MARKING) {
                g.drawRect(x, bandHeight + pad + phaseHeight, 1, phaseHeight);
            }
            if (getPhase(s) == Phase.EVACUATING) {
                g.drawRect(x, bandHeight + pad + 2 * phaseHeight, 1, phaseHeight);
            }
            if (getPhase(s) == Phase.UPDATE_REFS) {
                g.drawRect(x, bandHeight + pad + 3 * phaseHeight, 1, phaseHeight);
            }


            if (s.isFullActive()) {
                g.setColor(Colors.FULL);
                g.drawRect(x, bandHeight + pad, 1, 10);
            } else if (s.isDegenActive()) {
                g.setColor(Colors.DEGENERATE);
                g.drawRect(x, bandHeight + pad, 1, 10);
            }

            // Draw these in the upper band.
            g.setColor(Colors.USED);
            g.drawRect(x, (int) Math.round(startRaw - s.used() * stepY), 1, 1);
            g.setColor(Colors.LIVE_REGULAR);
            g.drawRect(x, (int) Math.round(startRaw - s.live() * stepY), 1, 1);
            g.setColor(Colors.LIVE_CSET);
            g.drawRect(x, (int) Math.round(startRaw - s.collectionSet() * stepY), 1, 1);

            g.setColor(Color.GRAY);
            g.drawString("OM", bandWidth + 10, bandHeight + pad + 20);
            g.drawString("M", bandWidth + 10, bandHeight + phaseHeight + pad + 20);
            g.drawString("E", bandWidth + 10, bandHeight + 2 * phaseHeight + pad + 20);
            g.drawString("UR", bandWidth + 10, bandHeight + 3 * phaseHeight + pad + 20);

            renderTimestampLabel(g);
        }
    }

    public synchronized void renderTimestampLabel(Graphics g) {
        int pad = 30;
        int bandHeight = (graphHeight - pad) / 2;
        int bandWidth = graphWidth - phaseLabelWidth;
        int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;

        Graphics2D g2 = (Graphics2D) g;
        g2.setStroke(new BasicStroke(2));
        g2.drawLine(0, bandHeight + 5, 0, bandHeight + pad - 5);
        g2.drawLine(bandWidth / 4, bandHeight + 5, bandWidth / 4, bandHeight + pad - 5);
        g2.drawLine(bandWidth / 2, bandHeight + 5, bandWidth / 2, bandHeight + pad - 5);
        g2.drawLine(bandWidth * 3 / 4, bandHeight + 5, bandWidth * 3 / 4, bandHeight + pad - 5);

        g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1).time() - lastSnapshots.get(0).time()) + " ms", 3, bandHeight + 20);

        if (lastSnapshots.size() > endBandIndex / 4) {
            g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1).time() - lastSnapshots.get(oneFourthIndex).time()) + " ms", bandWidth / 4 + 3, bandHeight + 20);
        }
        if (lastSnapshots.size() > endBandIndex / 2) {
            g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1).time() - lastSnapshots.get(oneHalfIndex).time()) + " ms", bandWidth / 2 + 3, bandHeight + 20);
        }
        if (lastSnapshots.size() > endBandIndex * 3 / 4) {
            g.drawString("-" + Long.toString(lastSnapshots.get(lastSnapshots.size() - 1).time() - lastSnapshots.get(threeFourthIndex).time()) + " ms", bandWidth * 3 / 4 + 3, bandHeight + 20);
        }
    }
}
