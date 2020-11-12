package ch.mikailgedik.kzn.matur.backend.connector;

public interface Constants {
    String VALUE_PROGRAM_VERSION = "string.value.version";

    String FILE_DEFAULT_OUTPUT = "string.file.defaultOutputFile";

    String CONNECTION_ACCEPTS_EXTERNAL = "int.setting.connection.acceptsExternal";
    String CONNECTION_HOST = "string.setting.connection.host";
    String CONNECTION_PORT = "int.setting.connection.port";

    String RENDER_ASPECT_RATIO = "double.setting.render.aspectRatio";
    String RENDER_ZOOM_FACTOR = "double.setting.render.zoomFactor";
    String RENDER_FRAMES_PER_SECOND = "int.setting.render.framesPerSecond";

    String CALCULATION_START_ITERATION = "int.setting.calculation.startIterations";
    String CALCULATION_ITERATION_MODEL = "string.setting.calculation.iterationModel";
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
