package org.openjdk.shenandoah;

import java.awt.*;

public class LayoutConstants {
    public static final int LINE = 15;

    public static void renderTimeLineLegendItem(Graphics g, int sqSize, Color color, int lineNumber, String label) {
        g.setColor(color);
        int y = (int) (lineNumber * LINE * 1.5);
        g.fillRect(0, y, sqSize, sqSize);
        g.setColor(Color.BLACK);
        g.drawString(label, (int) (sqSize * 1.5), y + sqSize);
    }
}
