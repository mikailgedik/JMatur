package ch.mikailgedik.kzn.matur.backend.data;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorMandelbrot;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;

import java.util.ArrayList;
import java.util.Iterator;


public abstract class DataSet<T extends Value> {
    private final ArrayList<Level<T>> levels;
    /** logic cluster width: amount of values
     * absolute cluster width: width in mathematical sense*/
    private final int logicClusterWidth, logicClusterHeight;
    private final int firstLevelLogicWidth, firstLevelLogicHeight;
    private final double firstLevelPrecision;
    private final int clusterFactor;
    private final Region region;

    /** The absolute height is calculated through the logic cluster dimensions and the start values for the logic level dimensions */

    public DataSet(int logicClusterWidth, int logicClusterHeight, int startLogicLevelWidth, int startLogicLevelHeight, int clusterFactor,
                   double regionStartX, double regionStartY, double regionWidth, int iterationsForFirstLevel) {
        this.levels = new ArrayList<>();
        this.logicClusterWidth = logicClusterWidth;
        this.logicClusterHeight = logicClusterHeight;
        this.firstLevelLogicWidth = startLogicLevelWidth;
        this.firstLevelLogicHeight = startLogicLevelHeight;
        this.clusterFactor = clusterFactor;

        //Create first level

        double absoluteClusterWidth = regionWidth / startLogicLevelWidth;
        double absoluteClusterHeight = absoluteClusterWidth * logicClusterHeight / logicClusterWidth;
        double absoluteLevelHeight = startLogicLevelHeight * absoluteClusterHeight;

        levels.add(new Level<>(
                0,
                startLogicLevelWidth,
                startLogicLevelHeight,
                regionWidth / (startLogicLevelWidth * logicClusterWidth),
                iterationsForFirstLevel
        ));

        this.firstLevelPrecision = levels.get(0).getPrecision();

        this.region = new Region(regionStartX, regionStartY,
                regionStartX + regionWidth, regionStartY + absoluteLevelHeight);

        //Only for testing purposes of the math done
        /*
        ensureLevelWithDepth(10);
        for(int i = 0; i < 11; i++) {
            System.out.println(i);

            System.out.println("W_abs_clu: " + (levelCalculateAbsoluteClusterWidthAtDepth(i) -  levelGetAbsoluteClusterWidthAtDepth(i)));
            System.out.println("H_abs_clu: " + (levelCalculateAbsoluteClusterHeightAtDepth(i) - levelGetAbsoluteClusterHeightAtDepth(i)));
            System.out.println("prec: " + (levelGetPrecisionAtDepth(i)- levelCalculatePrecisionAtDepth(i)));
            System.out.println("W_log_lev: " + (levelCalculateLogicLevelWidth(i) -  levelGetLogicLevelWidth(i)));
            System.out.println("H_log_lev: " + (levelCalculateLogicLevelHeight(i) - levelGetLogicLevelHeight(i)));

        }
         */
    }

    //These methods calculate the values independently without the aid of created levels
    //Use these methods to initialize levels or if it unknown if the level has been created
    public double levelCalculateAbsoluteClusterWidthAtDepth(int depth) {
        return region.getWidth() / (Math.pow(clusterFactor, depth) * firstLevelLogicWidth);
    }
    public double levelCalculateAbsoluteClusterHeightAtDepth(int depth) {
        return region.getHeight() / (Math.pow(clusterFactor, depth) * firstLevelLogicHeight);
    }
    /** Returns distance between two vertical or horizontal points */
    public double levelCalculatePrecisionAtDepth(int depth) {
        return firstLevelPrecision / Math.pow(clusterFactor, depth);
    }

    /** Returns the amount of clusters the level has at the specific depth*/
    public int levelCalculateLogicLevelWidth(int depth) {
        return (int) Math.round(Math.pow(clusterFactor, depth) * firstLevelLogicWidth);
    }
    public int levelCalculateLogicLevelHeight(int depth) {
        return (int) Math.round(Math.pow(clusterFactor, depth) * firstLevelLogicHeight);
    }

    public int levelCalculateIterationsForDepth(int depth) {
        return levels.get(0).getIterations();
    }

