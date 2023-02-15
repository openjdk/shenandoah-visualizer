package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;

public class RenderRunner implements Runnable {
    final RenderLive live;
    final RenderPlayback playback;

    final JFrame frame;
    boolean isLive;

    public RenderRunner(DataProvider data, JFrame frame, ToolbarPanel toolbarPanel) {
        this.frame = frame;
        live = new RenderLive(data, frame);
        playback = new RenderPlayback(frame, toolbarPanel);
        isLive = true;
    }

    public RenderRunner(DataLogProvider data, JFrame frame, ToolbarPanel toolbarPanel) {
        this.frame = frame;
        live = new RenderLive(frame);
        live.closeDataProvider();
        playback = new RenderPlayback(data, frame, toolbarPanel);
        isLive = false;
    }

    public synchronized void loadPlayback(DataLogProvider data) {
        if (isLive) {
            live.closeDataProvider();
        }
        isLive = false;
        playback.loadLogDataProvider(data);
    }

    public synchronized void loadLive(DataProvider data) {
        if (!isLive) {
            isLive = true;
            live.loadDataProvider(data);
        }
    }

    public synchronized void run() {
        if (isLive) {
            live.run();
        } else {
            playback.run();
        }
    }

    public synchronized Snapshot snapshot() {
        return isLive ? live.snapshot : playback.snapshot;
    }

    public synchronized void renderGraph(Graphics g) {
        if (isLive) {
            live.renderGraph(g);
        } else {
            playback.renderGraph(g);
        }
    }

    public synchronized void renderRegions(Graphics g) {
        if (isLive) {
            live.renderRegions(g);
        } else {
            playback.renderRegions(g);
        }
    }

    public synchronized void renderLegend(Graphics g) {
        if (isLive) {
            live.renderLegend(g);
        } else {
            playback.renderLegend(g);
        }
    }

    public synchronized void notifyRegionResized(int width, int height) {
        live.notifyRegionResized(width, height);
        playback.notifyRegionResized(width, height);
    }

    public synchronized void notifyGraphResized(int width, int height) {
        live.notifyGraphResized(width, height);
        playback.notifyGraphResized(width, height);
    }

    public void addPopup(RegionPopUp popup) {
        if (isLive) {
            this.live.addPopup(popup);
        } else {
            this.playback.addPopup(popup);
        }
    }

    public void deletePopup(RegionPopUp popup) {
        if (isLive) {
            this.live.deletePopup(popup);
        } else {
            this.playback.deletePopup(popup);
        }
    }
}
