package ch.mikailgedik.kzn.matur.core.data;

import java.util.ArrayList;
import java.util.Iterator;

public class CalculableArea implements Iterable<Cluster> {
    private final LogicalRegion region;
    private final double precision;
    private final ArrayList<Cluster> clusters;

    public CalculableArea(LogicalRegion region, double precision, ArrayList<Cluster> clusters) {
        this.region = region;
        this.precision = precision;
        this.clusters = clusters;
    }

    @Override
    public Iterator<Cluster> iterator() {
        return clusters.iterator();
    }

    public LogicalRegion getRegion() {
        return region;
    }

    public int getDepth() {
        return region.getDepth();
    }

    public double getPrecision() {
        return precision;
    }

    public ArrayList<Cluster> getClusters() {
        return clusters;
    }
}
