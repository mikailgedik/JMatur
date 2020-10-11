package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;

public abstract class DataAcceptor<T extends Value> {
    private double precision;

    public DataAcceptor() {
        this.precision = 0;
    }

    public abstract void accept(Cluster<T> t, int clusterX, int clusterY);

    public double getPrecision() {
        return precision;
    }

    public void setPrecision(double precision) {
        this.precision = precision;
    }
}
