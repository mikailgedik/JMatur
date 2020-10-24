package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL22;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

public class CalculatorUnitGPU implements CalculatorUnit {
    private static final String KERNEL_NAME="mandelbrot", KERNEL = "/clkernels/" + KERNEL_NAME + ".cl";
    private final long device;
    private long context, commandQueue, program, kernel;
    private long[] memObjects;
    private int logicClusterWidth, logicClusterHeight, maxIterations;
    private DataSet<ValueMandelbrot> currentDataSet;
    private double precision;
    private int depth;

    private ArrayList<Cluster<ValueMandelbrot>> clusters;
    private Thread thread;

    public CalculatorUnitGPU(long device) {
        this.device = device;
        this.clusters = new ArrayList<>();
        createContext();
        createCommandQueue();
        createProgram();
        buildProgram();
        createKernel();
    }

    private void buildProgram() {
        int error = CL22.clBuildProgram(program, null,
                "", null, 0);

        if(error != CL22.CL_SUCCESS) {
            printBuildError(program, device);
            throw new RuntimeException("OpenCL error: " + error);
        }
    }

    private void createKernel() {
        IntBuffer berr = BufferUtils.createIntBuffer(1);
        kernel = CL22.clCreateKernel(program, KERNEL_NAME, berr);
        assert berr.get(0) == CL22.CL_SUCCESS: berr.get(0);
    }

    private void createProgram() {
        IntBuffer error = BufferUtils.createIntBuffer(1);

        String kernel = readKernel(KERNEL);

        program = CL22.clCreateProgramWithSource(context, kernel, error);
        //https://www.codeproject.com/articles/86551/part-1-programming-your-graphics-card-gpu-with-jav
        check(error);
    }

    public void delete() {
        //TODO release context
    }

    @Override
    public void addCluster(Cluster<ValueMandelbrot> cluster) {
        clusters.add(cluster);
    }

    @Override
    public void startCalculation(int logicClusterWidth, int logicClusterHeight, int maxIterations, int depth, double precision, DataSet<ValueMandelbrot> dataSet) {
        this.logicClusterWidth = logicClusterWidth;
        this.logicClusterHeight = logicClusterHeight;
        this.maxIterations = maxIterations;
        this.depth = depth;
        this.precision = precision;
        this.currentDataSet = dataSet;

        this.thread = new Thread(() -> {
            for (Cluster<ValueMandelbrot> c : clusters) {
                submit(c);
                CL22.clFinish(commandQueue);
            }
        });
        this.thread.start();
    }

    private void submit(Cluster<ValueMandelbrot> c) {
        allocateMemory(c);
        runKernel();
        readBackMemory(c);
        releaseMemory();
    }

    private void readBackMemory(Cluster<ValueMandelbrot> c) {
        int[] data = new int[logicClusterWidth * logicClusterHeight];
        int error = CL22.clEnqueueReadBuffer(commandQueue, memObjects[1], true, 0L,
                data, null, null);
        check(error);
        for(int i = 0; i < data.length; i++) {
            c.getValue()[i] = new ValueMandelbrot(data[i]);
        }
    }

    private void runKernel() {
        ByteBuffer global_work_size = BufferUtils.createByteBuffer(Long.BYTES);
        ByteBuffer local_work_size = BufferUtils.createByteBuffer(Long.BYTES);

        global_work_size.asLongBuffer().put(logicClusterHeight * logicClusterWidth);
        local_work_size.asLongBuffer().put(1);

        int error = CL22.clEnqueueNDRangeKernel(commandQueue, kernel, 1,
                null,
                PointerBuffer.create(global_work_size),
                PointerBuffer.create(local_work_size),
                null,
                null);
        check(error);
    }

    private void releaseMemory() {
        int error;
        for(long mem: memObjects) {
            error = CL22.clReleaseMemObject(mem);
            check(error);
        }
    }

