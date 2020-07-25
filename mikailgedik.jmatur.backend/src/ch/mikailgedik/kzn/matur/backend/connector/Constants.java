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

    String CALCULATION_MINX = "double.setting.calculation.minX";
    String CALCULATION_MAXX = "double.setting.calculation.maxX";
    String CALCULATION_MINY = "double.setting.calculation.minY";
    String CALCULATION_MAXY = "double.setting.calculation.maxY";
    String CALCULATION_MAX_ITERATIONS = "int.setting.calculation.maxIterations";
    String CALCULATION_MAX_THREADS = "int.setting.calculation.maxThreads";
    String CALCULATION_MAX_WAITING_TIME_THREADS = "int.setting.calculation.maxWaitingTimeForThread";
    String CALCULATION_CLUSTER_INIT_DEPTH = "int.setting.calculation.cluster.depth";
    String CALCULATION_CLUSTER_TILES = "int.setting.calculation.cluster.tiles";
}
