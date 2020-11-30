package ch.mikailgedik.kzn.matur.core.data;

import ch.mikailgedik.kzn.matur.core.opencl.CLDevice;

import java.io.Serializable;

public class Cluster implements Serializable {
    private int[] value;
    private final int id, depth, iterations;
    private transient long gpuAddress;
    private transient CLDevice clDevice;

    public Cluster(int[] value, int id, int depth, int iterations) {
        this.value = value;
        this.depth = depth;
        this.iterations = iterations;
        if(id < 0) {
            throw new RuntimeException("id < 0");
        }
        this.id = id;
    }

    public int getIterations() {
        return iterations;
    }

    public int getDepth() {
        return depth;
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
