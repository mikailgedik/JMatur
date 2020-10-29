package ch.mikailgedik.kzn.matur.backend.data;

import java.util.ArrayList;
import java.util.Iterator;


public abstract class DataSet {
    private final ArrayList<Level> levels;
    /** logic cluster width: amount of values
     * absolute cluster width: width in mathematical sense*/
    private final int logicClusterWidth, logicClusterHeight;
    private final int firstLevelLogicWidth, firstLevelLogicHeight;
    private final double firstLevelPrecision;
    private final int clusterFactor;
    private final Region region;
    private final IterationModel iterationModel;

    /** The absolute height is calculated through the logic cluster dimensions and the start values for the logic level dimensions */

    public DataSet(int logicClusterWidth, int logicClusterHeight, int startLogicLevelWidth, int startLogicLevelHeight, int clusterFactor,
                   double regionStartX, double regionStartY, double regionWidth, int iterationsForFirstLevel, IterationModel iterationModel) {
        this.levels = new ArrayList<>();
        this.logicClusterWidth = logicClusterWidth;
        this.logicClusterHeight = logicClusterHeight;
        this.firstLevelLogicWidth = startLogicLevelWidth;
        this.firstLevelLogicHeight = startLogicLevelHeight;
        this.clusterFactor = clusterFactor;
        this.iterationModel = iterationModel;
        //Create first level

        double absoluteClusterWidth = regionWidth / startLogicLevelWidth;
        double absoluteClusterHeight = absoluteClusterWidth * logicClusterHeight / logicClusterWidth;
        double absoluteLevelHeight = startLogicLevelHeight * absoluteClusterHeight;

        levels.add(new Level(
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
        return iterationModel.
                getIterations(levels.get(0).getIterations(), depth, levelCalculatePrecisionAtDepth(depth), levels.get(0).getPrecision());
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
        Level l = levels.get(depth);
        return levelGetStartCoordinatesOfCluster(depth, id % l.getLogicalWidth(), id / l.getLogicalWidth());
    }

    public double[] levelGetStartCoordinatesOfCluster(int depth, int clusterX, int clusterY) {
        Level l = levels.get(depth);

        return new double[]{
                region.getStartX() + clusterX * logicClusterWidth * l.getPrecision(),
                region.getStartY() + clusterY * logicClusterHeight * l.getPrecision()
        };
    }

    /** ensures the all levels with the depth up to and including the parameter exist*/
    public void ensureLevelWithDepth(int depth) {
        //TODO check if this method is called too often
        assert depth > -1;
        Level prev = levels.get(levels.size() - 1);
        for(int i = levels.size(); i <= depth; i++) {
            levels.add(prev = new Level(i, prev.getLogicalWidth() * clusterFactor,
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

    public Cluster createCluster(int id) {
        return new Cluster(new int[logicClusterWidth * logicClusterHeight], id);
        //TODO initialize clusters with null and let calculators create data
        //return new Cluster(null, id);
    }

    public Region dataGetRegion(LogicalRegion logicalRegion) {
        double[] start = levelGetStartCoordinatesOfCluster(logicalRegion.getDepth(), logicalRegion.getStartX(), logicalRegion.getStartY());
        double[] end = levelGetStartCoordinatesOfCluster(logicalRegion.getDepth(), logicalRegion.getEndX(), logicalRegion.getEndY());

        return new Region(start[0], start[1], end[0], end[1]);
    }

    /** The returned LogicalRegion is always bigger than the parameter*/
    public LogicalRegion dataGetUnrestrictedLogicalRegion(Region region, double minPrecision) {
        int depth = levelCalculateLevelWithMinPrecision(minPrecision);
        int clustersInX = (int)Math.ceil(region.getWidth()/levelCalculateAbsoluteClusterWidthAtDepth(depth));
        int clustersInY = (int)Math.ceil(region.getHeight()/levelCalculateAbsoluteClusterHeightAtDepth(depth));

        //Walk through necessary clusters
        int[] firstCluster = levelCalculateFirstMatchingCluster(depth, region.getStartX(), region.getStartY());
        int[] end = levelCalculateFirstMatchingCluster(depth, region.getEndX(), region.getEndY());

        return new LogicalRegion(firstCluster[0], firstCluster[1], end[0] + 1, end[1] + 1, depth);
    }

    public LogicalRegion dataGetRestrictedLogicalRegion(LogicalRegion region) {
        Level l = levels.get(region.getDepth());
        return new LogicalRegion(Math.max(region.getStartX(), 0),
                Math.max(region.getStartY(), 0),
                Math.min(region.getEndX(), l.getLogicalWidth()),
                Math.min(region.getEndY(), l.getLogicalHeight()),
                region.getDepth());
    }

    /** Iterates over all clusters in the region, excluding the region.getEndX() and region.getEndY()!*/
    public void iterateOverLogicalRegion(LogicalRegion region, DataAcceptor function) {
        ensureLevelWithDepth(region.getDepth());
        Level l = getLevels().get(region.getDepth());

        //TODO walk through sorted list more efficiently
        for(Cluster c: l.getClusters()) {
            int x = c.getId() % l.getLogicalWidth(), y = c.getId() / l.getLogicalWidth();
            if(region.getStartX() <= x && x < region.getEndX() && region.getStartY() <= y && y < region.getEndY()) {
                function.accept(c, x, y);
            }
        }
    }

    public CalculableArea createCalculableArea(Region region, double minPrecision) {
        ensureLevelWithDepth(levelCalculateLevelWithMinPrecision(minPrecision));
        return createCalculableArea(dataGetRestrictedLogicalRegion(
                dataGetUnrestrictedLogicalRegion(region, minPrecision)
        ));
    }

    /**
     * The region has to be restricted by {@link #dataGetRestrictedLogicalRegion(LogicalRegion)},
     * or else the calculation might crash
     * @param r the desired region
     * */
    public CalculableArea createCalculableArea(LogicalRegion r) {
        Level l = getLevels().get(r.getDepth());
        ArrayList<Cluster> list = new ArrayList<>();

        list.ensureCapacity(r.getWidth() * r.getHeight());

        if(!l.getClusters().isEmpty()) {
            Iterator<Cluster> it = l.getClusters().iterator();
            Cluster curr = it.next();
            int idToCalculate;

            for(int y = r.getStartY(); y < r.getEndY(); y++) {
                for(int x = r.getStartX(); x < r.getEndX(); x++) {
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
            for(int y = r.getStartY(); y < r.getEndY(); y++) {
                for(int x = r.getStartX(); x < r.getEndX(); x++) {
                    list.add(createCluster(x + y * l.getLogicalWidth()));
                }
            }
        }

        return new CalculableArea
                (r.getDepth(), levelGetPrecisionAtDepth(r.getDepth()), list);
    }

    public int levelCalculateLevelWithMinPrecision(double precision) {
        int depth = (int) Math.ceil(Math.log(firstLevelPrecision / precision) / Math.log(clusterFactor));
        assert levelCalculatePrecisionAtDepth(depth) <= precision;
        return depth;
    }

    public void returnCalculableArea(CalculableArea area) {
        getLevels().get(area.getDepth()).addAll(area.getClusters());
    }

    public ArrayList<Level> getLevels() {
        return levels;
    }

    public int getLogicClusterWidth() {
        return logicClusterWidth;
    }

    public int getLogicClusterHeight() {
        return logicClusterHeight;
    }


    public static DataSet createDataSet(int logicClusterWidth, int logicClusterHeight, int startLogicLevelWidth,
                                                         int startLogicLevelHeight, int clusterFactor, double regionStartX,
                                                         double regionStartY, double regionWidth, int iterationsForFirstLevel, IterationModel iterationModel) {
        return new DataSetMandelbrot(logicClusterWidth, logicClusterHeight, startLogicLevelWidth, startLogicLevelHeight, clusterFactor,
                regionStartX, regionStartY, regionWidth, iterationsForFirstLevel, iterationModel);
    }

    private static class DataSetMandelbrot extends DataSet {
        public DataSetMandelbrot(int logicClusterWidth, int logicClusterHeight, int startLogicLevelWidth, int startLogicLevelHeight, int clusterFactor, double regionStartX, double regionStartY, double regionWidth, int iterationsForFirstLevel, IterationModel iterationModel) {
            super(logicClusterWidth, logicClusterHeight, startLogicLevelWidth, startLogicLevelHeight, clusterFactor, regionStartX, regionStartY, regionWidth, iterationsForFirstLevel, iterationModel);
        }
    }

    public static IterationModel getIterationModelFrom(String name) {
        return switch (name) {
            case "static" -> (s,d,p, sp) -> s;
            case "antiProportional" -> (s,d,p,sp) -> {
                //TODO add setting and improve selection
                int factor = 2;
                return (int)(s * (sp / p));
            };
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
