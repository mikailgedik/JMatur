package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Constants;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.*;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;

import java.awt.*;

public class ImageCreator<T extends Value> {
    private ColorFunction<T> colorFunction;
    private DataSet<T> dataSet;
    private ImageResult<T> imageResult;

    public ImageCreator(DataSet<T> dataSet, ColorFunction<T> colorFunction) {
        this.colorFunction = colorFunction;
        this.dataSet = dataSet;
    }

    public Screen createScreen(int minPixelWidth, int minPixelHeight, Region region) {
        //TODO buffer image results to avoid creating same images over and over
        //Only cropping has to be done anew
        imageResult = new ImageResult<>(minPixelWidth, minPixelHeight, region, colorFunction, dataSet);
        imageResult.create();

        Screen result = imageResult.getResult();
        Region actualRegion = imageResult.getActualRegion();

        Screen cutVersion;

        double sx = ((region.getStartX() - actualRegion.getStartX()) / actualRegion.getWidth() * result.getWidth()),
                sy = ((region.getStartY() - actualRegion.getStartY()) / actualRegion.getHeight() * result.getHeight()),
                ex = ((region.getEndX() - actualRegion.getStartX()) / actualRegion.getWidth() * result.getWidth()) -1,
                ey = ((region.getEndY() - actualRegion.getStartY()) / actualRegion.getHeight() * result.getHeight())-1;

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

        assert cutVersion.getWidth() >= minPixelWidth;
        assert cutVersion.getHeight() >= minPixelHeight;
        return cutVersion;
    }
}
