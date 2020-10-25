package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;
import org.lwjgl.BufferUtils;
import org.lwjgl.opencl.CL22;

import java.nio.IntBuffer;
import java.util.Arrays;

/**Short for MemoryManager*/
public class MemMan {
    public static void ensureInGPU(CLDevice device, Cluster<ValueMandelbrot> c) {
        assert device != null;
        if(c.getDevice() == null) {
            copyToGPU(device, c);
        } else if (c.getDevice() != device){
            moveToRAM(c);
            moveToGPU(device, c);
        }
    }

    public static void allocateInGPU(CLDevice device, Cluster<ValueMandelbrot> c, int totalSize) {
        assert c.getGPUAddress() == 0;
        assert c.getDevice() == null;
        long mem = MemMan.allocateReadWriteMemory(device, totalSize * Integer.BYTES);
        c.setDevice(device, mem);
    }

    /** Memory is always read/write*/
    public static void copyToGPU(CLDevice device, Cluster<ValueMandelbrot> c) {
        assert c.getGPUAddress() == 0;
        assert c.getValue() != null;

        int[] val = new int[c.getValue().length];
        for(int i = 0; i < val.length; i++) {
            val[i] = c.getValue()[i].getValue();
        }

        int[] error = new int[1];
        c.setDevice(device, CL22.clCreateBuffer(device.getContext(),
                CL22.CL_MEM_READ_WRITE | CL22.CL_MEM_COPY_HOST_PTR, val, error));
        if(error[0] == CL22.CL_MEM_OBJECT_ALLOCATION_FAILURE) {
            //TODO free some memory
            assert false: "Memory allocation failure";
        }
        check(error);
    }

    public static void copyToRAM(Cluster<ValueMandelbrot> c) {
        assert c.getValue() != null;
        assert c.getDevice() != null;
        assert c.getGPUAddress() != 0;

        int[] data = new int[c.getValue().length];
        int error = CL22.clEnqueueReadBuffer(c.getDevice().getCommandQueue(), c.getGPUAddress(), true, 0L,
                data, null, null);
        check(error);
        for(int i = 0; i < data.length; i++) {
            c.getValue()[i] = new ValueMandelbrot(data[i]);
        }
    }

    public static void copyToArray(CLDevice device, long pointer, int[] result) {
        int error = CL22.clEnqueueReadBuffer(device.getCommandQueue(), pointer, true, 0L,
                result, null, null);
        check(error);
    }

    public static void moveToRAM(Cluster<ValueMandelbrot> c) {
        copyToRAM(c);
        freeMemoryObject(c.getGPUAddress());
        c.setDevice(null, 0);
    }

    public static void moveToGPU(CLDevice device, Cluster<ValueMandelbrot> c) {
        copyToGPU(device, c);
        //TODO free memory better: delete whole array instead of c.value[i]
        Arrays.fill(c.getValue(), null);
    }

    public static long allocateReadWriteMemory(CLDevice device, long size) {
        IntBuffer errorBuffer = BufferUtils.createIntBuffer(1);
        long result = CL22.clCreateBuffer(device.getContext(), CL22.CL_MEM_READ_WRITE,
                size, errorBuffer);
        if(errorBuffer.get(0) == CL22.CL_MEM_OBJECT_ALLOCATION_FAILURE) {
            //TODO free some memory
            assert false: "Memory allocation failure";
        }
        check(errorBuffer);
        return result;
    }

    public static long allocateAsWriteMemory(CLDevice device, long size) {
        IntBuffer errorBuffer = BufferUtils.createIntBuffer(1);
        long result = CL22.clCreateBuffer(device.getContext(), CL22.CL_MEM_WRITE_ONLY,
                size, errorBuffer);
        if(errorBuffer.get(0) == CL22.CL_MEM_OBJECT_ALLOCATION_FAILURE) {
            //TODO free some memory
            assert false: "Memory allocation failure";
        }
        check(errorBuffer);
        return result;
    }

    public static long allocateAsReadMemory(CLDevice device, double[] value) {
        int[] error = new int[1];
        long result = CL22.clCreateBuffer(device.getContext(),
                CL22.CL_MEM_READ_ONLY | CL22.CL_MEM_COPY_HOST_PTR, value, error);
        if(error[0] == CL22.CL_MEM_OBJECT_ALLOCATION_FAILURE) {
            //TODO free some memory
            assert false: "Memory allocation failure";
        }
        return result;
    }

    public static long allocateAsReadMemory(CLDevice device, int[] value) {
        int[] error = new int[1];
        long result = CL22.clCreateBuffer(device.getContext(),
                CL22.CL_MEM_READ_ONLY | CL22.CL_MEM_COPY_HOST_PTR, value, error);
        if(error[0] == CL22.CL_MEM_OBJECT_ALLOCATION_FAILURE) {
            //TODO free some memory
            assert false: "Memory allocation failure";
        }
        return result;
    }

    public static void freeMemoryObject(long object) {
        int error;
        error = CL22.clReleaseMemObject(object);
        check(error);
    }

    private static void check(int[] error) {
        check(error[0]);
    }
    private static void check(int error) {
        if(error != CL22.CL_SUCCESS) {
            throw new RuntimeException("OpenCL error: " + error);
        }
    }
    private static void check(IntBuffer error) {
        if(error.get(0) != CL22.CL_SUCCESS) {
            throw new RuntimeException("OpenCL error: " + error.get(0));
        }
    }
}
