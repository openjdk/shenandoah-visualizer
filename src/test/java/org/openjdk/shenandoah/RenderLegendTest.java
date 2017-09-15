package org.openjdk.shenandoah;

import org.junit.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class RenderLegendTest {

    @Test
    public void test() throws IOException {
        BufferedImage img = new BufferedImage(300, 700, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 300, 700);
        ShenandoahVisualizer.Render.renderLegend(g);
        ImageIO.write(img, "png", new File("legend.png"));
    }

}
