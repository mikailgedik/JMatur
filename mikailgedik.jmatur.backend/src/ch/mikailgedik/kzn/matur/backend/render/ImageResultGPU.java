package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.MemMan;
import ch.mikailgedik.kzn.matur.backend.data.Region;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.PointerBuffer;
import org.lwjgl.opencl.CL22;

import java.nio.ByteBuffer;

public class ImageResultGPU extends ImageResult {
    private final CLDevice device;
    private final long kernel;

    private long pImage, pDestinationOffset, pLogClusW, pLogImgW, pMaxIter;

    public ImageResultGPU(int pixelWidth, int pixelHeight, Region region, DataSet dataSet, CLDevice device, long kernel) {
        super(pixelWidth, pixelHeight, region, dataSet);
        this.device = device;
        this.kernel = kernel;
    }

    public void create(long maxWaitingTime) {
        createScreen();

        pImage = MemMan.allocateAsWriteMemory(device, getScreen().getPixels().length * Integer.SIZE);
        this.pLogClusW = MemMan.allocateAsReadMemory(device, new int[]{getDataSet().getLogicClusterWidth()});
        this.pLogImgW = MemMan.allocateAsReadMemory(device, new int[]{getScreen().getWidth()});
        this.pMaxIter = MemMan.allocateAsReadMemory(device, new int[]{getMaxIterations()});
        //Set Param values
        {
            int[] error = new int[1];
            error[0] = CL22.clSetKernelArg1p(kernel, 1, pImage);
            assert error[0] == CL22.CL_SUCCESS: error[0];

            error[0] = CL22.clSetKernelArg1p(kernel, 3, pLogClusW);
            assert error[0] == CL22.CL_SUCCESS: error[0];

            error[0] = CL22.clSetKernelArg1p(kernel, 4, pLogImgW);
            assert error[0] == CL22.CL_SUCCESS: error[0];

            error[0] = CL22.clSetKernelArg1p(kernel, 5, pMaxIter);
            assert error[0] == CL22.CL_SUCCESS: error[0];
        }

        getDataSet().iterateOverLogicalRegion(getLogicalRegion(), this);

        MemMan.copyToArray(device, pImage, getScreen().getPixels());

        MemMan.freeMemoryObject(pImage);
        MemMan.freeMemoryObject(pLogClusW);
        MemMan.freeMemoryObject(pLogImgW);
        MemMan.freeMemoryObject(pMaxIter);
    }

    private void runKernel() {
        ByteBuffer global_work_size = BufferUtils.createByteBuffer(Long.BYTES);
        ByteBuffer local_work_size = BufferUtils.createByteBuffer(Long.BYTES);

        global_work_size.asLongBuffer().put(getDataSet().getLogicClusterWidth() * getDataSet().getLogicClusterHeight());
        local_work_size.asLongBuffer().put(1);

        int error = CL22.clEnqueueNDRangeKernel(device.getCommandQueue(), kernel, 1,
                null,
                PointerBuffer.create(global_work_size),
                PointerBuffer.create(local_work_size),
                null,
                null);
        OpenCLHelper.check(error);
    }

    private void makeParam(Cluster c, int[] destinationOffset) {
        this.pDestinationOffset = MemMan.allocateAsReadMemory(device, destinationOffset);

        int[] error = new int[1];
        error[0] = CL22.clSetKernelArg1p(kernel, 0, c.getGPUAddress());
        assert error[0] == CL22.CL_SUCCESS: error[0];

        error[0] = CL22.clSetKernelArg1p(kernel, 2, pDestinationOffset);
        assert error[0] == CL22.CL_SUCCESS: error[0];
    }

    private void releaseParam() {
        MemMan.freeMemoryObject(pDestinationOffset);
    }

    @Override
    public void accept(Cluster c, int clusterX, int clusterY) {
        MemMan.ensureInGPU(device, c, getDataSet().getLogicClusterHeight() * getDataSet().getLogicClusterWidth());
        int xOffset = getDataSet().getLogicClusterWidth()* (clusterX - getLogicalRegion().getStartX()),
                yOffset = getDataSet().getLogicClusterHeight()*(clusterY - getLogicalRegion().getStartY());


        makeParam(c, new int[]{xOffset, yOffset});
        runKernel(); //TODO free pDestinationOffset
        releaseParam();
    }
}
