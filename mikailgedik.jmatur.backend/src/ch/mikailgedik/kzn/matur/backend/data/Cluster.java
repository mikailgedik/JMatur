package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;

public class Cluster<T extends Value> {
    private final T[] value;
    private final int id;

    public Cluster(T[] value, int id) {
        this.value = value;
        this.id = id;
    }

    public T[] getValue() {
        return value;
    }

    public int getId() {
        return id;
    }
}
