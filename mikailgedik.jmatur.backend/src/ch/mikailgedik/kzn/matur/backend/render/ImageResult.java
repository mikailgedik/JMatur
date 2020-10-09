package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.OldCalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.Result;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;

import java.util.function.Consumer;
import java.util.function.Function;

public class ImageResult<T extends Result> {
    private final int depth, clustersX, clustersY, startXCluster, startYCluster;
    private final Screen screen, source;
    private final OldCalculationResult<T> calculationResult;
    private final double startX, startY, width, height;
    private final Function<Integer, Integer> colorFunction;
    private boolean complete;


    public ImageResult(int startXCluster, int startYCluster, int clustersX, int clustersY, int depth, OldCalculationResult<T> calculationResult, Function<Integer, Integer> colorFunction) {
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
        this.complete = false;
    }

    public boolean populate() {
        boolean complete = true;
        OldCalculationResult.Level<T> l = calculationResult.getLevel(depth);
        int cX, cY;
        for(int x = 0; x < clustersX; x++) {
            cX = (x + startXCluster);
            if(cX < 0) {
                continue;
            }
            if(cX >= l.getSize()) {
                break;
            }

            for(int y = 0; y < clustersY; y++) {
                cY = (y + startYCluster);
                if(cY < 0) {
                    continue;
                }
                if(cY >= l.getSize()) {
                    break;
                }
                if(source.getPixel(x, y) == this.depth) {
                    continue;
                }
                T[] cluster = l.getAt(cX + l.getSize() * cY);
                if(cluster[0] == null) {
                    //Cluster not yet calculated
                    int tmpDepth = depth, tmpcX = cX, tmpcY = cY;
                    int valuePosX, valuePosY;
                    OldCalculationResult.Level<T> tmpL;
                    do {
                        tmpDepth--;
                        tmpL = calculationResult.getLevel(tmpDepth);

                        valuePosX = tmpcX % calculationResult.getTiles();
                        valuePosY = tmpcY % calculationResult.getTiles();

                        tmpcX /= calculationResult.getTiles();
                        tmpcY /= calculationResult.getTiles();
                        cluster = tmpL.getAt(tmpcX + tmpL.getSize() * tmpcY);
                    } while(cluster[0] == null);

                    int index = valuePosX + valuePosY * calculationResult.getTiles();
                    screen.fillRect(x * calculationResult.getTiles(), y * calculationResult.getTiles(),
                            calculationResult.getTiles(), calculationResult.getTiles(), colorFunction.apply(
                                    cluster[index].getValue()));
                    source.setPixel(x, y, tmpDepth);
                    complete = false;
                } else {
                    this.drawCluster(cluster, x * calculationResult.getTiles(), y * calculationResult.getTiles());
                    source.setPixel(x, y, depth);
                }
            }
        }
        if(complete) {
            this.complete = true;
        }
        return complete;
    }

    private void drawCluster(T[] t, int startX, int startY) {
        for(int x = 0; x < calculationResult.getTiles(); x++) {
            for(int y = 0; y < calculationResult.getTiles(); y++) {
                screen.setPixel(x + startX, y + startY, colorFunction.apply(t[x + y * calculationResult.getTiles()].getValue()));
            }
        }
    }

    public boolean isComplete() {
        return complete;
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

    public OldCalculationResult<T> getCalculationResult() {
        return calculationResult;
    }

    public void forEachIncomplete(Consumer<Object> f) {
        for(int y = 0; y < source.getHeight(); y++) {
            for(int x = 0; x < source.getWidth(); x++) {
                if(source.getPixel(x,y) != depth) {
                    f.accept(new int[]{depth, x + startXCluster, y + startYCluster});
                }
            }
        }
    }
}