    public double[] levelCalculateStartCoordinatesOfCluster(int depth, int id) {
        assert false: "not tested";
        int lW = levelCalculateLogicLevelWidth(depth);
        double p = levelCalculatePrecisionAtDepth(depth);
        int clusterX = id % lW;
        int clusterY = id / lW;

        return new double[]{
                region.getStartX() + clusterX * logicClusterWidth * p,
                region.getStartY() + clusterY * logicClusterHeight * p
        };
    }

    //These methods retrieve the value from already created levels
    //Use these methods for accuracy and performance
    public double levelGetAbsoluteClusterWidthAtDepth(int depth) {
        return region.getWidth() / levels.get(depth).getLogicalWidth();
    }
    public double levelGetAbsoluteClusterHeightAtDepth(int depth) {
        return region.getHeight() / levels.get(depth).getLogicalHeight();
    }
    /** Returns distance between two vertical or horizontal points */
    public double levelGetPrecisionAtDepth(int depth) {
        return levels.get(depth).getPrecision();
    }
    /** Returns the amount of clusters the level has at the specific depth*/
    public int levelGetLogicLevelWidth(int depth) {
        return levels.get(depth).getLogicalWidth();
    }
    public int levelGetLogicLevelHeight(int depth) {
        return levels.get(depth).getLogicalHeight();
    }
    public int levelGetIterationsForDepth(int depth) {
        return levels.get(depth).getIterations();
    }

    public double[] levelGetStartCoordinatesOfCluster(int depth, int id) {
        Level<T> l = levels.get(depth);
        int clusterX = id % l.getLogicalWidth();
        int clusterY = id / l.getLogicalWidth();

        return new double[]{
                region.getStartX() + clusterX * logicClusterWidth * l.getPrecision(),
                region.getStartY() + clusterY * logicClusterHeight * l.getPrecision()
        };
    }

    public double[] valueGetCenterCoordinates(int depth, int id, int index) {
        Level<T> l = levels.get(depth);
        int clusterX = id % l.getLogicalWidth();
        int clusterY = id / l.getLogicalHeight();

        double valueX = index % logicClusterWidth + .5;
        double valueY = (index / logicClusterWidth) + .5;

        return new double[]{
                region.getStartX() + clusterX * logicClusterWidth * (l.getPrecision()) + l.getPrecision() * (valueX + .5),
                region.getStartY() + clusterY * logicClusterHeight * (l.getPrecision()) + l.getPrecision() * (valueY + .5)
        };
    }

    /** ensures the all levels with the depth up to and including the parameter exist*/
    public void ensureLevelWithDepth(int depth) {
        assert depth > -1;
        Level<T> prev = levels.get(levels.size() - 1);
        for(int i = levels.size(); i <= depth; i++) {
            levels.add(prev = new Level<>(i, prev.getLogicalWidth() * clusterFactor,
                    prev.getLogicalHeight() * clusterFactor,
                    prev.getPrecision() / clusterFactor,
                    levelCalculateIterationsForDepth(i)));
        }
    }

    public int[] levelCalculateFirstMatchingCluster(int depth, double startX, double startY) {
        //Get relative position between 0 and 1
        startX -= region.getStartX();
        startY -= region.getStartY();
        startX /= region.getWidth();
        startY /= region.getHeight();

        return new int[]{(int) Math.floor(startX * levelCalculateLogicLevelWidth(depth)),
                (int) Math.floor(startY * levelCalculateLogicLevelHeight(depth))};
    }

    public Cluster<T> createCluster(int id) {
        return new Cluster<>(createArray(logicClusterWidth * logicClusterHeight), id);
    }

    protected abstract T[] createArray(int length);

