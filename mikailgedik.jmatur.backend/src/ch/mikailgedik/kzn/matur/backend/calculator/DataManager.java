package ch.mikailgedik.kzn.matur.backend.calculator;

import java.util.function.Function;

public abstract class DataManager {
    private double startX, startY, width, height;
    public DataManager(double startX, double startY, double endX, double endY) {
        this.startX = startX;
        this.startY = startY;
        this.width = endX -startX;
        this.height = endY -startY;
    }

    public abstract void iterateOverArea
            (double precision, Function<Object, Object> f, double[] area);

    public static DataManager createInstance() {
        return new LazyDataManager(0,0,0,0);
    }

    private static class LazyDataManager extends DataManager {

        public LazyDataManager(double startX, double startY, double endX, double endY) {
            super(startX, startY, endX, endY);
        }

        @Override
        public void iterateOverArea(double precision, Function<Object, Object> f, double[] area) {

        }
    }
}
