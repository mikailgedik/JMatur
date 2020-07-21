package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
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
        Screen image = new Screen(width, height);
        int[] content = image.getPixels();

        double minx = settingsManager.getD(SettingsManager.RENDER_MINX);
        double maxx = settingsManager.getD(SettingsManager.RENDER_MAXX);
        double miny = settingsManager.getD(SettingsManager.RENDER_MINY);
        double maxy = settingsManager.getD(SettingsManager.RENDER_MAXY);

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
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

        return image;
    }
}
