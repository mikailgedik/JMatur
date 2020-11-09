package ch.mikailgedik.kzn.matur.frontend.window;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class FractalCanvas extends JComponent {
    private Screen screen;
    private BufferedImage image;
    private int[] buffer;
    private boolean busy;

    public FractalCanvas() {
        screen = null;
    }

    @Override
    public void paint(Graphics g) {
        if(screen == null) {
            g.setColor(Color.BLACK);
            g.fillRect(0,0,getWidth(), getHeight());
            g.setColor(new Color(0xFF00FF99, true));
            g.fillRect(0,0, getWidth(), getHeight()/10);
            g.setColor(new Color(0xff0000));
            g.drawString("Calculating...", 0, getHeight()/15);
            return;
        }

        System.arraycopy(screen.getPixels(), 0, buffer, 0, buffer.length);

        g.drawImage(image, (getWidth() - screen.getWidth()) / 2, (getHeight() - screen.getHeight()) / 2, null);

        if(busy) {
            g.setColor(new Color(0xFF00FF99, true));
            g.fillRect(0,0, getWidth(), getHeight()/10);
            g.setColor(new Color(0xff0000));
            g.drawString("Calculating...", 0, getHeight()/15);
        }
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
        if(this.image == null || this.image.getWidth() != screen.getWidth() ||
                this.image.getHeight() != screen.getHeight()) {
            image = new BufferedImage(screen.getWidth(), screen.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
            buffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();
        }

        repaint();
    }

    public void setBusy(boolean gray) {
        this.busy = gray;
        repaint();
    }
}
