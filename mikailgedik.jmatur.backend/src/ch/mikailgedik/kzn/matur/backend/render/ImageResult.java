package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.Result;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;

import java.util.function.Function;

public class ImageResult<T extends Result> {
    private final int depth, clustersX, clustersY, startXCluster, startYCluster;
    private final Screen screen, source;
    private final CalculationResult<T> calculationResult;
    private final double startX, startY, width, height;
    private final Function<Integer, Integer> colorFunction;

    public ImageResult(int startXCluster, int startYCluster, int clustersX, int clustersY, int depth, CalculationResult<T> calculationResult, Function<Integer, Integer> colorFunction) {
        this.depth = depth;

        this.startXCluster = startXCluster;
        this.startYCluster = startYCluster;
        this.clustersX = clustersX;
        this.clustersY = clustersY;

        double[] start = calculationResult.centerCoordinates(depth, startXCluster, startYCluster, 0);
        this.startX = start[0];
        this.startY = start[1];

        this.width = clustersX * calculationResult.getWidth() / Math.pow(calculationResult.getTiles(), depth);
        this.height = clustersY * calculationResult.getHeight() / Math.pow(calculationResult.getTiles(), depth);

        this.calculationResult = calculationResult;
        this.colorFunction = colorFunction;

        this.source = new Screen(clustersX, clustersY, -1);

        this.screen = new Screen(clustersX * calculationResult.getTiles(), clustersY * calculationResult.getTiles());
    }

    public void drawCluster(T[] t, int depth, int startX, int startY) {
        if(depth < this.depth) {
            int recSize = calculationResult.getLevel(this.depth - depth).getSize();
            for(int x = 0; x < calculationResult.getTiles(); x++) {
                for(int y = 0; y < calculationResult.getTiles(); y++) {
                    screen.fillRect(x * recSize + startX, y * recSize + startY, recSize, recSize, colorFunction.apply(t[x + y * calculationResult.getTiles()].getValue()));
                }
            }
        } else {
            for(int x = 0; x < calculationResult.getTiles(); x++) {
                for(int y = 0; y < calculationResult.getTiles(); y++) {
                    screen.setPixel(x + startX, y + startY, colorFunction.apply(t[x + y * calculationResult.getTiles()].getValue()));
                }
            }
        }
    }

    public void setValidation(int x, int y, int status) {
        this.source.setPixel(x, y, status);
    }

    public int getValidation(int x, int y) {
        return this.source.getPixel(x, y);
    }

    public int getDepth() {
        return depth;
    }

    public int getClustersX() {
        return clustersX;
    }

    public int getClustersY() {
        return clustersY;
    }

    public int getStartXCluster() {
        return startXCluster;
    }

    public int getStartYCluster() {
        return startYCluster;
    }

    public Screen getScreen() {
        return screen;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    public CalculationResult<T> getCalculationResult() {
        return calculationResult;
    }
}
