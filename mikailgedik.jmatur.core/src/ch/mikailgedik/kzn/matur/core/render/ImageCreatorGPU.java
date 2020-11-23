package ch.mikailgedik.kzn.matur.core.render;

import ch.mikailgedik.kzn.matur.core.connector.Screen;
import ch.mikailgedik.kzn.matur.core.data.DataSet;
import ch.mikailgedik.kzn.matur.core.data.LogicalRegion;
import ch.mikailgedik.kzn.matur.core.data.Region;
import ch.mikailgedik.kzn.matur.core.opencl.CLDevice;
import ch.mikailgedik.kzn.matur.core.opencl.OpenCLHelper;

import java.util.ArrayList;

public class ImageCreatorGPU extends ImageCreator{
    private static final String KERNEL_NAME = "colorKernel";
    private final DataSet dataSet;
    private ImageResultGPU imageResult;
    private final ArrayList<ImageResultGPU> buffer;
    private CLDevice device;
    private final long program, kernel;

    public ImageCreatorGPU(DataSet dataSet, CLDevice device, String colorFunctionSource) {
        this.dataSet = dataSet;
        buffer = new ArrayList<>();

        this.device = device;
        this.program = OpenCLHelper.createProgramFromString(device.getContext(),
                colorFunctionSource);
        OpenCLHelper.buildProgram(device.getDevice(), program);
        this.kernel = OpenCLHelper.createKernel(program, KERNEL_NAME);
    }

    /** Does not scale the images down to minPixelWidth and minPixelHeight, but guarantees that the returned Screen has always bigger dimensions*/
    public Screen createScreen(int minPixelWidth, int minPixelHeight, Region region, long maxWaitingTime) {
        //TODO buffer image results to avoid creating same images over and over
        //Only cropping has to be done anew

        imageResult = new ImageResultGPU(minPixelWidth, minPixelHeight, region, dataSet, device, kernel);
        boolean create = true;
        LogicalRegion tr;
        for(ImageResultGPU i: buffer) {
            tr = i.getLogicalRegion();
            if(tr.getStartX() <= imageResult.getLogicalRegion().getStartX() &&
                    tr.getStartY() <= imageResult.getLogicalRegion().getStartY() &&
                    tr.getEndX() >= imageResult.getLogicalRegion().getEndX() &&
                    tr.getEndY() >= imageResult.getLogicalRegion().getEndY() &&
                tr.getDepth() == imageResult.getLogicalRegion().getDepth()) {
                create = false;
                imageResult = i;
                break;
            }
        }

        if(create) {
            imageResult.create(maxWaitingTime);
            buffer.add(imageResult);
            //TODO empty buffer if too full
        }

        return getCutVersion(region);
    }

    private Screen getCutVersion(Region region) {
        Screen result = imageResult.getScreen();
        Region actualRegion = imageResult.getActualRegion();

        Screen cutVersion;

        double sx = ((region.getStartX() - actualRegion.getStartX()) / actualRegion.getWidth() * result.getWidth()),
                sy = ((region.getStartY() - actualRegion.getStartY()) / actualRegion.getHeight() * result.getHeight()),
                ex = ((region.getEndX() - actualRegion.getStartX()) / actualRegion.getWidth() * result.getWidth()) -1,
                ey = ((region.getEndY() - actualRegion.getStartY()) / actualRegion.getHeight() * result.getHeight())-1;

        //Assert no upscaling...
        assert sx >= 0;
        assert sy >= 0;
        assert ex < result.getWidth();
        assert ey < result.getHeight();

        cutVersion = result.subScreen((int)sx, (int)sy,(int)(ex -sx), (int)(ey -sy));

        int[] row = new int[cutVersion.getWidth()];
        for(int y = 0; y < cutVersion.getHeight()/2; y++) {
            System.arraycopy(cutVersion.getPixels(), y * cutVersion.getWidth(), row, 0, cutVersion.getWidth());
            System.arraycopy(cutVersion.getPixels(), (cutVersion.getHeight() - 1 - y) * cutVersion.getWidth(), cutVersion.getPixels(),y * cutVersion.getWidth(), cutVersion.getWidth());
            System.arraycopy(row, 0, cutVersion.getPixels(),(cutVersion.getHeight() - 1 - y) * cutVersion.getWidth(), cutVersion.getWidth());
        }

        return cutVersion;
    }

    public void destroy() {
        //TODO delete VRAM usage
    }
}
