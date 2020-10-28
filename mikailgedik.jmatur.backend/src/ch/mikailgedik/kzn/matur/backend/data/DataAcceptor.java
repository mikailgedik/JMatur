package ch.mikailgedik.kzn.matur.backend.data;

public abstract class DataAcceptor {

    public DataAcceptor() {
    }

    public abstract void accept(Cluster t, int clusterX, int clusterY);
}
