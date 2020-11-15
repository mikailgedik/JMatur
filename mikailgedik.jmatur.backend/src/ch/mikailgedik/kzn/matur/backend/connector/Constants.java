package ch.mikailgedik.kzn.matur.backend.connector;

public interface Constants {
    String CONNECTION_ACCEPTS_EXTERNAL = "int.setting.connection.acceptsExternal";
    String CONNECTION_HOST = "string.setting.connection.host";
    String CONNECTION_PORT = "int.setting.connection.port";
    String CONNECTION_BUFFER_LOWER_THRESHOLD = "int.setting.connection.bufferLowerThreshold";
    String CONNECTION_BUFFER_UPPER_THRESHOLD = "int.setting.connection.bufferUpperThreshold";

    String RENDER_ASPECT_RATIO = "double.setting.render.aspectRatio";
    String RENDER_ZOOM_FACTOR = "double.setting.render.zoomFactor";
    String RENDER_FRAMES_PER_SECOND = "int.setting.render.framesPerSecond";

    String CALCULATION_START_ITERATION = "int.setting.calculation.startIterations";
    String CALCULATION_MAX_WAITING_TIME_THREADS = "int.setting.calculation.maxWaitingTimeForThread";

    String DATA_LOGIC_CLUSTER_WIDTH = "int.setting.data.logicClusterWidth";
    String DATA_LOGIC_CLUSTER_HEIGHT = "int.setting.data.logicClusterHeight";
    String DATA_START_LOGIC_LEVEL_WIDTH = "int.setting.data.startLogicLevelWidth";
    String DATA_START_LOGIC_LEVEL_HEIGHT = "int.setting.data.startLogicLevelHeight";
    String DATA_CLUSTER_FACTOR = "int.setting.data.clusterFactor";
    String DATA_REGION_START_X = "int.setting.data.regionStartX";
    String DATA_REGION_START_Y = "int.setting.data.regionStartY";
    String DATA_REGION_WIDTH = "int.setting.data.regionWidth";

    static String getDescription(String val) {
        switch (val) {
            case CONNECTION_ACCEPTS_EXTERNAL:
                return "Accept external connections";
            case CONNECTION_HOST:
                return "Connect to this host as a slave";
            case CONNECTION_PORT:
                return "Connect to this port as a slave";
            case CONNECTION_BUFFER_LOWER_THRESHOLD:
                return "Lower buffer threshold as a slave";
            case CONNECTION_BUFFER_UPPER_THRESHOLD:
                return "Upper buffer threshold as a slave";
            case RENDER_ASPECT_RATIO:
                return "Aspect ratio of generated images";
            case RENDER_ZOOM_FACTOR:
                return "Zoom factor when zooming in";
            case RENDER_FRAMES_PER_SECOND:
                return "Frames per second of exported videos";
            case CALCULATION_START_ITERATION:
                return "Maximal iterations per point";
            case CALCULATION_MAX_WAITING_TIME_THREADS:
                return "Maximal waiting time for the calculation";
            case DATA_LOGIC_CLUSTER_WIDTH:
                return "Logic cluster width";
            case DATA_LOGIC_CLUSTER_HEIGHT:
                return "Logic cluster height";
            case DATA_START_LOGIC_LEVEL_WIDTH:
                return "Level-0 logic width";
            case DATA_START_LOGIC_LEVEL_HEIGHT:
                return "Level-0 logic height";
            case DATA_CLUSTER_FACTOR:
                return "Cluster factor";
            case DATA_REGION_START_X:
                return "Region start x";
            case DATA_REGION_START_Y:
                return "Region start y";
            case DATA_REGION_WIDTH:
                return "Region width";
            default:
                return val;
        }
    }
}
