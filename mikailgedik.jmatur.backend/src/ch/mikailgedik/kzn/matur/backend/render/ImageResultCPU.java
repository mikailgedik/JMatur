package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.*;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ImageResultCPU<T extends Value> extends ImageResult<T> {
    private final ColorFunction<T> colorFunction;
    private ExecutorService executorService;

    public ImageResultCPU(int pixelWidth, int pixelHeight, Region region, ColorFunction<T> colorFunction, DataSet<T> dataSet) {
        super(pixelWidth, pixelHeight, region, dataSet);
        this.colorFunction = colorFunction;
    }

    public void create(int threads, long maxWaitingTime) {
        createScreen();
        executorService = Executors.newFixedThreadPool(threads);
        getDataSet().iterateOverLogicalRegion(getLogicalRegion(), this);
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
            int xOffset = getDataSet().getLogicClusterWidth()* (clusterX - getLogicalRegion().getStartX()),
                    yOffset = getDataSet().getLogicClusterHeight()*(clusterY - getLogicalRegion().getStartY());

            for(int y = 0; y < getDataSet().getLogicClusterHeight(); y++) {
                for(int x = 0; x < getDataSet().getLogicClusterWidth(); x++) {
                    getScreen().setPixel(x + xOffset, y + yOffset,
                            colorFunction.colorOf(t.getValue()[x + y * getDataSet().getLogicClusterWidth()], getMaxIterations()));
                }
            }
        });
    }
}