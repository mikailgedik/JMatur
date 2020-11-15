package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.connector.CalculatorUnit;
import ch.mikailgedik.kzn.matur.backend.data.MemMan;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL22;
import org.lwjgl.opencl.CLEventCallback;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.concurrent.atomic.AtomicBoolean;

//TODO use OpenCLHelper
//TODO decide when memory has to be released. The result is stored in VRAM addressable by Cluster.getGPUAddress()
public class CalculatorUnitGPU implements CalculatorUnit {
    private static final String KERNEL_NAME="fractal";
    private String kernelSource;
    private final CLDevice device;
    private long program, kernel;
    /** Prefix p stands for pointer, these are addresses for memory on the GPU*/
    private long pData, pMaxIterations, pClusterDimensions, pPrecision, pCoordinates, pAbort;
    private int logicClusterWidth, logicClusterHeight;
    private final AtomicBoolean abortable;
    private Thread thread;
    private volatile Calculable calculable;
    private String description;

    public CalculatorUnitGPU(long device) {
        this.device = new CLDevice(device);
        abortable = new AtomicBoolean();

        description = OpenCLHelper.queryDeviceInfo(getDevice().getDevice(), CL22.CL_DEVICE_TYPE) + ": " +
                OpenCLHelper.queryDeviceInfo(getDevice().getDevice(), CL22.CL_DEVICE_NAME) + ", " +
                OpenCLHelper.queryDeviceInfo(getDevice().getDevice(), CL22.CL_DEVICE_MAX_CLOCK_FREQUENCY) + "MHz";
    }

    public void init(Init init) {
        this.kernelSource = init.getClKernelSource();
        createContext();
        createCommandQueue();
        createProgram();
        buildProgram();
        createKernel();

        System.out.println("Device info: ");
        System.out.println("\tBuilt in kernels  : " +
                OpenCLHelper.queryDeviceInfo(device.getDevice(), CL22.CL_DEVICE_BUILT_IN_KERNELS));
        System.out.println("\tDevice name       : " +
                OpenCLHelper.queryDeviceInfo(device.getDevice(), CL22.CL_DEVICE_NAME));
        System.out.println("\tGlobal memory     : " +
                OpenCLHelper.queryDeviceInfo(device.getDevice(), CL22.CL_DEVICE_GLOBAL_MEM_CACHE_SIZE));
        System.out.println("\tCompute Units     : " +
                OpenCLHelper.queryDeviceInfo(device.getDevice(), CL22.CL_DEVICE_MAX_COMPUTE_UNITS));
        System.out.println("\tClock             : " +
                OpenCLHelper.queryDeviceInfo(device.getDevice(), CL22.CL_DEVICE_MAX_CLOCK_FREQUENCY));
    }

