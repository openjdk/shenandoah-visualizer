package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.TimeUnit;

public class RenderPlayback extends Render {
    volatile DataLogProvider data;
    volatile boolean isPaused;

    volatile double speed = 1.0;
    volatile int frontSnapshotIndex = 0;
    volatile int endSnapshotIndex = 0;
    int oneFourthIndex = 0;
    int oneHalfIndex = 0;
    int threeFourthIndex = 0;

    ToolbarPanel toolbarPanel;

    public RenderPlayback(JFrame frame, ToolbarPanel toolbarPanel) {
        super(frame);
        this.toolbarPanel = toolbarPanel;
        this.snapshot = null;
        this.isPaused = true;
    }

    public RenderPlayback(DataLogProvider data, JFrame frame, ToolbarPanel toolbarPanel) {
        super(frame);
        this.toolbarPanel = toolbarPanel;
        this.data = data;
        this.snapshot = data.snapshot();
        this.isPaused = false;
    }

    public void updateTimestampLabelIndexes() {
        int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
        if (frontSnapshotIndex > 0) {
            oneFourthIndex = frontSnapshotIndex + (endSnapshotIndex - frontSnapshotIndex) / 4;
            oneHalfIndex = frontSnapshotIndex + (endSnapshotIndex - frontSnapshotIndex) / 2;
            threeFourthIndex = frontSnapshotIndex + (endSnapshotIndex - frontSnapshotIndex) * 3 / 4;
        } else {
            if (endSnapshotIndex == endBandIndex / 4) {
                oneFourthIndex = endSnapshotIndex - 1;
            }
            if (endSnapshotIndex == endBandIndex / 2) {
                oneHalfIndex = endSnapshotIndex - 1;
            }
            if (endSnapshotIndex == endBandIndex * 3 / 4) {
                threeFourthIndex = endSnapshotIndex - 1;
            }
        }
    }

    public synchronized void run() {
        int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
        if (!isPaused) {
            if (!data.stopwatch.isStarted()) {
                data.controlStopwatch("START");
            }
            if (endSnapshotIndex < lastSnapshots.size()) {
                int i = Math.max(endSnapshotIndex, 0);
                long time = lastSnapshots.get(i).time();
                snapshot = data.getSnapshotAtTime(time);
                if (data.snapshotTimeHasOccurred(snapshot)) {
                    popupSnapshots.add(snapshot);
                    toolbarPanel.setValue(popupSnapshots.size());
                    endSnapshotIndex++;
                    frame.repaint();
                    repaintPopups();
                }
                if (endSnapshotIndex - frontSnapshotIndex > endBandIndex) {
                    frontSnapshotIndex++;
                }
            } else {
                Snapshot cur = data.snapshot();
                if (!cur.equals(snapshot)) {
                    snapshot = cur;
                    lastSnapshots.add(new SnapshotView(cur));
                    popupSnapshots.add(cur);
                    endSnapshotIndex = lastSnapshots.size();
                    if (lastSnapshots.size() - frontSnapshotIndex > endBandIndex) {
                        frontSnapshotIndex++;
                    }
                    toolbarPanel.setValue(popupSnapshots.size());
                    frame.repaint();
                    repaintPopups();
                }
            }
            updateTimestampLabelIndexes();
            if (data.isEndOfSnapshots() && endSnapshotIndex >= lastSnapshots.size()) {
                toolbarPanel.setValue(popupSnapshots.size());
                System.out.println("Should only enter here at end of snapshots.");
                data.controlStopwatch("STOP");
                isPaused = true;
            }
        } else {
            updateTimestampLabelIndexes();
            repaintPopups();
            if (data.stopwatch.isStarted()) {
                data.controlStopwatch("STOP");
            }
        }
    }

    public synchronized void stepBackSnapshots(int n) {
        if (lastSnapshots.size() == 0) return;

        frontSnapshotIndex = Math.max(frontSnapshotIndex - n, 0);
        endSnapshotIndex = Math.max(endSnapshotIndex - n, 0);

        int i = Math.max(endSnapshotIndex - 1, 0);
        long time = lastSnapshots.get(i).time();
        data.setStopwatchTime(TimeUnit.MILLISECONDS.toNanos(time));

        snapshot = data.getSnapshotAtTime(time);

        for (int j = 0; j < n; j++) {
            if (popupSnapshots.size() > 0) {
                popupSnapshots.remove(popupSnapshots.size() - 1);
            }
        }
        updateTimestampLabelIndexes();
        toolbarPanel.setValue(popupSnapshots.size());

        frame.repaint();
        repaintPopups();
    }

