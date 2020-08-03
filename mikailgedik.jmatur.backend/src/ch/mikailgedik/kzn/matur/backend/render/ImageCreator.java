package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.Cluster;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ImageCreator {
    private SettingsManager settingsManager;
    public ImageCreator(SettingsManager settingsManager) {
        this.settingsManager = settingsManager;
        //TODO
    }

    public Screen createImage(CalculationResult<CalculationResult.DataMandelbrot> data) {
        int width = settingsManager.getI(SettingsManager.RENDER_IMAGE_WIDTH), height = settingsManager.getI(SettingsManager.RENDER_IMAGE_HEIGHT);
        Screen result = new Screen(width, height);
        int[] content = result.getPixels();

        double minx = settingsManager.getD(SettingsManager.RENDER_MINX);
        double maxx = settingsManager.getD(SettingsManager.RENDER_MAXX);
        double miny = settingsManager.getD(SettingsManager.RENDER_MINY);
        double maxy = settingsManager.getD(SettingsManager.RENDER_MAXY);
        int maxWaitTime = settingsManager.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);
        int maxThreads = settingsManager.getI(SettingsManager.CALCULATION_MAX_THREADS);

        double piDenX = width / (maxx - minx);
        double piDenY = height / (maxy - miny);


        Cluster<CalculationResult.DataMandelbrot> baseCluster = data.getCluster();

        int requiredDepthX = (int)Math.ceil(Math.log(piDenX * baseCluster.getWidth())/Math.log(baseCluster.getTiles()));
        int requiredDepthY = (int)Math.ceil(Math.log(piDenY * baseCluster.getHeight())/Math.log(baseCluster.getTiles()));

        int reqDepth = Math.max(requiredDepthX, requiredDepthY);
        Screen screen = allClusters(baseCluster, minx, maxx, miny, maxy, reqDepth -1);

        //Reverse y-Axis
        int[] help = new int[screen.getWidth()];
        for(int y = 0; y < screen.getHeight() /2; y++) {
            System.arraycopy(screen.getPixels(), y * help.length, help, 0, help.length);
            System.arraycopy(screen.getPixels(), (screen.getHeight() - y -1) * help.length, screen.getPixels(), y * help.length, help.length);
            System.arraycopy(help, 0, screen.getPixels(), (screen.getHeight() - y -1) * help.length, help.length);
        }

        return screen.getScaledScreen(width, height);
    }

    private Screen allClusters(Cluster<CalculationResult.DataMandelbrot> baseCluster, double minx, double maxx, double miny, double maxy, int depth) {
        double singleClusterWidth = baseCluster.getWidth() / Math.pow(baseCluster.getTiles(), depth);
        double singleClusterHeight = baseCluster.getHeight() / Math.pow(baseCluster.getTiles(), depth);

        double minRendX ,minRendY, maxRendX, maxRendY;

        minRendX = (int)((minx - baseCluster.getStartx()) / singleClusterWidth);
        minRendX = baseCluster.getStartx() + minRendX * singleClusterWidth;

        maxRendX = (int)((maxx - baseCluster.getStartx()) / singleClusterWidth) + 1;
        maxRendX = baseCluster.getStartx() + maxRendX * singleClusterWidth;

        minRendY = (int)((miny - baseCluster.getStarty()) / singleClusterHeight);
        minRendY = baseCluster.getStarty() + minRendY * singleClusterHeight;

        maxRendY = (int)((maxy - baseCluster.getStarty()) / singleClusterHeight);
        maxRendY = baseCluster.getStarty() + maxRendY * singleClusterHeight;

        minx = minRendX;
        maxx = maxRendX;
        miny = minRendY;
        maxy = maxRendY;

        int tilesAmountWidth = (int)Math.ceil((maxx - minx) / singleClusterWidth);
        int tilesAmountHeight = (int)Math.ceil((maxy - miny) / singleClusterHeight);

        final Screen ret = new Screen(tilesAmountWidth * baseCluster.getTiles(), tilesAmountHeight * baseCluster.getTiles());



        AtomicInteger co = new AtomicInteger();
        double finalMinx = minx;
        double finalMiny = miny;
        double finalMaxx = maxx;
        double finalMaxy = maxy;
        baseCluster.forEachLowestSub(depth, (c) -> {
            if(!(c.getStartx() >= finalMinx && c.getStarty() >= finalMiny && c.getStartx() + c.getWidth() <= finalMaxx && c.getStarty() + c.getHeight() <= finalMaxy)) {
                return;
            }

            int xPos = (int)( tilesAmountWidth * baseCluster.getTiles() * (c.getStartx() - finalMinx) / (finalMaxx - finalMinx));
            int yPos =  (int)(tilesAmountHeight * baseCluster.getTiles() * (c.getStarty() - finalMiny) / (finalMaxy - finalMiny));

            if(c.getDepth() == depth) {
                renderValues(xPos, yPos, ret, c);
            } else {
                renderValues(xPos, yPos, ret, c, (int)Math.pow(c.getTiles(), depth - c.getDepth()));
            }

            co.getAndIncrement();

        });

        return ret;
    }

    private void renderValues(int startX, int startY, Screen s, Cluster<CalculationResult.DataMandelbrot> cluster, int size) {
        for(int y = 0; y < cluster.getTiles(); y++) {
            for(int x = 0; x < cluster.getTiles(); x++) {
                s.fillRect(startX + x * size, startY + y * size, size, size, cluster.getValue(x + y * cluster.getTiles()).getValue() ? 0xff00ff: 0x00ff00);
            }
        }
    }

    private void renderValues(int startX, int startY, Screen s, Cluster<CalculationResult.DataMandelbrot> cluster) {
        for(int y = 0; y < cluster.getTiles(); y++) {
            for(int x = 0; x < cluster.getTiles(); x++) {
                s.setPixel(startX + x, startY + y, cluster.getValue(x + y * cluster.getTiles()).getValue() ? 0xff00ff: 0x00ff00);
            }
        }
    }

    private void drawCluster(int startx, int starty, Screen screen, Cluster<CalculationResult.DataMandelbrot> cluster, int maxDepth, int size) {
        if(cluster.getDepth() < maxDepth) {
            for(int i = 0; i < cluster.getLength(); i++) {
                Cluster<CalculationResult.DataMandelbrot> cl= cluster.get(i);
                if(cl != null) {
                    int nSize = size / cluster.getTiles();
                    drawCluster(startx + (i % cl.getTiles()) * nSize, starty + (i / cl.getTiles()) * nSize, screen, cl, maxDepth, nSize);
                } else {
                    //TODO check code
                    assert false;
                    //System.out.println(size);
                    //int val = cluster.getValue(i).getValue() ? 0xff00ff: 0x0;
                    //screen.fillRect(startx, starty, size, size, val);
                    //screen.forRect(startx, starty, size, size, (p) -> (int) (Math.random() * 0xffffff));
                }
            }
        } else {
            for(int x = 0; x < cluster.getTiles(); x++) {
                for(int y = 0; y < cluster.getTiles(); y++) {
                    screen.setPixel(x + startx, y + starty, cluster.getValue(x + y * cluster.getTiles()).getValue() ? 0xff00ff: 0x0);
                }
            }
        }
    }
}
