package ch.mikailgedik.kzn.matur.core.render;

import ch.mikailgedik.kzn.matur.core.connector.Screen;
import ch.mikailgedik.kzn.matur.core.data.DataAcceptor;
import ch.mikailgedik.kzn.matur.core.data.DataSet;
import ch.mikailgedik.kzn.matur.core.data.LogicalRegion;
import ch.mikailgedik.kzn.matur.core.data.Region;

public abstract class ImageResult extends DataAcceptor {
    private final DataSet dataSet;
    private final LogicalRegion logicalRegion;
    private final Region actualRegion;
    private final int maxIterations;
    private Screen screen;

    public ImageResult(int pixelWidth, int pixelHeight, Region region, DataSet dataSet) {
        this.dataSet = dataSet;

        double minimalPrecision = Math.min(region.getWidth() / pixelWidth, region.getHeight() / pixelHeight);
        logicalRegion = dataSet.dataGetUnrestrictedLogicalRegion(region, minimalPrecision);

        actualRegion = dataSet.dataGetRegion(logicalRegion);
        maxIterations = dataSet.levelGetIterationsForDepth(logicalRegion.getDepth());
    }

    public abstract void create(long maxWaitingTime);

    public DataSet getDataSet() {
        return dataSet;
    }

    public LogicalRegion getLogicalRegion() {
        return logicalRegion;
    }

    public Region getActualRegion() {
        return actualRegion;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public Screen getScreen() {
        return screen;
    }

    public void setScreen(Screen screen) {
        this.screen = screen;
    }

    public void createScreen() {
        setScreen(new Screen((getLogicalRegion().getWidth()) * getDataSet().getLogicClusterWidth(),
                (getLogicalRegion().getHeight()) * getDataSet().getLogicClusterHeight(), 0xff00ff));
    }
}