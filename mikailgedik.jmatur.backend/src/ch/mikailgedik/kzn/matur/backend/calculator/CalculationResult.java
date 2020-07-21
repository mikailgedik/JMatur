package ch.mikailgedik.kzn.matur.backend.calculator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Consumer;

public class CalculationResult<T> implements Iterable<CalculationResult.Cluster<T>>{
    private final Cluster<T>[][] clusters;
    public CalculationResult(double[] xBounds, double[] yBounds, int initialSize) {
        this(xBounds, yBounds, (xBounds[1] - xBounds[0]) / 10, (yBounds[1] - yBounds[0]) / 10);
    }

    @SuppressWarnings("unchecked")
    public CalculationResult(double[] xBounds, double[] yBounds, double chunkWidth, double chunkHeight) {
        int xChunks = (int)((xBounds[1] - xBounds[0]) / chunkWidth), yChunks = (int)((yBounds[1] - yBounds[0]) / chunkHeight);
        clusters = (Cluster<T>[][]) new Cluster[xChunks][yChunks];

        for(int x = 0; x < clusters.length; x++) {
            for(int y = 0; y < clusters[0].length; y++) {
                clusters[x][y] = new Cluster<>(xBounds[0] + (x) * chunkWidth, yBounds[0] + (y) * chunkHeight,
                        xBounds[0] + (x + 1) * chunkWidth, yBounds[0] + (y + 1) * chunkHeight,
                        100);
            }
        }
    }

    public Cluster<T> getCluster(double xCo, double yCo) {
        int x = 0, y = 0;
        Cluster<T> chunk;
        while(clusters[x][y].xend < xCo) {
            x++;
        }
        while((chunk = clusters[x][y]).yend < yCo) {
            y++;
        }
        return chunk;
    }

    public void addOtherResult(CalculationResult<T> other) {
        assert false;
    }

    @Override
    public Iterator<Cluster<T>> iterator() {
        return new ClusterIterator();
    }

    private class ClusterIterator implements Iterator<Cluster<T>> {
        int x = 0, y = 0;

        @Override
        public boolean hasNext() {
            if(x == clusters.length - 1) {
                return y != clusters[x].length;
            } else {
                return true;
            }
        }

        @Override
        public Cluster<T> next() {
            if(y == clusters[x].length) {
                x++;
                y = 0;
            }
            try {
                return clusters[x][y++];
            } catch (IndexOutOfBoundsException e) {
                throw new RuntimeException(x +", " + y, e);
            }
        }
    }

    public static class DataMandelbrot {
        private double x, y;
        private boolean value;

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

    public static class Cluster<A> implements Iterable<A> {
        private ArrayList<A> data;
        private double xstart, ystart, xend, yend;

        public Cluster(double xstart, double ystart, double endx, double endy, int initsize) {
            data = new ArrayList<>(initsize);
            this.xend = endx;
            this.yend = endy;
            this.xstart = xstart;
            this.ystart = ystart;
        }

        public void add(A t) {
            data.add(t);
        }

        public double getXstart() {
            return xstart;
        }

        public double getYstart() {
            return ystart;
        }

        public double getXend() {
            return xend;
        }

        public double getYend() {
            return yend;
        }

        public int size() {
            return data.size();
        }

        public A get(int index) { return data.get(index); }

        @Override
        public Iterator<A> iterator() {
            return data.iterator();
        }
    }
}
