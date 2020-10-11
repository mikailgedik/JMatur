package ch.mikailgedik.kzn.matur.backend.calculator;

public class OldDataMandelbrot implements OldResult {

    private final double x, y;
    private final int value;

    public OldDataMandelbrot(double x, double y, int value) {
        this.x = x;
        this.y = y;
        this.value = value;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public int getValue() {
        return value;
    }
}
