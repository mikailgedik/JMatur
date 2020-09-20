package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.DataMandelbrot;
import ch.mikailgedik.kzn.matur.backend.connector.Constants;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Function;

public class ImageCreator implements Function<Integer, Integer> {
    private SettingsManager settingsManager;
    private ArrayList<ImageResult<DataMandelbrot>> buffer;

    public ImageCreator(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        this.buffer = new ArrayList<>();
        //TODO
    }

    public Screen createImage(CalculationResult.CalculationResultMandelbrot data) {
        int width = settingsManager.getI(SettingsManager.RENDER_IMAGE_WIDTH), height = settingsManager.getI(SettingsManager.RENDER_IMAGE_HEIGHT);
        Screen result = new Screen(width, height);

        double minx = settingsManager.getD(SettingsManager.RENDER_MINX);
        double maxx = settingsManager.getD(SettingsManager.RENDER_MAXX);
        double miny = settingsManager.getD(SettingsManager.RENDER_MINY);
        double maxy = settingsManager.getD(SettingsManager.RENDER_MAXY);
        int maxWaitTime = settingsManager.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);
        int maxThreads = settingsManager.getI(SettingsManager.CALCULATION_MAX_THREADS);

        double piDenX = width / (maxx - minx);
        double piDenY = height / (maxy - miny);

        int requiredDepthX = (int)Math.ceil(Math.log(piDenX * data.getWidth())/Math.log(data.getTiles()));
        int requiredDepthY = (int)Math.ceil(Math.log(piDenY * data.getHeight())/Math.log(data.getTiles()));

        int reqDepth = Math.max(requiredDepthX, requiredDepthY);

        ImageResult<DataMandelbrot> imageResult = createImageResult(minx, maxx, miny, maxy, reqDepth, data);

        Screen screen = imageResult.getScreen();

        int sx = (int)((minx - imageResult.getStartX()) / imageResult.getWidth() * screen.getWidth()),
                sy = (int)((miny - imageResult.getStartY()) / imageResult.getHeight() * screen.getHeight()),
                ex = (int)((maxx - imageResult.getStartX()) / imageResult.getWidth() * screen.getWidth()),
                ey = (int)((maxy - imageResult.getStartY()) / imageResult.getHeight() * screen.getHeight());

        screen = screen.subScreen(sx, sy,ex - sx, ey -sy);

        //Reverse y-Axis
        int[] help = new int[screen.getWidth()];
        for(int y = 0; y < screen.getHeight() /2; y++) {
            System.arraycopy(screen.getPixels(), y * help.length, help, 0, help.length);
            System.arraycopy(screen.getPixels(), (screen.getHeight() - y -1) * help.length, screen.getPixels(), y * help.length, help.length);
            System.arraycopy(help, 0, screen.getPixels(), (screen.getHeight() - y -1) * help.length, help.length);
        }

        return screen;
    }

    private ImageResult<DataMandelbrot> createImageResult(double minx, double maxx, double miny, double maxy, int depth, CalculationResult.CalculationResultMandelbrot data) {
        ArrayList<CalculationResult.Level<DataMandelbrot>> levels = data.getLevels();

        if(levels.size() <= depth) {
            data.ensureDepth(depth);
            //assert false;
        }

        CalculationResult.Level<DataMandelbrot> l = data.getLevel(depth);
        double chunkWidth = data.getWidth() / l.getSize(), chunkHeight = data.getHeight() / l.getSize();

        int clustersX = (int) Math.ceil((maxx - minx) / chunkWidth);
        int clustersY = (int) Math.ceil((maxy - miny) / chunkHeight);

        int startXCluster, startYCluster;

        startXCluster = (int) ((minx - data.getStartX()) / data.getWidth() * l.getSize());
        startYCluster = (int) ((miny - data.getStartY()) / data.getHeight() * l.getSize());

        for(ImageResult<DataMandelbrot> ir: buffer) {
            if(ir.getStartXCluster() == startXCluster && ir.getStartYCluster() == startYCluster &&
            clustersX == ir.getClustersX() && clustersY == ir.getClustersY() && depth == ir.getDepth() && data == ir.getCalculationResult()) {
                return ir;
            }
        }

        ImageResult<DataMandelbrot> ret =
                new ImageResult<>(startXCluster, startYCluster, clustersX, clustersY,
                        depth, data, this);

        buffer.add(ret);
        return ret;
    }

    @Override
    public Integer apply(Integer value) {
        if(value == -1) {
            return 0x000000;
        }

        double max = Math.log(settingsManager.getI(Constants.CALCULATION_MAX_ITERATIONS));
        double log = Math.log(value);

        return Color.HSBtoRGB((float) (log/max),1, 1) & 0xffffff;
    }
}
