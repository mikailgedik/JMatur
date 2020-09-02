package ch.mikailgedik.kzn.matur.backend.calculator;

import java.awt.*;
import java.util.function.Function;

public class DataMandelbrot implements Result {

    private final double x, y;
    private final int value;

    public DataMandelbrot(double x, double y, int value) {
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
