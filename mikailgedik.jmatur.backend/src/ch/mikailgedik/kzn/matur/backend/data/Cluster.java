package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;

public class Cluster {
    private int[] value;
    private final int id;
    private long gpuAddress;
    private CLDevice clDevice;

    public Cluster(int[] value, int id) {
        this.value = value;
        this.id = id;
    }

    public int[] getValue() {
        return value;
    }

    public void setValue(int[] value) {
        this.value = value;
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
