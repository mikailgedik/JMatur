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
        super.paint(g);
        if(screen == null) {
            return;
        }

        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] buffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

        if(screen.getWidth() == getWidth() && screen.getHeight() == getHeight()) {
            System.arraycopy(screen.getPixels(), 0, buffer, 0, buffer.length);
        } else {
            if(scaler == null || scaler.getWidth() != getWidth() || scaler.getHeight() != getHeight()) {
                scaler = new ScreenScaler(screen, getWidth(), getHeight());
            }
            System.arraycopy(scaler.scaledInstance().getPixels(), 0, buffer, 0, buffer.length);
        }

        g.drawImage(image, 0, 0, null);
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
        this.scaler = null;

        repaint();
    }
}
