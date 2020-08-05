package ch.mikailgedik.kzn.matur.backend.calculator;

import java.util.ArrayList;
import java.util.function.Consumer;

public abstract class CalculationResult<T extends Result> {
    private final int tiles;
    private final double width, height, startX, startY;

    /** content.get(level)[cluster][value]
     *
     * */
    private final ArrayList<Level<T>> content;

    public static class Level<T extends Result> {
        private int depth;
        private int size;
        private T[][] content;

        public Level(int depth, int size, T[][] content) {
            this.size = size;
            this.content = content;
            this.depth = depth;
        }

        public T[][] get() {
            return content;
        }

        public int getSize() {
            return size;
        }

        public int getDepth() {
            return depth;
        }

        public void set(T[][] content) {
            this.content = content;
            assert content.length == size * size;
        }
    }

    public CalculationResult(double[] xBounds, double[] yBounds, int tiles) {
        content = new ArrayList<>();

        this.startX = xBounds[0];
        this.width = xBounds[1] - xBounds[0];

        this.startY = yBounds[0];
        this.height = yBounds[1]- yBounds[0];
        this.tiles = tiles;
    }

    public double[] startCoordinates(int depth, int cluster, int value) {
        int l = (int)Math.pow(this.tiles, depth);

        double[] ret = new double[] {
                (cluster % l) * (width / l) + startX,
                (cluster / l) * (height / l) + startY
        };
        l *= tiles;

        ret[0] += (value % tiles) * (width / l);
        ret[1] += (value / tiles) * (height / l);


        return ret;
    }

    public double[] startCoordinates(int depth, int cluster) {
        int l = (int)Math.pow(this.tiles, depth);

        return new double[] {
                (cluster % l) * (width / l) + startX,
                (cluster / l) * (height / l) + startY
        };
    }

    public double[] centerCoordinates(int depth, int cluster, int value) {
        int l = (int)Math.pow(this.tiles, depth);

        double[] ret = new double[] {
                (cluster % l) * (width / l) + startX,
                (cluster / l) * (height / l) + startY
        };
        l *= tiles;

        ret[0] += (value % tiles) * (width / l);
        ret[1] += (value / tiles) * (height / l);

        ret[0] += (width / l / 2);
        ret[1] += (height / l / 2);

        return ret;
    }

    public double[] centerCoordinates(int depth, int cluster) {
        int l = (int)Math.pow(this.tiles, depth);

        double[] ret = new double[] {
                (cluster % l) * (width / l) + startX,
                (cluster / l) * (height / l) + startY
        };

        ret[0] += (width / l / 2);
        ret[1] += (height / l / 2);

        return ret;
    }

    public void create(int depth) {
        for(int i = 0; i <= depth; i++) {
            Level<T> level = constructLevel(i);
            content.add(level);

            for(int j = 0; j < level.get().length; j++) {
                level.get()[j] = constructCluster();
            }
        }
    }

    private Level<T> constructLevel(int depth) {
        assert content.size() >= depth;

        int size = depth == 0 ? 1 :content.get(depth - 1).size * getTiles();

        return new Level<>(depth, size, createLevel(size * size));
    }

    protected abstract T[] constructCluster();

    protected abstract T[][] createLevel(int size);

    public Level<T> getLevel(int depth) {
        return content.get(depth);
    }

    public T[] getCluster(int depth, int cluster) {
        return getLevel(depth).get()[cluster];
    }

    public static class CalculationResultMandelbrot extends CalculationResult<DataMandelbrot> {
        public CalculationResultMandelbrot(double[] xBounds, double[] yBounds, int tiles) {
            super(xBounds, yBounds, tiles);
        }

        @Override
        protected DataMandelbrot[][] createLevel(int length) {
            return new DataMandelbrot[length][];
        }

        @Override
        protected DataMandelbrot[] constructCluster() {
            return new DataMandelbrot[length()];
        }
    }

    public int getTiles() {
        return tiles;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public int length() {
        return getTiles() * getTiles();
    }
}
