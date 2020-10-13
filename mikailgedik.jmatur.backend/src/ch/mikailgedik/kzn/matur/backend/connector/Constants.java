package ch.mikailgedik.kzn.matur.backend.connector;

public interface Constants {
    String VALUE_PROGRAM_VERSION = "string.value.version";

    String FILE_DEFAULT_OUTPUT = "string.file.defaultOutputFile";

    String RENDER_MINX = "double.setting.render.minX";
    String RENDER_MAXX = "double.setting.render.maxX";
    String RENDER_MINY = "double.setting.render.minY";
    String RENDER_MAXY = "double.setting.render.maxY";
    String RENDER_IMAGE_WIDTH = "int.setting.render.imageWidth";
    String RENDER_IMAGE_HEIGHT = "int.setting.render.imageHeight";
    String RENDER_ZOOM_FACTOR = "double.setting.render.zoomFactor";
    String RENDER_COLOR_FUNCTION = "string.setting.render.colorFunction";

    String CALCULATION_START_ITERATION = "int.setting.calculation.startIterations";
    String CALCULATION_ITERATION_MODEL = "string.setting.calculation.iterationModel";
    String CALCULATION_MAX_THREADS = "int.setting.calculation.maxThreads";
    String CALCULATION_MAX_WAITING_TIME_THREADS = "int.setting.calculation.maxWaitingTimeForThread";

    String DATA_LOGIC_CLUSTER_WIDTH = "int.setting.data.logicClusterWidth";
    String DATA_LOGIC_CLUSTER_HEIGHT = "int.setting.data.logicClusterHeight";
    String DATA_START_LOGIC_LEVEL_WIDTH = "int.setting.data.startLogicLevelWidth";
    String DATA_START_LOGIC_LEVEL_HEIGHT = "int.setting.data.startLogicLevelHeight";
    String DATA_CLUSTER_FACTOR = "int.setting.data.clusterFactor";
    String DATA_REGION_START_X = "int.setting.data.regionStartX";
    String DATA_REGION_START_Y = "int.setting.data.regionStartY";
    String DATA_REGION_WIDTH = "int.setting.data.regionWidth";
}
