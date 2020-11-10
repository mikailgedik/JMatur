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
    private int[] selectedAreaOnCanvas;
    private int[] selectedAreaOnScreen;
    private String selectedString;

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

        if(selectedAreaOnCanvas != null) {
            Graphics gI = image.getGraphics();
            gI.setColor(Color.RED);
            gI.drawString(selectedString, selectedAreaOnCanvas[0] - 5, selectedAreaOnCanvas[1] - 20);
            gI.fillOval(selectedAreaOnScreen[0] - 5, selectedAreaOnScreen[1] - 5,10,10);
        }

        g.drawImage(image, (getWidth() - screen.getWidth()) / 2, (getHeight() - screen.getHeight()) / 2, null);

        if(selectedAreaOnCanvas != null) {
            g.fillRect(selectedAreaOnCanvas[0], selectedAreaOnCanvas[1], selectedAreaOnCanvas[2] - selectedAreaOnCanvas[0],
                    selectedAreaOnCanvas[3] - selectedAreaOnCanvas[1]);
        }

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

    public Screen getScreen() {
        return screen;
    }

    public void setSelectedArea(int[] selectedArea, String selectedString) {
        this.selectedString = selectedString;
        this.selectedAreaOnCanvas = selectedArea;
        if(selectedAreaOnCanvas != null) {
            this.selectedAreaOnScreen = new int[] {
                    selectedAreaOnCanvas[0] - (getWidth() - screen.getWidth()) / 2,
                    selectedAreaOnCanvas[1] - (getHeight() - screen.getHeight()) / 2,
                    selectedAreaOnCanvas[2] - (getWidth() - screen.getWidth()) / 2,
                    selectedAreaOnCanvas[3] - (getHeight() - screen.getHeight()) / 2
            };
        }

        repaint();
    }

    public double[] getSelectedAreaOnScreen() {
        return new double[] {
                1.0 * selectedAreaOnScreen[0] / screen.getWidth(),
                1.0 * selectedAreaOnScreen[1] / screen.getHeight(),
                1.0 * selectedAreaOnScreen[2] / screen.getWidth(),
                1.0 * selectedAreaOnScreen[3] / screen.getHeight()
        };
    }
}
