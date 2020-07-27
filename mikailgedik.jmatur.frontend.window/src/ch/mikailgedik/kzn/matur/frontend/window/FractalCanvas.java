package ch.mikailgedik.kzn.matur.frontend.window;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.connector.ScreenScaler;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class FractalCanvas extends JComponent {
    private Screen screen;
    private ScreenScaler scaler;

    public FractalCanvas() {
        scaler = null;
        screen = null;
    }

    @Override
    public void paint(Graphics g) {
        //super.paint(g);
        if(screen == null) {
            g.drawString("No image", 50, 50);
            return;
        }

        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] buffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        scaler = new ScreenScaler(screen, getWidth(), getHeight());

        System.arraycopy(scaler.scaledInstance().getPixels(), 0, buffer, 0, buffer.length);
        g.drawImage(image, 0, 0, null);
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
        this.scaler = null;

        repaint();
    }
}
