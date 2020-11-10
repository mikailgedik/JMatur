package ch.mikailgedik.kzn.matur.backend.opencl;

import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL22;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.util.ArrayList;

public class OpenCLHelper {
    public static long createProgramFromString(long context, String kernelSource) {
        IntBuffer error = BufferUtils.createIntBuffer(1);

        long program = CL22.clCreateProgramWithSource(context, kernelSource, error);
        //https://www.codeproject.com/articles/86551/part-1-programming-your-graphics-card-gpu-with-jav
        OpenCLHelper.check(error);
        return program;
    }

    public static long createProgramFromFile(long context, String kernelFile) {
        return createProgramFromString(context, FileManager.getFileManager().readFile(kernelFile));
    }

    public static void buildProgram(long device, long program) {
        int error = CL22.clBuildProgram(program, null,
                "", null, 0);

        if(error != CL22.CL_SUCCESS) {
            OpenCLHelper.printBuildError(program, device);
            throw new RuntimeException("OpenCL error: " + error);
        }
    }

    public static long createKernel(long program, String kernelName) {
        IntBuffer berr = BufferUtils.createIntBuffer(1);
        long kernel = CL22.clCreateKernel(program, kernelName, berr);
        assert berr.get(0) == CL22.CL_SUCCESS: berr.get(0);
        return kernel;
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
            ret.addAll(getDevicesFromPlatform(platforms.get()));
        }

        return ret;
    }

    private static ArrayList<Long> getDevicesFromPlatform(long platform) {
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

    public static void printBuildError(long program, long device) {
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

    public static void check(int[] error) {
        check(error[0]);
    }
    public static void check(int error) {
        if(error != CL22.CL_SUCCESS) {
            throw new RuntimeException("OpenCL error: " + error);
        }
    }
    public static void check(IntBuffer error) {
        if(error.get(0) != CL22.CL_SUCCESS) {
            throw new RuntimeException("OpenCL error: " + error.get(0));
        }
    }

    public static String queryDeviceInfoString(long device, int info) {
        ByteBuffer value = queryDeviceInfo(device, info);

        StringBuilder builder = new StringBuilder(value.capacity());
        for(int i = 0; i < builder.capacity(); i++){
            builder.append((char)value.get());
        }

        return builder.toString();
    }

    public static long queryDeviceInfoNum(long device, int info) {
        ByteBuffer value = queryDeviceInfo(device, info);

        return switch (value.limit()) {
            case 1 -> value.get();
            case 2 -> value.asCharBuffer().get();
            case 4 -> value.asIntBuffer().get();
            case 8 -> value.asLongBuffer().get();

            default ->
                    throw new IllegalStateException("Unexpected value: " + value.limit());
        };
    }

    public static ByteBuffer queryDeviceInfo(long device, int info) {
        int error;
        PointerBuffer length = PointerBuffer.allocateDirect(1);
        error = CL22.clGetDeviceInfo(device, info, (long[])null, length);
        check(error);

        ByteBuffer value = ByteBuffer.allocateDirect((int)length.get());
        CL22.clGetDeviceInfo(device, info, value, null);
        check(error);

        return value;
    }

    public static long getDevicePlatform(long device) {
        int error;
        LongBuffer l = BufferUtils.createLongBuffer(1);

        error = CL22.clGetDeviceInfo(device , CL22.CL_DEVICE_PLATFORM, l, null);
        check(error);

        return l.get();
    }
}