    private void allocateMemory(Cluster<ValueMandelbrot> c) {
        double[] start = currentDataSet.
                levelGetStartCoordinatesOfCluster(depth, c.getId());
        int[] error = new int[1];
        long coordinates, result, maxIterations, logicClusterDimensions, precision;

        coordinates = CL22.clCreateBuffer(context, CL22.CL_MEM_READ_ONLY | CL22.CL_MEM_COPY_HOST_PTR, start, error);
        check(error);

        IntBuffer errorBuffer = BufferUtils.createIntBuffer(1);
        result = CL22.clCreateBuffer(context, CL22.CL_MEM_WRITE_ONLY,
                Integer.BYTES * logicClusterHeight * logicClusterWidth, errorBuffer);
        check(errorBuffer);

        maxIterations = CL22.clCreateBuffer(context, CL22.CL_MEM_READ_ONLY | CL22.CL_MEM_COPY_HOST_PTR,
                new int[]{this.maxIterations}, error);
        check(error);

        logicClusterDimensions = CL22.clCreateBuffer(context, CL22.CL_MEM_READ_ONLY | CL22.CL_MEM_COPY_HOST_PTR, new int[]{
                logicClusterWidth, logicClusterHeight
        }, error);
        check(error);

        precision = CL22.clCreateBuffer(context, CL22.CL_MEM_READ_ONLY | CL22.CL_MEM_COPY_HOST_PTR, new double[]{
                this.precision
        }, error);
        check(error);

        error[0] = CL22.clSetKernelArg1p(kernel, 0, coordinates);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 1, result);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 2, maxIterations);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 3, logicClusterDimensions);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 4, precision);
        assert error[0] == CL22.CL_SUCCESS: error[0];
        memObjects = new long[]{coordinates, result, maxIterations, logicClusterDimensions, precision};
    }


    @Override
    public void awaitTermination(long maxWaitingTime) {
        try {
            thread.join(maxWaitingTime);
            clusters.clear();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void createCommandQueue() {
        IntBuffer error = BufferUtils.createIntBuffer(1);
        commandQueue = CL22.clCreateCommandQueue(context, device, 0L, error);
        assert error.get(0) == CL22.CL_SUCCESS: error.get(0);
    }

    private void createContext() {
        PointerBuffer pDevice = PointerBuffer.allocateDirect(1);
        pDevice.put(device);
        IntBuffer error = BufferUtils.createIntBuffer(1);
        //TODO: No idea how to use properties, but must be present for program
        ByteBuffer properties = BufferUtils.createByteBuffer(Long.BYTES);
        BufferUtils.zeroBuffer(properties);
        context = CL22.clCreateContext(PointerBuffer.create(properties), device, null, 0, error);
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

    static void printBuildError(long program, long device) {
        ByteBuffer b = BufferUtils.createByteBuffer(Long.BYTES);
        int error;
        error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, (IntBuffer)null,
                PointerBuffer.create(b));
        assert error == CL22.CL_SUCCESS: error;
        int length = b.asIntBuffer().get();

        ByteBuffer message = BufferUtils.createByteBuffer(length);
        error = CL22.clGetProgramBuildInfo(program, device, CL22.CL_PROGRAM_BUILD_LOG, message,null);
        assert error == CL22.CL_SUCCESS: error;

        while(message.hasRemaining()) {
            System.out.print((char)message.get());
        }
    }

    static String readKernel(String f) {
        StringBuilder builder = new StringBuilder();
        System.out.println(f);
        BufferedReader r = new BufferedReader(new InputStreamReader(
                FileManager.getFileManager().getResourceAsStream(f)));

        try {
            String line;
            while((line = r.readLine()) != null) {
                builder.append(line).append("\n");
            }
            r.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return builder.toString();
    }

    public static ArrayList<Long> getAllAvailableDevices() {
        ArrayList<Long> ret = new ArrayList<>();
        IntBuffer amount = BufferUtils.createIntBuffer(1);
        int error = CL22.clGetPlatformIDs(null, amount);
        check(error);

        PointerBuffer platforms = PointerBuffer.allocateDirect(amount.get());
        CL22.clGetPlatformIDs(platforms, (IntBuffer) null);
        check(error);

        while(platforms.hasRemaining()) {
            ret.addAll(fromPlatform(platforms.get()));
        }

        return ret;
    }

    private static ArrayList<Long> fromPlatform(long platform) {
        ArrayList<Long> ret = new ArrayList<>();

        int error;
        int[] amount = new int[]{-1};
        error = CL22.clGetDeviceIDs(platform, CL22.CL_DEVICE_TYPE_ALL, null, amount);
        check(error);

        PointerBuffer devices = PointerBuffer.allocateDirect(amount[0]);
        error = CL22.clGetDeviceIDs(platform, CL22.CL_DEVICE_TYPE_ALL, devices, (int[])null);
        check(error);

        while(devices.hasRemaining()) {
            ret.add(devices.get());
        }

        return ret;
    }
}
