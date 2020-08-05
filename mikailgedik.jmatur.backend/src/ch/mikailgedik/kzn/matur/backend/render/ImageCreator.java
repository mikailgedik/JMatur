package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.DataMandelbrot;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.concurrent.atomic.AtomicInteger;

public class ImageCreator {
    private SettingsManager settingsManager;
    public ImageCreator(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
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
        Screen screen = allClusters(data, minx, maxx, miny, maxy, reqDepth);

        //Reverse y-Axis
        int[] help = new int[screen.getWidth()];
        for(int y = 0; y < screen.getHeight() /2; y++) {
            System.arraycopy(screen.getPixels(), y * help.length, help, 0, help.length);
            System.arraycopy(screen.getPixels(), (screen.getHeight() - y -1) * help.length, screen.getPixels(), y * help.length, help.length);
            System.arraycopy(help, 0, screen.getPixels(), (screen.getHeight() - y -1) * help.length, help.length);
        }

        return screen;
    }

    private Screen allClusters(CalculationResult.CalculationResultMandelbrot result, double minx, double maxx, double miny, double maxy, int depth) {
        CalculationResult.Level<DataMandelbrot> l = result.getLevel(depth);
        Screen ret = new Screen(l.getSize() * result.getTiles(), l.getSize() * result.getTiles());

        for(int y = 0; y < l.getSize(); y++) {
            for(int x = 0; x < l.getSize(); x++) {
                drawData(l.get()[x + y * l.getSize()], x * result.getTiles(), y * result.getTiles(), ret, result.getTiles());
            }
        }


        int startX, startY, endX, endY;

        startX =(int) ((minx - result.getStartX()) / result.getWidth() * ret.getWidth());
        startY =(int) ((miny - result.getStartY()) / result.getHeight() * ret.getHeight());
        endX =  (int) ((maxx - result.getStartX()) / result.getWidth() * ret.getWidth());
        endY =  (int) ((maxy - result.getStartY()) / result.getHeight() * ret.getHeight());

        return ret.subScreen(startX, startY, endX -startX, endY -startY);
    }

    private void drawData(DataMandelbrot[] d, int startX, int startY, Screen s, int tiles) {
        for(int x = 0; x < tiles; x++) {
            for(int y = 0; y < tiles; y++) {
                s.setPixel(x + startX, y + startY, d[x + y * tiles].getValue() ? 0xff00ff : 0x00ff00);
            }
        }
    }
}
