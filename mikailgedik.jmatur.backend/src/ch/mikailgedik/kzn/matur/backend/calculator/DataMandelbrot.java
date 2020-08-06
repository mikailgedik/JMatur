package ch.mikailgedik.kzn.matur.backend.calculator;

public class DataMandelbrot implements Result {

    private final double x, y;
    private final boolean value;

    public DataMandelbrot(double x, double y, boolean value) {
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

    public int getColor() {
        return value ? 0xff00ff : 0x00ff00;
    }
}
