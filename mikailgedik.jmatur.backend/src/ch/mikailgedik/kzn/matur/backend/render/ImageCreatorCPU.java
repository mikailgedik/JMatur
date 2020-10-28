package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.*;

import java.util.ArrayList;

public class ImageCreatorCPU extends ImageCreator {
    private ColorFunction colorFunction;
    private DataSet dataSet;
    private ImageResultCPU imageResult;
    private ArrayList<ImageResultCPU> buffer;

    public ImageCreatorCPU(DataSet dataSet, ColorFunction colorFunction) {
        this.colorFunction = colorFunction;
        this.dataSet = dataSet;
        buffer = new ArrayList<>();
    }

    /** Does not scale the images down to minPixelWidth and minPixelHeight, but guarantees that the returned Screen has always bigger dimensions*/
    public Screen createScreen(int minPixelWidth, int minPixelHeight, Region region, int threads, long maxWaitingTime) {
        //TODO buffer image results to avoid creating same images over and over
        //Only cropping has to be done anew

        imageResult = new ImageResultCPU(minPixelWidth, minPixelHeight, region, colorFunction, dataSet);
        boolean create = true;
        LogicalRegion tr;
        for(ImageResultCPU i: buffer) {
            tr = i.getLogicalRegion();
            if(tr.getStartX() <= imageResult.getLogicalRegion().getStartX() &&
                    tr.getStartY() <= imageResult.getLogicalRegion().getStartY() &&
                    tr.getEndX() >= imageResult.getLogicalRegion().getEndX() &&
                    tr.getEndY() >= imageResult.getLogicalRegion().getEndY()) {
                create = false;
                imageResult = i;
                break;
            }
        }

        if(create) {
            imageResult.create(threads, maxWaitingTime);
            buffer.add(imageResult);
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
}
