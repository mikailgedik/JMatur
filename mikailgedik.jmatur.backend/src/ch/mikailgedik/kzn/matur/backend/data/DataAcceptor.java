package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;

public abstract class DataAcceptor<T extends Value> {

    public DataAcceptor() {
    }

    public abstract void accept(Cluster<T> t, int clusterX, int clusterY);
}
