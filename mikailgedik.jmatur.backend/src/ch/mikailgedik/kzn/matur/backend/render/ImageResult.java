package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.DataAcceptor;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.LogicalRegion;
import ch.mikailgedik.kzn.matur.backend.data.Region;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;

public abstract class ImageResult<T extends Value> extends DataAcceptor<T> {
    private final DataSet<T> dataSet;
    private final LogicalRegion logicalRegion;
    private final Region actualRegion;
    private final int maxIterations;
    private Screen screen;

    public ImageResult(int pixelWidth, int pixelHeight, Region region, DataSet<T> dataSet) {
        this.dataSet = dataSet;

        double minimalPrecision = Math.min(region.getWidth() / pixelWidth, region.getHeight() / pixelHeight);
        logicalRegion = dataSet.dataGetUnrestrictedLogicalRegion(region, minimalPrecision);

        actualRegion = dataSet.dataGetRegion(logicalRegion);
        maxIterations = dataSet.levelGetIterationsForDepth(logicalRegion.getDepth());
    }

    public abstract void create(int threads, long maxWaitingTime);

    public DataSet<T> getDataSet() {
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
