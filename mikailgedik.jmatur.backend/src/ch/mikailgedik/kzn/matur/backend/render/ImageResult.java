package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.*;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;

import java.util.Arrays;

public class ImageResult<T extends Value> extends DataAcceptor<T> {
    private ColorFunction<T> colorFunction;
    private DataSet<T> dataSet;
    private double minimalPrecision;
    private LogicalRegion logicalRegion;
    private Screen result;
    private Region actualRegion;
    private int maxIterations;

    public ImageResult(int pixelWidth, int pixelHeight, Region region, ColorFunction<T> colorFunction, DataSet<T> dataSet) {
        this.colorFunction = colorFunction;
        this.dataSet = dataSet;

        prepare(pixelWidth, pixelHeight, region);
    }

    public void create() {
        result = new Screen((logicalRegion.getWidth()) * dataSet.getLogicClusterWidth(),
                (logicalRegion.getHeight()) * dataSet.getLogicClusterHeight(), 0xff00ff);
        dataSet.iterateOverLogicalRegion(logicalRegion, this);
    }

    private void prepare(int pixelWidth, int pixelHeight, Region region) {
        minimalPrecision = Math.min(region.getWidth() / pixelWidth, region.getHeight() / pixelHeight);
        logicalRegion = dataSet.dataGetUnrestrictedLogicalRegion(region, minimalPrecision);

        actualRegion = dataSet.dataGetRegion(logicalRegion);
        maxIterations = dataSet.levelGetIterationsForDepth(logicalRegion.getDepth());
    }

    @Override
    public void accept(Cluster<T> t, int clusterX, int clusterY) {
        int xOffset = dataSet.getLogicClusterWidth()* (clusterX - logicalRegion.getStartX()),
                yOffset = dataSet.getLogicClusterHeight()*(clusterY - logicalRegion.getStartY());

        for(int y = 0; y < dataSet.getLogicClusterHeight(); y++) {
            for(int x = 0; x < dataSet.getLogicClusterWidth(); x++) {
                result.setPixel(x + xOffset, y + yOffset,
                        colorFunction.colorOf(t.getValue()[x + y * dataSet.getLogicClusterWidth()], maxIterations));
            }
        }
    }

    public Screen getResult() {
        return result;
    }

    public Region getActualRegion() {
        return actualRegion;
    }
}
