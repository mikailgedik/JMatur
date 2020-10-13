package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorMandelbrot;
import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.Region;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreator;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.TreeMap;

/** This class connects the frontend with the backend */
public class Connector {
    private final SettingsManager settingsManager;
    private Screen image;
    private DataSet<ValueMandelbrot> dataSet;
    private CalculatorMandelbrot calculatorMandelbrot;
    private ImageCreator<ValueMandelbrot> imageCreator;

    public Connector() {
        image = null;

        settingsManager = SettingsManager.createDefaultSettingsManager();
        dataSet = DataSet.createDataSet(128, 128, 1, 1, 4, -2, -2, 4, 3000);
        calculatorMandelbrot = new CalculatorMandelbrot();
        imageCreator = new ImageCreator<>(dataSet, ImageCreator.MANDELBROT_COLOR_FUNCTION_HSB);
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
        //TODO remove
        System.out.println("Calculate does nothing");
    }

    public synchronized void createImage() {
        int w = settingsManager.getI(Constants.RENDER_IMAGE_WIDTH);
        int h = settingsManager.getI(Constants.RENDER_IMAGE_HEIGHT);
        double[] c = settingsManager.getRenderConstraints();

        Region region = new Region(c[0], c[2], c[1], c[3]);

        CalculableArea<ValueMandelbrot> area = dataSet.createCalculableArea(region, Math.min(1.0 * region.getWidth()/w, 1.0 * region.getHeight()/h));

        if(!area.getClusters().isEmpty()) {
            calculatorMandelbrot.calculate(area, dataSet, settingsManager.getI(Constants.CALCULATION_MAX_THREADS));
            dataSet.returnCalculableArea(area);
        }

        image = imageCreator.createScreen(w, h, region);
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

    public void setImagePixelSize(int w, int h) {
        settingsManager.addSetting(Constants.RENDER_IMAGE_WIDTH, w);
        settingsManager.addSetting(Constants.RENDER_IMAGE_HEIGHT, h);
    }

}
