package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;

import java.io.IOException;
import java.io.Serializable;

public class Cluster implements Serializable {
    private int[] value;
    private final int id;
    private transient long gpuAddress;
    private transient CLDevice clDevice;

    public Cluster(int[] value, int id) {
        this.value = value;
        assert id >= 0: id;
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

    private void writeObject(java.io.ObjectOutputStream out)
            throws IOException {
        assert this.value != null;
        out.defaultWriteObject();
    }
    private void readObject(java.io.ObjectInputStream in)
            throws IOException, ClassNotFoundException {
        in.defaultReadObject();
    }
}
