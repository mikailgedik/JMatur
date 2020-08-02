package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.Cluster;
import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

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


        Cluster<CalculationResult.DataMandelbrot> cluster = data.getCluster();

        int requiredDepthX = (int)Math.ceil(Math.log(piDenX * cluster.getWidth())/Math.log(cluster.getTiles()));
        int requiredDepthY = (int)Math.ceil(Math.log(piDenY * cluster.getHeight())/Math.log(cluster.getTiles()));

        int reqDepth = Math.max(requiredDepthX, requiredDepthY);
        Screen screen = new Screen((int) Math.pow(cluster.getTiles(), reqDepth),
                (int) Math.pow(cluster.getTiles(), reqDepth));

        screen.forRect(0,0,screen.getWidth(), screen.getHeight(), (i) -> (int)(Math.random() * 0xffffff));

        drawCluster(0,0, screen, cluster, reqDepth - 1, screen.getWidth());



        {
            double startx, starty, endx, endy;
            startx = (minx - cluster.getStartx()) / (cluster.getWidth()) * screen.getWidth();
            endx = (maxx - cluster.getStartx()) / (cluster.getWidth()) * screen.getWidth();

            starty = (miny - cluster.getStarty()) / (cluster.getHeight()) * screen.getHeight();
            endy = (maxy - cluster.getStarty()) / (cluster.getHeight())* screen.getHeight();

            screen = screen.subScreen((int)startx, (int)starty,
                    (int)(endx -startx),
                    (int)(endy -starty));
        }

        //Reverse y-Axis
        int[] help = new int[screen.getWidth()];
        for(int y = 0; y < screen.getHeight() /2; y++) {
            System.arraycopy(screen.getPixels(), y * help.length, help, 0, help.length);
            System.arraycopy(screen.getPixels(), (screen.getHeight() - y -1) * help.length, screen.getPixels(), y * help.length, help.length);
            System.arraycopy(help, 0, screen.getPixels(), (screen.getHeight() - y -1) * help.length, help.length);
        }

        return screen.getScaledScreen(width, height);
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
