package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.backend.calculator.MandelbrotCalculator;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreator;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.Arrays;
import java.util.TreeMap;

/** This class connects the frontend with the backend */
public class Connector {
    private final SettingsManager settingsManager;
    private final MandelbrotCalculator calculator;
    private CalculationResult<CalculationResult.DataMandelbrot> calculationResult;
    private final ImageCreator imageCreator;
    private Screen image;
    private double zoom;

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

    public Screen getImage() {
        return image;
    }

    public void saveImage(String path) {
        assert image != null;
        assert path != null;
        FileManager.getFileManager().saveImage(path, image);
    }

    public TreeMap<String, Object> getAllSettings() {
        return settingsManager.getAllSettings();
    }

    public void zoom(double ticks) {
        double[] bounds = settingsManager.getRenderConstraints();
        double factor = Math.exp(settingsManager.getD(Constants.RENDER_ZOOM_FACTOR) * ticks);

        double dx = (bounds[1] - bounds[0])/2;
        double dy = (bounds[3] - bounds[2])/2;
        double midX = bounds[0] + dx;
        double midY = bounds[2] + dy;

        dx *= factor;
        dy *= factor;

        bounds[0] = midX - dx;
        bounds[1] = midX + dx;
        bounds[2] = midY - dy;
        bounds[3] = midY + dy;


        settingsManager.setRenderConstraints(bounds);
    }

    /** @param dx relative distance
     * @param dy relative distance**/
    public void moveRenderZone(double dx, double dy) {
        double[] bounds = settingsManager.getRenderConstraints();

        double w = (bounds[1] - bounds[0]);
        double h = (bounds[3] - bounds[2]);

        bounds[0] += w * dx;
        bounds[1] += w * dx;
        bounds[2] += h * dy;
        bounds[3] += h * dy;

        settingsManager.setRenderConstraints(bounds);
    }
}
