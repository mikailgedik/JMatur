package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.MandelbrotCalculator;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreator;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.awt.image.BufferedImage;

/** This class connects the frontend with the backend */
public class Connector {
    private final SettingsManager settingsManager;
    private final MandelbrotCalculator calculator;
    private CalculationResult<CalculationResult.DataMandelbrot> calculationResult;
    private final ImageCreator imageCreator;
    private BufferedImage image;

    public Connector() {
        calculationResult = null;
        image = null;

        settingsManager = SettingsManager.createDefaultSettingsManager();
        calculator = new MandelbrotCalculator(settingsManager);
        imageCreator = new ImageCreator(settingsManager);
    }

    public Object getSetting(String name) {
        return settingsManager.get(name);
    }

    public String getSettingS(String name) {
        return settingsManager.getS(name);
    }

    public int getSettingI(String name) {
        return settingsManager.getI(name);
    }

    public double getSettingD(String name) {
        return settingsManager.getD(name);
    }
    public void setSetting(String name, Object value) {
        settingsManager.addSetting(name, value);
    }

    public void calculate() {
        calculationResult = calculator.calculate();
    }

    public void createImage() {
        assert calculationResult != null;
        image = imageCreator.createImage(calculationResult);
    }

    public BufferedImage getImage() {
        return image;
    }

    public void saveImage(String path) {
        assert image != null;
        assert path != null;
        FileManager.getFileManager().saveImage(path, image);
    }


}