    public void iterateOverRegion(Region region, double minPrecision, DataAcceptor<T> function) {
        int depth = getLevelWithMinPrecision(minPrecision);
        ensureLevelWithDepth(depth);
        Level<T> l = getLevels().get(depth);

        int clustersInX = (int)Math.ceil(region.getWidth()/levelCalculateAbsoluteClusterWidthAtDepth(depth));
        int clustersInY = (int)Math.ceil(region.getHeight()/levelCalculateAbsoluteClusterHeightAtDepth(depth));

        //Walk through necessary clusters
        int[] firstCluster = levelCalculateFirstMatchingCluster(depth, region.getStartX(), region.getStartY());
        int[] end = new int[] {firstCluster[0] + clustersInX, firstCluster[1] + clustersInY};

        if(firstCluster[0] < 0) {
            firstCluster[0] = 0;
        }
        if(firstCluster[1] < 0) {
            firstCluster[1] = 0;
        }
        if(end[0] > l.getLogicalWidth()) {
            end[0] = l.getLogicalWidth();
        }
        if(end[1] > l.getLogicalHeight()) {
            end[1] = l.getLogicalHeight();
        }
        clustersInX = end[0] - firstCluster[0];
        clustersInY = end[1] - firstCluster[1];
        if(clustersInX < 1 || clustersInY < 1) {
            System.out.println("[WARNING]Iterating area with no clusters");
        }

        function.setPrecision(levelGetPrecisionAtDepth(depth));

        //TODO walk through sorted list more efficiently
        for(Cluster<T> c: l.getClusters()) {
            int x = c.getId() % l.getLogicalWidth(), y = c.getId() / l.getLogicalWidth();
            if(firstCluster[0] <= x && x < end[0] && firstCluster[1] <= y && y < end[1]) {
                function.accept(c, x, y);
            }
        }
    }

    public CalculableArea<T> createCalculableArea(Region region, double minPrecision) {
        int depth = getLevelWithMinPrecision(minPrecision);
        ensureLevelWithDepth(depth);
        Level<T> l = getLevels().get(depth);
        ArrayList<Cluster<T>> list = new ArrayList<>();

        int clustersInX = (int)Math.ceil(region.getWidth()/levelCalculateAbsoluteClusterWidthAtDepth(depth));
        int clustersInY = (int)Math.ceil(region.getHeight()/levelCalculateAbsoluteClusterHeightAtDepth(depth));

        //Walk through necessary clusters
        int[] firstCluster = levelCalculateFirstMatchingCluster(depth, region.getStartX(), region.getStartY());
        int[] end = new int[] {firstCluster[0] + clustersInX, firstCluster[1] + clustersInY};

        if(firstCluster[0] < 0) {
            firstCluster[0] = 0;
        }
        if(firstCluster[1] < 0) {
            firstCluster[1] = 0;
        }
        if(end[0] > l.getLogicalWidth()) {
            end[0] = l.getLogicalWidth();
        }
        if(end[1] > l.getLogicalHeight()) {
            end[1] = l.getLogicalHeight();
        }
        clustersInX = end[0] - firstCluster[0];
        clustersInY = end[1] - firstCluster[1];
        if(clustersInX < 1 || clustersInY < 1) {
            System.out.println("[WARNING]Calculating area with no clusters");
            return new CalculableArea<>
                    (depth, levelGetPrecisionAtDepth(depth), list);
        }

        list.ensureCapacity(clustersInX * clustersInY);

        if(!l.getClusters().isEmpty()) {
            Iterator<Cluster<T>> it = l.getClusters().iterator();
            Cluster<T> curr = it.next();
            int idToCalculate;

            for(int y = firstCluster[1]; y < end[1]; y++) {
                for(int x = firstCluster[0]; x < end[0]; x++) {
                    idToCalculate = x + y * l.getLogicalWidth();
                    while(curr.getId() < idToCalculate && it.hasNext()) {
                        curr = it.next();
                    }
                    if(curr.getId() > idToCalculate || (curr.getId() < idToCalculate && !it.hasNext())) {
                        list.add(createCluster(idToCalculate));
                    }
                    //TODO exit loop and add remaining elements manually when it.hasNext() returns false
                    //Works too this way but is less performant
                }
            }

            list.trimToSize(); //Make list smaller to reduce footprint
        } else {
            for(int y = firstCluster[1]; y < end[1]; y++) {
                for(int x = firstCluster[0]; x < end[0]; x++) {
                    list.add(createCluster(x + y * l.getLogicalWidth()));
                }
            }
        }

        return new CalculableArea<>
                (depth, levelGetPrecisionAtDepth(depth), list);
    }