    private void buildProgram() {
        int error = CL22.clBuildProgram(program, null,
                "", null, 0);

        if(error != CL22.CL_SUCCESS) {
            OpenCLHelper.printBuildError(program, device.getDevice());
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

        program = CL22.clCreateProgramWithSource(device.getContext(), kernelSource, error);
        //https://www.codeproject.com/articles/86551/part-1-programming-your-graphics-card-gpu-with-jav
        OpenCLHelper.check(error);
    }

    public void delete() {
        //TODO release context?
    }

    @Override
    public synchronized void configureAndStart(CalculatorConfiguration configuration) {
        this.logicClusterWidth = configuration.getLogicClusterWidth();
        this.logicClusterHeight = configuration.getLogicClusterHeight();
        CalculatorMandelbrot calculatorMandelbrot = configuration.getCalculatorMandelbrot();

        this.thread = new Thread(() -> {
            setUnchangedParams();
            calculable = calculatorMandelbrot.get();
            while(calculable != null) {
                abortable.set(true);
                allocateMemory(calculable);
                runKernel();

                CL22.clFinish(device.getCommandQueue());
                releaseMemory();
                abortable.set(false);
                boolean accepted = calculatorMandelbrot.accept(calculable, device, pData);
                if(!accepted) {
                    //If not accepted, it has been aborted
                    MemMan.freeMemoryObject(pData);
                    pData = 0;
                }

                calculable = calculatorMandelbrot.get();
            }

            releaseUnchangedParams();
        });
        this.thread.setName(""+this.device.getContext());
        this.thread.start();
    }

    @Override
    public synchronized void abort(int calcId) {
        if(abortable.get() && calculable != null && calculable.getCalculatorId() == calcId) {
            abortable.set(false);
            IntBuffer berr = BufferUtils.createIntBuffer(1);
            long abortQueue = (CL22.clCreateCommandQueue(device.getContext(), device.getDevice(), 0L, berr));
            assert berr.get(0) == CL22.CL_SUCCESS: berr.get(0);

            int error = CL22.clEnqueueWriteBuffer(abortQueue, pAbort, true, 0, new int[]{1}, null,  null);
            OpenCLHelper.check(error);

            CL22.clReleaseCommandQueue(abortQueue);
        }
    }

    private void runKernel() {
        ByteBuffer global_work_size = BufferUtils.createByteBuffer(Long.BYTES);

        global_work_size.asLongBuffer().put(logicClusterHeight * logicClusterWidth);

        int error = CL22.clEnqueueNDRangeKernel(device.getCommandQueue(), kernel, 1,
                null,
                PointerBuffer.create(global_work_size),
                null,
                null,
                null);
        OpenCLHelper.check(error);
    }

    private void releaseMemory() {
        MemMan.freeMemoryObject(pCoordinates);
        MemMan.freeMemoryObject(pMaxIterations);
        MemMan.freeMemoryObject(pPrecision);

        pPrecision = 0;
        pMaxIterations = 0;
        pCoordinates = 0;
    }

    private void setUnchangedParams() {
        pClusterDimensions = MemMan.allocateAsReadMemory(device, new int[]{logicClusterWidth, logicClusterHeight});
        pAbort = MemMan.allocateAsReadMemory(device, new int[]{0});

        int[] error = new int[1];

        error[0] = CL22.clSetKernelArg1p(kernel, 3, pClusterDimensions);
        assert error[0] == CL22.CL_SUCCESS: error[0];
    }

    private void releaseUnchangedParams() {
        MemMan.freeMemoryObject(pClusterDimensions);
        MemMan.freeMemoryObject(pAbort);

        pClusterDimensions = 0;
        pAbort = 0;
    }

    private void allocateMemory(Calculable c) {
        double[] start = new double[] {c.getStartX(), c.getStartY()};
        pCoordinates = MemMan.allocateAsReadMemory(device, start);
        pData = MemMan.allocateReadWriteMemory(device, Integer.BYTES * logicClusterWidth * logicClusterHeight);
        pMaxIterations = MemMan.allocateAsReadMemory(device, new int[]{c.getMaxIterations()});
        pPrecision = MemMan.allocateAsReadMemory(device, new double[]{c.getPrecision()});

        int[] error = new int[1];

        error[0] = CL22.clEnqueueWriteBuffer(device.getCommandQueue(), pAbort, true, 0, new int[]{0}, null, null);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 0, pCoordinates);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 1, pData);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 2, pMaxIterations);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 4, pPrecision);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 5, pAbort);
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg(kernel, 6, new int[]{logicClusterWidth * logicClusterHeight});
        assert error[0] == CL22.CL_SUCCESS: error[0];
    }

    @Override
    public void awaitTerminationAndCleanup(long maxWaitingTime) {
        try {
            thread.join(maxWaitingTime);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private void createCommandQueue() {
        IntBuffer error = BufferUtils.createIntBuffer(1);
        device.setCommandQueue(CL22.clCreateCommandQueue(device.getContext(), device.getDevice(), 0L, error));
        assert error.get(0) == CL22.CL_SUCCESS: error.get(0);
    }

    private void createContext() {
        PointerBuffer pDevice = PointerBuffer.allocateDirect(1);
        pDevice.put(device.getDevice());
        IntBuffer error = BufferUtils.createIntBuffer(1);
        //TODO: No idea how to use properties, but must be present for program
        ByteBuffer properties = BufferUtils.createByteBuffer(Long.BYTES * 3);
        LongBuffer tmp = properties.asLongBuffer();
        tmp.put(0, CL22.CL_CONTEXT_PLATFORM);
        tmp.put(1, OpenCLHelper.getDevicePlatform(device.getDevice()));
        tmp.put(2,0);
        device.setContext(CL22.clCreateContext(PointerBuffer.create(properties), device.getDevice(), null, 0, error));
        OpenCLHelper.check(error);
    }

    public CLDevice getDevice() {
        return device;
    }

    public long getContext() {
        return device.getContext();
    }

    public long getCommandQueue() {
        return device.getCommandQueue();
    }

    @Override
    public String toString() {
        return description;

    }
}
