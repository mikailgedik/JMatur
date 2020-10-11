package ch.mikailgedik.kzn.matur.backend.calculator;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public abstract class OldCalculationResult<T extends OldResult> {
    private final int tiles;
    private final double width, height, startX, startY;

    /** content.get(level)[cluster][value]
     *
     * */
    private final ArrayList<Level<T>> content;

    public static abstract class Level<T extends OldResult> {
        private final int depth;
        private final int size;

        public Level(int depth, int size) {
            this.size = size;
            this.depth = depth;
        }

        public abstract T[] getAt(int index);

        public abstract void putAt(int index, T[] value);

        public int getSize() {
            return size;
        }

        public abstract int totalElements();

        public int getDepth() {
            return depth;
        }
    }

    private class LazyLevel extends Level<T> {
        private final List<Cluster> clusters;

        public LazyLevel(int depth, int size) {
            super(depth, size);
            this.clusters = Collections.synchronizedList(new ArrayList<>());
        }

        @Override
        public T[] getAt(int index) {
            for(Cluster c: clusters) {
                if(c.index == index) {
                    return c.values;
                }
            }
            clusters.add(new Cluster(index, constructCluster()));

            return clusters.get(clusters.size() - 1).values;
        }

        @Override
        public void putAt(int index, T[] value) {
            assert index >= 0 && index < totalElements();
            for (Cluster cluster : clusters) {
                if (cluster.index == index) {
                    cluster.values = value;
                    return;
                }
            }
            clusters.add(new Cluster(index, value));
        }

        private class Cluster {
            T[] values;
            int index;

            Cluster(int i, T[] v) {
                index = i;
                values = v;
            }
        }

        @Override
        public int totalElements() {
            return getSize() * getSize();
        }
    }



    private static class AbsoluteLevel<T extends OldResult> extends Level<T> {
        private final T[][] content;

        public AbsoluteLevel(int depth, int size, T[][] content) {
            super(depth, size);
            this.content = content;
        }

        public T[][] getAll() {
            return content;
        }

        @Override
        public T[] getAt(int index) {
            return content[index];
        }

        @Override
        public void putAt(int index, T[] value) {
            content[index] = value;
        }

        @Override
        public int totalElements() {
            return content.length;
        }
    }

    public OldCalculationResult(double[] xBounds, double[] yBounds, int tiles) {
        content = new ArrayList<>();

        this.startX = xBounds[0];
        this.width = xBounds[1] - xBounds[0];

        this.startY = yBounds[0];
        this.height = yBounds[1]- yBounds[0];
        this.tiles = tiles;
    }

    public double[] centerCoordinates(int depth, int cluster, int value) {
        int l = (int)Math.pow(this.tiles, depth);

        double[] ret = new double[] {
                (cluster % l) * (width / l) + startX,
                (int)(cluster / l) * (height / l) + startY
        };
        l *= tiles;

        ret[0] += (value % tiles) * (width / l);
        ret[1] += (int)(value / tiles) * (height / l);

        ret[0] += (width / l / 2);
        ret[1] += (height / l / 2);

        return ret;
    }

    public double[] centerCoordinates(int depth, int clusterX, int clusterY, int value) {
        int l = (int)Math.pow(this.tiles, depth);

        double[] ret = new double[] {
                clusterX * (width / l) + startX,
                clusterY * (height / l) + startY
        };
        l *= tiles;

        ret[0] += (value % tiles) * (width / l);
        ret[1] += (value / tiles) * (height / l);

        ret[0] += (width / l / 2);
        ret[1] += (height / l / 2);

        return ret;
    }

    /** Only creates new levels, does not delete old ones*/
    public void ensureDepth(int depth) {
        for(int i = content.size(); i <= depth; i++) {
            System.out.print("Constructing depth " + i + "... ");
            if(i < 100) {
                Level<T> level = constructAbsoluteLevel(i);
                content.add(level);

                for(int j = 0; j < level.totalElements(); j++) {
                    level.putAt(j, constructCluster());
                }
            } else {
                Level<T> level = constructLazyLevel(i);
                content.add(level);
            }

            System.out.println("constructed");
        }
    }

    private Level<T> constructLazyLevel(int depth) {
        assert content.size() >= depth;

        int size = depth == 0 ? 1 :content.get(depth - 1).getSize() * getTiles();

        return new LazyLevel(depth, size);
    }

    private Level<T> constructAbsoluteLevel(int depth) {
        assert content.size() >= depth;

        int size = depth == 0 ? 1 :content.get(depth - 1).getSize() * getTiles();

        return new AbsoluteLevel<>(depth, size, createLevel(size * size));
    }

    protected abstract T[] constructCluster();

    protected abstract T[][] createLevel(int arraySize);

    public Level<T> getLevel(int depth) {
        return content.get(depth);
    }

    public ArrayList<Level<T>> getLevels() {
        return content;
    }

    public T[] getCluster(int depth, int cluster) {
        return getLevel(depth).getAt(cluster);
    }

    public static class CalculationResultMandelbrot extends OldCalculationResult<OldDataMandelbrot> {
        public CalculationResultMandelbrot(double[] xBounds, double[] yBounds, int tiles) {
            super(xBounds, yBounds, tiles);
        }

        @Override
        protected OldDataMandelbrot[][] createLevel(int arraySize) {
            return new OldDataMandelbrot[arraySize][];
        }

        @Override
        protected OldDataMandelbrot[] constructCluster() {
            return new OldDataMandelbrot[length()];
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
