package ch.mikailgedik.kzn.matur.render;

import ch.mikailgedik.kzn.matur.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.settings.SettingsManager;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class ImageCreator {
    public ImageCreator() {
        //TODO
    }

    public BufferedImage createImage(CalculationResult<CalculationResult.DataMandelbrot> data, SettingsManager sm) {
        int width = sm.getI(SettingsManager.RENDER_IMAGE_WIDTH), height = sm.getI(SettingsManager.RENDER_IMAGE_HEIGHT);
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] content = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

        double minx = sm.getD(SettingsManager.RENDER_MINX);
        double maxx = sm.getD(SettingsManager.RENDER_MAXX);
        double miny = sm.getD(SettingsManager.RENDER_MINY);
        double maxy = sm.getD(SettingsManager.RENDER_MAXY);

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                double[] loc = {minx + (maxx - minx) * (1.0 * (x + .5) / width),
                        miny + (maxy - miny) * (1.0 * (y + .5) / height)};
                CalculationResult.DataMandelbrot d = data.get(0);

                double dist = (d.getX() - loc[0]) * (d.getX() - loc[0]) + (d.getY() - loc[1]) * (d.getY() - loc[1]);
                double tmpd;
                for(CalculationResult.DataMandelbrot tmp: data) {
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
