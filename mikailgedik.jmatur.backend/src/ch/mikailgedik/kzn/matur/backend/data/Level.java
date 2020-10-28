package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Level<T extends Value> {
    private final ArrayList<Cluster<T>> clusters;
    private final int depth;
    //Amount of clusters in x-direction
    private final int logicalWidth;
    //Amount of clusters in y-direction
    private final int logicalHeight;
    private final double precision;
    private final int iterations;

    public Level(int depth, int logicalWidth, int logicalHeight, double precision, int iterations) {
        this.clusters = new ArrayList<>();
        this.depth = depth;
        this.logicalWidth = logicalWidth;
        this.logicalHeight = logicalHeight;
        this.precision = precision;
        this.iterations = iterations;
    }

    public Cluster<T> getClusterById(int id) {
        //TODO add binary search for performance
        for(Cluster<T> t: clusters) {
            if(t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    public ArrayList<Cluster<T>> getClusters() {
        return clusters;
    }

    public int getDepth() {
        return depth;
    }

    public int getLogicalWidth() {
        return logicalWidth;
    }

    public int getLogicalHeight() {
        return logicalHeight;
    }

    public int getIterations() {
        return iterations;
    }

    public double getPrecision() {
        return precision;
    }

    public void addAll(List<Cluster<T>> others) {
        this.clusters.addAll(others);
        this.clusters.sort(comparator);
    }

    private Comparator<Cluster<T>> comparator = (o1, o2) -> {
        assert o1.getId() != o2.getId(): "Two objects with same id";
        return o1.getId() < o2.getId() ? -1: 1;
    };
}