    private int getLevelWithMinPrecision(double precision) {
        int depth = (int) Math.ceil(Math.log(firstLevelPrecision / precision) / Math.log(clusterFactor));
        assert levelCalculatePrecisionAtDepth(depth) <= precision;
        return depth;
    }

    public void returnCalculableArea(CalculableArea<T> area) {
        getLevels().get(area.getDepth()).addAll(area.getClusters());
    }

    public ArrayList<Level<T>> getLevels() {
        return levels;
    }

    public int getLogicClusterWidth() {
        return logicClusterWidth;
    }

    public int getLogicClusterHeight() {
        return logicClusterHeight;
    }


    public static DataSet<ValueMandelbrot> createDataSet(int logicClusterWidth, int logicClusterHeight, int startLogicLevelWidth,
                                                         int startLogicLevelHeight, int clusterFactor, double regionStartX,
                                                         double regionStartY, double regionWidth, int iterationsForFirstLevel) {
        return new DataSetMandelbrot(logicClusterWidth, logicClusterHeight, startLogicLevelWidth, startLogicLevelHeight, clusterFactor,
                regionStartX, regionStartY, regionWidth, iterationsForFirstLevel);
    }

    private static class DataSetMandelbrot extends DataSet<ValueMandelbrot> {
        /**
         * The absolute height is calculated through the logic cluster dimensions and the start values for the logic level dimensions
         *
         * @param logicClusterWidth
         * @param logicClusterHeight
         * @param startLogicLevelWidth
         * @param startLogicLevelHeight
         * @param clusterFactor
         * @param regionStartX
         * @param regionStartY
         * @param regionWidth
         * @param iterationsForFirstLevel
         */
        public DataSetMandelbrot(int logicClusterWidth, int logicClusterHeight, int startLogicLevelWidth, int startLogicLevelHeight, int clusterFactor, double regionStartX, double regionStartY, double regionWidth, int iterationsForFirstLevel) {
            super(logicClusterWidth, logicClusterHeight, startLogicLevelWidth, startLogicLevelHeight, clusterFactor, regionStartX, regionStartY, regionWidth, iterationsForFirstLevel);
        }

        @Override
        protected ValueMandelbrot[] createArray(int length) {
            return new ValueMandelbrot[length];
        }
    }

    public static void main(String ... args) {
        DataSet<ValueMandelbrot> d =  createDataSet(100, 100, 1,
                1, 4, -1, -1, 2, 3000);
        d.ensureLevelWithDepth(10);

        int depth=0;

        long t = System.currentTimeMillis();

        CalculatorMandelbrot calc = new CalculatorMandelbrot();
        CalculableArea<ValueMandelbrot> area = d.createCalculableArea(
                new Region(-1, -1, 1, 1),
                0.001);
        calc.calculate(area, d, 12);
        d.returnCalculableArea(area);
        depth = area.getDepth();

        System.out.println("Time: " + (System.currentTimeMillis() - t));

        Screen s = new Screen(d.getLevels().get(depth).getLogicalWidth() * d.logicClusterWidth,
                d.getLevels().get(depth).getLogicalHeight() * d.logicClusterHeight, 0xff00ff);
        DataAcceptor<ValueMandelbrot> ac = new DataAcceptor<>() {
            final int logicClusterWidth = d.getLogicClusterWidth(), logicClusterHeight = d.getLogicClusterHeight();
            @Override
            public void accept(Cluster<ValueMandelbrot> t, int clusterX, int clusterY) {
                int xOff = clusterX * logicClusterWidth, yOff = clusterY * logicClusterHeight;
                for(int y = 0; y < logicClusterWidth; y++) {
                    for(int x = 0; x < logicClusterHeight; x++) {
                        assert s.setPixel(xOff + x,yOff + y, t.getValue()[x + y * logicClusterWidth].getValue() == -1 ? 0xffffff: 0x0);
                    }
                }
            }
        };

        d.iterateOverRegion(new Region(-1, -1, 1, 1), 0.001, ac);
        FileManager.getFileManager().saveImage("/home/mikail/Desktop/File.png", s);
    }
}
