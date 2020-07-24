package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ImageCreator {
    private SettingsManager settingsManager;
    public ImageCreator(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        //TODO
    }

    public Screen createImage(CalculationResult<CalculationResult.DataMandelbrot> data) {
        int width = settingsManager.getI(SettingsManager.RENDER_IMAGE_WIDTH), height = settingsManager.getI(SettingsManager.RENDER_IMAGE_HEIGHT);
        Screen image = new Screen(width, height);
        int[] content = image.getPixels();

        double minx = settingsManager.getD(SettingsManager.RENDER_MINX);
        double maxx = settingsManager.getD(SettingsManager.RENDER_MAXX);
        double miny = settingsManager.getD(SettingsManager.RENDER_MINY);
        double maxy = settingsManager.getD(SettingsManager.RENDER_MAXY);
        int maxWaitTime = settingsManager.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);
        int maxThreads = settingsManager.getI(SettingsManager.CALCULATION_MAX_THREADS);
        ExecutorService executorService = Executors.newFixedThreadPool(maxThreads);

        int pixelX = 0;
        int pixelIncr = width / maxThreads;
        for(int i = 0; i < maxThreads; i++) {
            if(i == maxThreads -1) {
                executorService.submit(new ThreadRender(content, pixelX,0, width, height,
                        minx, maxx, miny, maxy, width, height, data));
            } else {
                executorService.submit(new ThreadRender(content, pixelX,0, pixelX + pixelIncr, height,
                        minx, maxx, miny, maxy, width, height, data));
            }
            pixelX += pixelIncr;
        }

        executorService.shutdown();
        try {
            if(!executorService.awaitTermination(maxWaitTime, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Threadpool exceeded max wait time (" + maxWaitTime + "ms)");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        return image;
    }

    private static class ThreadRender implements Runnable {
        private int startx, starty, endx, endy;
        private int[] content;
        private int width, height;
        private double minx, maxx, miny, maxy;
        private CalculationResult<CalculationResult.DataMandelbrot> data;

        public ThreadRender(int[] content, int startx, int starty, int endx, int endy,
                            double minx, double maxx, double miny, double maxy, int width, int height,
                            CalculationResult<CalculationResult.DataMandelbrot> data) {
            this.startx = startx;
            this.starty = starty;
            this.endx = endx;
            this.endy = endy;
            this.content = content;

            this.minx = minx;
            this.maxx = maxx;
            this.miny = miny;
            this.maxy = maxy;
            this.width = width;
            this.height = height;
            this.data = data;
        }

        @Override
        public void run() {
            for(int x = startx; x < endx; x++) {
                for(int y = starty; y < endy; y++) {
                    double[] loc = {minx + (maxx - minx) * (1.0 * (x + .5) / width),
                            miny + (maxy - miny) * (1.0 * (y + .5) / height)};

                    CalculationResult.Cluster<CalculationResult.DataMandelbrot> chunk = data.getCluster(loc[0], loc[1]);

                    CalculationResult.DataMandelbrot d = chunk.get(0);

                    double dist = (d.getX() - loc[0]) * (d.getX() - loc[0]) + (d.getY() - loc[1]) * (d.getY() - loc[1]);
                    double tmpd;
                    for(CalculationResult.DataMandelbrot tmp: chunk) {
                        tmpd = (tmp.getX() - loc[0]) * (tmp.getX() - loc[0]) + (tmp.getY() - loc[1]) * (tmp.getY() - loc[1]);
                        if(dist > tmpd) {
                            dist = tmpd;
                            d = tmp;
                        }
                    }
                    content[x + ((height - y - 1) * width)] = d.getValue() ? 0xffffff: 0xff00ff;
                }
            }
        }
    }
}
