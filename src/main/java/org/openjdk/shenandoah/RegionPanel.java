package org.openjdk.shenandoah;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class RegionPanel extends JPanel {
    private final RenderRunner renderRunner;

    int regionWidth, regionHeight;

    public RegionPanel(RenderRunner renderRunner, KeyAdapter keyboardShortCuts) {
        this.renderRunner = renderRunner;

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent ev) {
                regionWidth = ev.getComponent().getWidth();
                regionHeight = ev.getComponent().getHeight();
            }
        });

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                Snapshot snapshot = renderRunner.snapshot();
                int area = regionWidth * regionHeight;
                int sqSize = Math.max(1, (int) Math.sqrt(1D * area / snapshot.regionCount()));
                int cols = regionWidth / sqSize;
                int regionNumber = (e.getX() / sqSize) + ((e.getY() / sqSize) * cols) ;
                if (regionNumber >= 0 && regionNumber < snapshot.statsSize()) {
                    RegionPopUp popup = new RegionPopUp(regionNumber);
                    popup.setSize(450, 450);
                    popup.setLocation(e.getX(), e.getY());
                    popup.setVisible(true);
                    popup.addKeyListener(keyboardShortCuts);
                    popup.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosing(WindowEvent e) {
                            super.windowClosing(e);
                            popup.setVisible(false);
                            popup.dispose();
                            renderRunner.deletePopup(popup);
                        }
                    });
                    renderRunner.addPopup(popup);
                }
            }
        });
    }

    @Override
    public void paint(Graphics g) {
        Snapshot snapshot = renderRunner.snapshot();
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
}
