package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.OldCalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.OldDataMandelbrot;
import ch.mikailgedik.kzn.matur.backend.connector.Constants;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.awt.*;
import java.util.ArrayList;
import java.util.function.Function;

public class ImageCreator implements Function<Integer, Integer> {
    private SettingsManager settingsManager;
    private ArrayList<ImageResult<OldDataMandelbrot>> buffer;
    private ImageResult<OldDataMandelbrot> latestImageResult;
    /**latest params used*/
    private double minx,  maxx, miny, maxy;

    public ImageCreator(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        this.buffer = new ArrayList<>();
        latestImageResult = null;
        //TODO
    }

    public Screen createImage(OldCalculationResult.CalculationResultMandelbrot data) {
        int width = settingsManager.getI(SettingsManager.RENDER_IMAGE_WIDTH), height = settingsManager.getI(SettingsManager.RENDER_IMAGE_HEIGHT);

        minx = settingsManager.getD(SettingsManager.RENDER_MINX);
        maxx = settingsManager.getD(SettingsManager.RENDER_MAXX);
        miny = settingsManager.getD(SettingsManager.RENDER_MINY);
        maxy = settingsManager.getD(SettingsManager.RENDER_MAXY);

        int reqDepth = reqDepth(width, height, data);

        this.latestImageResult = createImageResult(reqDepth, data);
        Screen ret = getLatestScreen();

        assert ret.getWidth() >= width;
        assert ret.getHeight() >= height;
        assert ret.getWidth() < width * data.getTiles() || ret.getHeight() < height * data.getTiles();

        return ret;
    }

    private int reqDepth(int pixelW, int pixelH, OldCalculationResult.CalculationResultMandelbrot data) {
        //TODO better way to find reqDepth
        double pixelSizeW = (maxx - minx) / pixelW;
        double pixelSizeH = (maxy - miny) / pixelH;
        double pixelSizeDataW = data.getWidth();
        double pixelSizeDataH = data.getHeight();
        int depthW = 0;
        int depthH = 0;
        while((pixelSizeDataW /= data.getTiles()) > pixelSizeW) {
            depthW++;
        }
        while((pixelSizeDataH /= data.getTiles()) > pixelSizeH) {
            depthH++;
        }
        return Math.max(depthH, depthW);
    }

    private ImageResult<OldDataMandelbrot> createImageResult(int depth, OldCalculationResult.CalculationResultMandelbrot data) {
        ArrayList<OldCalculationResult.Level<OldDataMandelbrot>> levels = data.getLevels();

        if(levels.size() <= depth) {
            data.ensureDepth(depth);
        }

        OldCalculationResult.Level<OldDataMandelbrot> l = data.getLevel(depth);
        double chunkWidth = data.getWidth() / l.getSize(), chunkHeight = data.getHeight() / l.getSize();

        int clustersX = (int) Math.ceil((maxx - minx) / chunkWidth);
        int clustersY = (int) Math.ceil((maxy - miny) / chunkHeight);

        int startXCluster, startYCluster;

        startXCluster = (int) ((minx - data.getStartX()) / data.getWidth() * l.getSize());
        startYCluster = (int) ((miny - data.getStartY()) / data.getHeight() * l.getSize());

        for(ImageResult<OldDataMandelbrot> ir: buffer) {
            if(ir.getStartXCluster() == startXCluster && ir.getStartYCluster() == startYCluster &&
            clustersX == ir.getClustersX() && clustersY == ir.getClustersY() && depth == ir.getDepth() && data == ir.getCalculationResult()) {
                return ir;
            }
        }

        ImageResult<OldDataMandelbrot> ret =
                new ImageResult<>(startXCluster, startYCluster, clustersX, clustersY,
                        depth, data, this);

        ret.populate();

        buffer.add(ret);
        return ret;
    }

    public void updateLatestImageResult() {
        this.latestImageResult.populate();
    }

    public Screen getLatestScreen() {
        Screen screen = latestImageResult.getScreen();

        int sx = (int)((minx - latestImageResult.getStartX()) / latestImageResult.getWidth() * screen.getWidth()),
                sy = (int)((miny - latestImageResult.getStartY()) / latestImageResult.getHeight() * screen.getHeight()),
                ex = (int)((maxx - latestImageResult.getStartX()) / latestImageResult.getWidth() * screen.getWidth()),
                ey = (int)((maxy - latestImageResult.getStartY()) / latestImageResult.getHeight() * screen.getHeight());

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

    @Override
    public Integer apply(Integer value) {
        if(value == -1) {
            return 0x000000;
        }

        double max = Math.log(settingsManager.getI(Constants.CALCULATION_MAX_ITERATIONS));
        double log = Math.log(value);

        return Color.HSBtoRGB((float) (log/max),1, 1) & 0xffffff;
    }

    public ImageResult<OldDataMandelbrot> getLatestImageResult() {
        return latestImageResult;
    }
}
