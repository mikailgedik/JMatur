package ch.mikailgedik.kzn.matur.core.data;

import java.io.Serializable;

public class Region implements Serializable {
    private final double startX, startY, endX, endY;

    public Region(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getEndX() {
        return endX;
    }

    public double getEndY() {
        return endY;
    }

    public double getWidth() {
        return endX - startX;
    }

    public double getHeight() {
        return endY - startY;
    }

    public String toString() {
        return Region.class.getCanonicalName() +
                "["+getStartX() + ", "+getStartY() + ", "+getEndX() + ", "+getEndY() + " ]";
    }
}