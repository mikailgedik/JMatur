package ch.mikailgedik.kzn.matur.backend.data;

import java.io.Serializable;

public class LogicalRegion implements Serializable {
    private final int startX, startY, endX, endY;
    private final int depth;

    public LogicalRegion(int startX, int startY, int endX, int endY, int depth) {
        this.startX = startX;
        this.startY = startY;
        this.endX = endX;
        this.endY = endY;
        this.depth = depth;
    }

    public int getStartX() {
        return startX;
    }

    public int getStartY() {
        return startY;
    }

    public int getEndX() {
        return endX;
    }

    public int getEndY() {
        return endY;
    }

    public int getWidth() {
        return endX - startX;
    }

    public int getHeight() {
        return endY - startY;
    }

    public String toString() {
        return Region.class.getCanonicalName() +
                "["+getStartX() + ", "+getStartY() + ", "+getEndX() + ", "+getEndY() + "]";
    }

    public int getDepth() {
        return depth;
    }
}
