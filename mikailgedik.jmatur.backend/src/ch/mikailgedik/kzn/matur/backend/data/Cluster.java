package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;

public class Cluster<T extends Value> {
    private final T[] value;
    private final int id;
    private long gpuAddress;
    private CLDevice clDevice;

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

    public long getGPUAddress() {
        return gpuAddress;
    }

    public CLDevice getDevice() {
        return clDevice;
    }

    public void setDevice(CLDevice clDevice, long gpuAddress) {
        this.clDevice = clDevice;
        this.gpuAddress = gpuAddress;
    }
}
