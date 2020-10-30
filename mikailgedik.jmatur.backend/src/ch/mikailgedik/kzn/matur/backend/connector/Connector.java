package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorMandelbrot;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnitGPU;
import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.Region;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.backend.render.ColorFunction;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreator;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreatorCPU;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreatorGPU;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.io.File;
import java.io.IOException;
import java.util.TreeMap;

/** This class connects the frontend with the backend */
public class Connector {
    private final SettingsManager settingsManager;
    private Screen image;
    private DataSet dataSet;
    private CalculatorMandelbrot calculatorMandelbrot;
    private ImageCreator imageCreator;

    public Connector() {
        image = null;

        settingsManager = SettingsManager.createDefaultSettingsManager();
        dataSet = DataSet.createDataSet(
                settingsManager.getI(Constants.DATA_LOGIC_CLUSTER_WIDTH),
                settingsManager.getI(Constants.DATA_LOGIC_CLUSTER_HEIGHT),
                settingsManager.getI(Constants.DATA_START_LOGIC_LEVEL_WIDTH),
                settingsManager.getI(Constants.DATA_START_LOGIC_LEVEL_HEIGHT),
                settingsManager.getI(Constants.DATA_CLUSTER_FACTOR),
                settingsManager.getI(Constants.DATA_REGION_START_X),
                settingsManager.getI(Constants.DATA_REGION_START_Y),
                settingsManager.getI(Constants.DATA_REGION_WIDTH),
                settingsManager.getI(Constants.CALCULATION_START_ITERATION),
                DataSet.getIterationModelFrom(settingsManager.getS(Constants.CALCULATION_ITERATION_MODEL)));

        calculatorMandelbrot = new CalculatorMandelbrot();

        CalculatorUnitGPU unit = (CalculatorUnitGPU) calculatorMandelbrot.getUnits().get(0);

        //imageCreator = new ImageCreatorCPU(dataSet, ColorFunction.mandelbrotFromString(settingsManager.getS(Constants.RENDER_COLOR_FUNCTION)));
        imageCreator = new ImageCreatorGPU(dataSet, unit.getDevice(),   "/clkernels/colorFunctionLog.cl", "colorFunctionLog");
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

    @Deprecated
    public void calculate() {
        //TODO remove
        System.out.println("Calculate does nothing");
    }

    public void saveData(File basePath) {
        try {
            dataSet.saveAll("/home/mikail/Desktop/test");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readData(File basePath) {
        try {
            dataSet.readAll("/home/mikail/Desktop/test");
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void createImage() {
        int w = settingsManager.getI(Constants.RENDER_IMAGE_WIDTH);
        int h = settingsManager.getI(Constants.RENDER_IMAGE_HEIGHT);
        int threads = settingsManager.getI(Constants.CALCULATION_MAX_THREADS);
        long maxWaitingTime =settingsManager.getI(Constants.CALCULATION_MAX_WAITING_TIME_THREADS);
        double[] c = settingsManager.getRenderConstraints();

        Region region = new Region(c[0], c[2], c[1], c[3]);

        CalculableArea area = dataSet.createCalculableArea(region, Math.min(1.0 * region.getWidth()/w, 1.0 * region.getHeight()/h));

        if(!area.getClusters().isEmpty()) {
            calculatorMandelbrot.calculate(area, dataSet, threads, maxWaitingTime);
            dataSet.returnCalculableArea(area);
        }

        image = imageCreator.createScreen(w, h, region, threads, maxWaitingTime);
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
