package ch.mikailgedik.kzn.matur.backend.calculator;

public class CalculationResult<T extends Cluster.Result> {
    private final Cluster<T> firstCluster;

    public CalculationResult(double[] xBounds, double[] yBounds, int tiles) {
        firstCluster = new Cluster<>(xBounds[0], yBounds[0], xBounds[1] - xBounds[0], yBounds[1]- yBounds[0], 0, tiles);
    }

    public Cluster<T> getCluster() {
        return firstCluster;
    }

    public T getNearest(double x, double y) {
        T t;
        Cluster<T> curr = firstCluster, next;
        while((next = curr.get(x, y)) != null) {
            curr = next;
        }
        return curr.getValue(x, y);
    }

    public static class DataMandelbrot implements Cluster.Result {
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

        public boolean getValue() {
            return value;
        }
    }
}
