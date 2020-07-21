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

    private double midx, midy, zoom;

    public FractalCanvas() {
        scaler = null;
        screen = null;

        this.midx = 0.5;
        this.midy = 0.5;
        this.zoom = 1;
    }

    @Override
    public void paint(Graphics g) {
        //super.paint(g);
        if(screen == null) {
            return;
        }

        BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
        int[] buffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();


        double w = (screen.getWidth() / zoom), h = (screen.getHeight() / zoom);
        double startx = ((midx) * screen.getWidth()) - w/2;
        double starty = ((midy) * screen.getHeight()) - h/2;
        Screen sub = screen.subScreen((int)startx,
                (int)starty,
                (int)w,
                (int)h);

        if(scaler == null || scaler.getWidth() != getWidth() || scaler.getHeight() != getHeight()) {
            //Always change sub screen
            //TODO
            //scaler = new ScreenScaler(sub, getWidth(), getHeight());
        }
        scaler = new ScreenScaler(sub, getWidth(), getHeight());

        System.arraycopy(scaler.scaledInstance().getPixels(), 0, buffer, 0, buffer.length);
        g.drawImage(image, 0, 0, null);
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
        this.scaler = null;

        this.midx = .5;
        this.midy = .5;
        this.zoom = 1;

        repaint();
    }

    public void setZoom(double zoom) {
        this.zoom = zoom;
    }

    public double getZoom() {
        return zoom;
    }

    public void zoom(double factor) {
        zoom *= factor;
    }

    public void setRelativePosition(double relX, double relY) {
        this.midx = relX;
        this.midy = relY;
    }

    public void moveByRelativePosition(double relX, double relY) {
        this.midx += relX;
        this.midy += relY;
    }
}
