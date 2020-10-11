package ch.mikailgedik.kzn.matur.backend.calculator;

public abstract class OldCalculator {

    public OldCalculator() {
    }

    public abstract void calculateBase();

    public abstract void calculateArea
            (double xstart, double xend, double ystart, double yend, double precision);
}
