package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.*;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;

import java.awt.*;

public class ImageCreator<T extends Value> extends DataAcceptor<T> {
    private ColorFunction<T> colorFunction;
    private DataSet<T> dataSet;
    private double minPrecision;
    private LogicalRegion logicalRegion;
    private Screen result;

    public ImageCreator(DataSet<T> dataSet, ColorFunction<T> colorFunction) {
        this.colorFunction = colorFunction;
        this.dataSet = dataSet;
    }

    public Screen createScreen(int pixelWidth, int pixelHeight, Region region) {
        prepare(pixelWidth, pixelHeight, region);
        System.out.println(logicalRegion.getDepth());
        dataSet.iterateOverRegion(region, minPrecision, this);

        return result;
    }

    private void prepare(int pixelWidth, int pixelHeight, Region region) {
        minPrecision = Math.min(region.getWidth() / pixelWidth, region.getHeight() / pixelHeight);
        logicalRegion = dataSet.dataGetLogicalRegion(region, minPrecision);

        result = new Screen(logicalRegion.getWidth() * dataSet.getLogicClusterWidth(),
                logicalRegion.getHeight() * dataSet.getLogicClusterHeight(), 0xff00ff);
    }

    @Override
    public void accept(Cluster<T> t, int clusterX, int clusterY) {
        int xOffset = dataSet.getLogicClusterWidth()* (clusterX - logicalRegion.getStartX()),
                yOffset = dataSet.getLogicClusterHeight()*(clusterY - logicalRegion.getStartY());

        for(int y = 0; y < dataSet.getLogicClusterHeight(); y++) {
            for(int x = 0; x < dataSet.getLogicClusterWidth(); x++) {
                result.setPixel(x + xOffset, y + yOffset, colorFunction.colorOf(t.getValue()[x + y * dataSet.getLogicClusterWidth()]));
            }
        }
    }

    public static final ColorFunction<ValueMandelbrot> MANDELBROT_COLOR_FUNCTION =
            (v) -> v.getValue() == -1 ? 0xffffff : 0x0;
}
