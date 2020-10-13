package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.*;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageResult<T extends Value> extends DataAcceptor<T> {
    private final ColorFunction<T> colorFunction;
    private final DataSet<T> dataSet;
    private final LogicalRegion logicalRegion;
    private final Region actualRegion;
    private final int maxIterations;
    private Screen result;
    private ExecutorService executorService;

    public ImageResult(int pixelWidth, int pixelHeight, Region region, ColorFunction<T> colorFunction, DataSet<T> dataSet) {
        this.colorFunction = colorFunction;
        this.dataSet = dataSet;

        double minimalPrecision = Math.min(region.getWidth() / pixelWidth, region.getHeight() / pixelHeight);
        logicalRegion = dataSet.dataGetUnrestrictedLogicalRegion(region, minimalPrecision);

        actualRegion = dataSet.dataGetRegion(logicalRegion);
        maxIterations = dataSet.levelGetIterationsForDepth(logicalRegion.getDepth());
    }

    public void create(int threads, long maxWaitingTime) {
        result = new Screen((logicalRegion.getWidth()) * dataSet.getLogicClusterWidth(),
                (logicalRegion.getHeight()) * dataSet.getLogicClusterHeight(), 0xff00ff);
        executorService = Executors.newFixedThreadPool(threads);
        dataSet.iterateOverLogicalRegion(logicalRegion, this);
        executorService.shutdown();
        try {
            executorService.awaitTermination(maxWaitingTime, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            //TODO correct behaviour when InterruptedException is thrown?
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public void accept(Cluster<T> t, int clusterX, int clusterY) {

        executorService.submit(() -> {
            int xOffset = dataSet.getLogicClusterWidth()* (clusterX - logicalRegion.getStartX()),
                    yOffset = dataSet.getLogicClusterHeight()*(clusterY - logicalRegion.getStartY());

            for(int y = 0; y < dataSet.getLogicClusterHeight(); y++) {
                for(int x = 0; x < dataSet.getLogicClusterWidth(); x++) {
                    result.setPixel(x + xOffset, y + yOffset,
                            colorFunction.colorOf(t.getValue()[x + y * dataSet.getLogicClusterWidth()], maxIterations));
                }
            }
        });
    }

    public Screen getResult() {
        return result;
    }

    public Region getActualRegion() {
        return actualRegion;
    }

    public LogicalRegion getLogicalRegion() {
        return logicalRegion;
    }
}
