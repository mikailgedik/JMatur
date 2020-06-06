package ch.mikailgedik.kzn.matur.render;

import ch.mikailgedik.kzn.matur.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.settings.SettingsManager;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public class ImageCreator {
    public ImageCreator() {
        //TODO
    }

    public BufferedImage createImage(CalculationResult<CalculationResult.DataMandelbrot> data, int width, int height, SettingsManager sm) {
        SettingsManager.SettingViewport vp =
                (SettingsManager.SettingViewport) sm.get(SettingsManager.SettingViewport.IDENTIFIER);
        SettingsManager.SettingNumber num =
                (SettingsManager.SettingNumber) sm.get(SettingsManager.SettingNumber.IDENTIFIER);

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        int[] content = ((DataBufferInt)image.getRaster().getDataBuffer()).getData();

        for(int x = 0; x < width; x++) {
            for(int y = 0; y < height; y++) {
                double[] loc = {vp.getxCo()[0] + (vp.getxCo()[1] - vp.getxCo()[0]) * (1.0 * x / width),
                        vp.getyCo()[0] + (vp.getyCo()[1] - vp.getyCo()[0]) * (1.0 * y / height)};
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