    public synchronized void stepForwardSnapshots(int n) {
        if (lastSnapshots.size() == 0) return;

        int endBandIndex = (graphWidth - phaseLabelWidth) / STEP_X;
        for (int i = 0; i < n; i++) {
            if (endSnapshotIndex < lastSnapshots.size()) {
                int index = Math.max(endSnapshotIndex, 0);
                long time = lastSnapshots.get(index).time();
                snapshot = data.getSnapshotAtTime(time);
                popupSnapshots.add(snapshot);
                toolbarPanel.setValue(popupSnapshots.size());
            } else {
                // keep processing snapshots from logData until it reaches a diff snapshot from this.snapshot
                Snapshot cur = data.getNextSnapshot();
                while (cur == snapshot && !data.isEndOfSnapshots()) {
                    cur = data.getNextSnapshot();
                }
                if (data.isEndOfSnapshots()) break;

                snapshot = cur;
                lastSnapshots.add(new SnapshotView(cur));
                popupSnapshots.add(cur);
                toolbarPanel.setValue(popupSnapshots.size());
            }
            updateTimestampLabelIndexes();
            data.setStopwatchTime(TimeUnit.MILLISECONDS.toNanos(snapshot.time()));
            endSnapshotIndex++;
        }

        while (endSnapshotIndex - frontSnapshotIndex > endBandIndex) {
            frontSnapshotIndex++;
        }

        frame.repaint();
        repaintPopups();
    }

    synchronized void loadLogDataProvider(DataLogProvider data) {
        this.data = data;
        this.lastSnapshots.clear();
        this.popupSnapshots.clear();
        this.snapshot = data.snapshot();
        this.isPaused = false;
        this.speed = 1.0;
        endSnapshotIndex = 0;
        frontSnapshotIndex = 0;
    }

    public synchronized void renderGraph(Graphics g) {
        if (endSnapshotIndex - frontSnapshotIndex < 2) return;

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

        long firstTime = lastSnapshots.get(frontSnapshotIndex).time();
        long lastTime = lastSnapshots.get(endSnapshotIndex - 1).time();
        double stepX = (double) STEP_X * Math.min(endSnapshotIndex - frontSnapshotIndex, bandWidth) / (lastTime - firstTime);

        for (int i = frontSnapshotIndex; i < endSnapshotIndex; i++) {
            SnapshotView s = lastSnapshots.get(i);
            int x = (int) Math.round((s.time() - firstTime) * stepX);

            if (s.oldPhase() == Phase.MARKING && s.globalPhase() == Phase.IDLE) {
                g.setColor(Colors.OLD[0]);
                g.drawRect(x, bandHeight + pad, 1, phaseHeight);
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


            Graphics2D g2 = (Graphics2D) g;
            g2.setStroke(new BasicStroke(2));
            g2.drawLine(0, bandHeight + 5, 0, bandHeight + pad - 5);
            g2.drawLine(bandWidth / 4, bandHeight + 5, bandWidth / 4, bandHeight + pad - 5);
            g2.drawLine(bandWidth / 2, bandHeight + 5, bandWidth / 2, bandHeight + pad - 5);
            g2.drawLine(bandWidth * 3 / 4, bandHeight + 5, bandWidth * 3 / 4, bandHeight + pad - 5);

            g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() - popupSnapshots.get(frontSnapshotIndex).time()) + " ms", 3, bandHeight + 20);
            if (x >= bandWidth / 4 && popupSnapshots.size() > oneFourthIndex) {
                g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() - popupSnapshots.get(oneFourthIndex).time()) + " ms", bandWidth / 4 + 3, bandHeight + 20);
            }
            if (x >= bandWidth / 2 && popupSnapshots.size() > oneHalfIndex) {
                g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() - popupSnapshots.get(oneHalfIndex).time()) + " ms", bandWidth / 2 + 3, bandHeight + 20);
            }
            if (x >= bandWidth * 3 / 4 && popupSnapshots.size() > threeFourthIndex) {
                g.drawString("-" + Long.toString(popupSnapshots.get(popupSnapshots.size() - 1).time() - popupSnapshots.get(threeFourthIndex).time()) + " ms", bandWidth * 3 / 4 + 3, bandHeight + 20);
            }

        }
    }

    public int getPopupSnapshotsSize() {
        return popupSnapshots.size();
    }

}
