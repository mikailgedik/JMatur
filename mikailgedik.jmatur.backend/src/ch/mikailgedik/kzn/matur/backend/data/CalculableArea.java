package ch.mikailgedik.kzn.matur.backend.data;

import java.util.ArrayList;
import java.util.Iterator;

public class CalculableArea implements Iterable<Cluster> {
    private final int depth;
    private final double precision;
    private final ArrayList<Cluster> clusters;

    public CalculableArea(int depth, double precision, ArrayList<Cluster> clusters) {
        this.depth = depth;
        this.precision = precision;
        this.clusters = clusters;
    }

    @Override
    public Iterator<Cluster> iterator() {
        return clusters.iterator();
    }

    public int getDepth() {
        return depth;
    }

    public double getPrecision() {
        return precision;
    }

    public ArrayList<Cluster> getClusters() {
        return clusters;
    }
}
