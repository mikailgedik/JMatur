package ch.mikailgedik.kzn.matur.backend.data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Level implements Serializable {
    private final ArrayList<Cluster> clusters;
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

    public Cluster getClusterById(int id) {
        //TODO add binary search for performance
        for(Cluster t: clusters) {
            if(t.getId() == id) {
                return t;
            }
        }
        return null;
    }

    public ArrayList<Cluster> getClusters() {
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

    public void addAll(List<Cluster> others) {
        this.clusters.addAll(others);
        this.clusters.sort(comparator);
    }

    private static Comparator<Cluster> comparator = (o1, o2) -> {
        assert o1.getId() != o2.getId(): "Two objects with same id:" + o1.getId() + ", " + o2.getId();
        return o1.getId() < o2.getId() ? -1: 1;
    };
}
