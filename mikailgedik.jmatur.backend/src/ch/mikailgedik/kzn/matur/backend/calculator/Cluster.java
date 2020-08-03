package ch.mikailgedik.kzn.matur.backend.calculator;

import java.lang.invoke.VolatileCallSite;
import java.util.Arrays;
import java.util.Iterator;
import java.util.function.Consumer;
import java.util.function.Function;

public class Cluster<T extends Cluster.Result> implements Iterable<T> {
    //Empty marker interface
    public interface Result {
        boolean getValue();
    }

    private Cluster<T>[] subClusters;
    private T[] values;

    private final double startx, starty, width, height;
    private final int depth;
    private final int tiles;

    @SuppressWarnings("unchecked")
    public Cluster(double startx, double starty, double width, double height, int depth, int tiles) {
        assert tiles > 1;

        this.startx = startx;
        this.starty = starty;
        this.width = width;
        this.height = height;

        this.depth = depth;
        this.tiles = tiles;

        this.subClusters = (Cluster<T>[])new Cluster[tiles * tiles];
        this.values = (T[])new Result[tiles * tiles];
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

    public int getIndex(double x, double y) {
        x -= startx;
        y -= starty;

        x /= width;
        y /= height;

        assert x >= 0;
        assert y >= 0;

        assert x < 1;
        assert y < 1;

        return tiles * ((int)(tiles * y)) + (int)(tiles * x);
    }

    public double[] getCenterCoordinates(int index) {
        int x = index % tiles;
        int y = index / tiles;

        return new double[]{startx + width * ((x + .5) /tiles), starty + height * ( (y + .5) /tiles)};
    }

    public double[] getCoordinates(int index) {
        int x = index % tiles;
        int y = index / tiles;

        return new double[]{startx + width * (((double)x) /tiles), starty + height * ( ((double)y) /tiles)};
    }

    public void set(int index, Cluster<T> n) {
        subClusters[index] = n;
    }

    public Cluster<T> get(int index) {
        return subClusters[index];
    }

    public void set(double x, double y, Cluster<T> n) {
        subClusters[getIndex(x, y)] = n;
    }

    public Cluster<T> get(double x, double y) {
        return subClusters[getIndex(x, y)];
    }

    public void createSubLevels(int[] indices) {
        for(int i: indices) {
            createSubLevelAtIndex(i);
        }
    }

    public void createSubLevels(int maxDepth) {
        if(depth < maxDepth) {
            for(int i = 0; i < subClusters.length; i++) {
                createSubLevelAtIndex(i);
                subClusters[i].createSubLevels(maxDepth);
            }
        }
    }

    public void createAllSubLevels() {
        for(int i = 0; i < subClusters.length; i++) {
            createSubLevelAtIndex(i);
        }
    }

    private void createSubLevelAtIndex(int i) {
        assert subClusters[i] == null;
        double[] co = getCoordinates(i);
        subClusters[i] = new Cluster<>(co[0], co[1], width/tiles, height/tiles, depth + 1, tiles);
    }

    private class ClusterIterator implements Iterator<Cluster<T>> {
        int index = 0;

        @Override
        public boolean hasNext() {
            return index < values.length;
        }

        @Override
        public Cluster<T> next() {
            return subClusters[index++];
        }
    }

    private class ResultIterator implements Iterator<T> {
        int index = 0;

        @Override
        public boolean hasNext() {
            return index < values.length;
        }

        @Override
        public T next() {
            return values[index++];
        }
    }

    public Cluster<T> getSubByIndex(int[] list) {
        Cluster<T> t = this;
        for(int i: list) {
            t = t.subClusters[i];
        }
        return t;
    }

    @Override
    public Iterator<T> iterator() {
        return new ResultIterator();
    }

    public Iterable<Cluster<T>> clusterIterable() {
        return ClusterIterator::new;
    }

    public int getDepth() {
        return depth;
    }

    public int getLength() {
        return values.length;
    }

    public void setValue(int index, T value) {
        values[index] = value;
    }

    public T getValue(int index) {
        return values[index];
    }

    public T getValue(double x, double y) {
        return values[getIndex(x, y)];
    }

    public double getStartx() {
        return startx;
    }

    public double getStarty() {
        return starty;
    }

    public void forEachLowestSub(int maxDepth, Consumer<Cluster<T>> function) {
        assert maxDepth >= this.depth;
        if(maxDepth == getDepth()) {
            function.accept(this);
        } else {
            int subs = 0;
            for(Cluster<T> t: this.clusterIterable()) {
                if(t != null) {
                    subs++;
                }
            }
            if(subs != this.subClusters.length) {
                function.accept(this);
            }
            if(subs > 0) {
                for(Cluster<T> t: this.clusterIterable()) {
                    t.forEachLowestSub(maxDepth, function);
                }
            }

        }
    }
}
