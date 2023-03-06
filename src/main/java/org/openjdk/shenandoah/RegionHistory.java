package org.openjdk.shenandoah;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.awt.*;
import java.util.List;

public class RegionHistory extends JFrame implements DocumentListener {
    public static final String DEFAULT_REGION_SELECTION = "0, 100 - 130, 2000";
    public static final int MIN_REGION_HEIGHT = 5;
    private static final int MAX_REGION_HEIGHT = 25;

    private final RenderRunner renderRunner;

    private final List<Integer> regions;

    private final RegionSelectionParser parser;

    public RegionHistory(RenderRunner renderRunner) {
        this.renderRunner = renderRunner;
        this.parser = new RegionSelectionParser();
        this.regions = parser.parse(DEFAULT_REGION_SELECTION);

        setSize(600, 500);
        setLayout(new BorderLayout());
        setTitle("Region History");

        Container content = getContentPane();
        var regionSelection = Box.createHorizontalBox();
        regionSelection.add(new JLabel("Regions"));
        JTextField regionInput = new JTextField(DEFAULT_REGION_SELECTION, 25);
        regionInput.getDocument().addDocumentListener(this);
        regionSelection.add(regionInput);
        var historyPanel = new RegionHistoryPanel();

        content.add(regionSelection, BorderLayout.NORTH);
        content.add(historyPanel, BorderLayout.CENTER);
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        try {
            Document document = e.getDocument();
            updateRegionSection(document.getText(0, document.getLength()));
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        try {
            Document document = e.getDocument();
            updateRegionSection(document.getText(0, document.getLength()));
        } catch (BadLocationException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
    }

    private void updateRegionSection(String expression) {
        try {
            List<Integer> selection = parser.parse(expression);
            regions.clear();
            regions.addAll(selection);
        } catch (Exception e) {
            // TODO: Show parse errors somewhere
        }
    }

    private class RegionHistoryPanel extends JPanel {
        @Override
        public void paint(Graphics g) {
            // TODO: Scroll bars?
            // TODO: Fix keyboard shortcuts for this window
            // TODO: Tooltips for region detail
            if (regions.isEmpty()) {
                g.drawString("No regions selected.", 10, 10);
                return;
            }

            Rectangle viewport = g.getClipBounds();
            int regionSquareSize = clamp(viewport.height / regions.size());
            renderRegionLabels(g, regionSquareSize, 1);
            renderRegionHistory(g, viewport, regionSquareSize, 21);
        }

        private void renderRegionHistory(Graphics g, Rectangle viewport, int regionSquareSize, int x) {
            List<Snapshot> snapshots = renderRunner.snapshots();
            for (int i = snapshots.size() - 1; i >= 0; i--) {
                x += regionSquareSize;
                Snapshot snapshot = snapshots.get(i);
                int y = 1;
                for (var region : regions) {
                    RegionStat r = snapshot.get(region);
                    r.render(g, x, y, regionSquareSize, regionSquareSize);
                    y += regionSquareSize;
                    if (y > (viewport.height - regionSquareSize)) {
                        // Break a bit early to leave room for a row with timestamps
                        break;
                    }
                }

                y += regionSquareSize;
                if (i % 10 == 0) {
                    g.setColor(Color.BLACK);
                    g.drawString(snapshot.time() + "ms", x, y);
                }

                if (x > viewport.width) {
                    break;
                }
            }
        }

        private void renderRegionLabels(Graphics g, int regionSquareSize, int x) {
            int labelStride = regionSquareSize < 10 ? 10 : 1;
            for (int i = 0; i < regions.size(); i += labelStride) {
                int region = regions.get(i);
                g.setColor(Color.BLACK);
                g.drawString(String.valueOf(region), x, (i + 1) * regionSquareSize);
            }
        }
    }

    private static int clamp(int value) {
        return Math.min(Math.max(MIN_REGION_HEIGHT, value), MAX_REGION_HEIGHT);
    }
}
