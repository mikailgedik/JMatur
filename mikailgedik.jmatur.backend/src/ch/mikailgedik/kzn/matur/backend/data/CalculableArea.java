package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;

import java.util.ArrayList;
import java.util.Iterator;

public class CalculableArea<T extends Value> implements Iterable<Cluster<T>> {
    private final int depth;
    private final double precision;
    private final ArrayList<Cluster<T>> clusters;

    public CalculableArea(int depth, double precision, ArrayList<Cluster<T>> clusters) {
        this.depth = depth;
        this.precision = precision;
        this.clusters = clusters;
    }

    @Override
    public Iterator<Cluster<T>> iterator() {
        return clusters.iterator();
    }

    public int getDepth() {
        return depth;
    }

    public double getPrecision() {
        return precision;
    }

    public ArrayList<Cluster<T>> getClusters() {
        return clusters;
    }
}
