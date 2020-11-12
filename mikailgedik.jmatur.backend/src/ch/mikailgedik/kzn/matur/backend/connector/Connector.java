package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorMandelbrotArea;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnitGPU;
import ch.mikailgedik.kzn.matur.backend.calculator.remote.CalculatorMandelbrotExternSlave;
import ch.mikailgedik.kzn.matur.backend.calculator.remote.CalculatorUnitExternMaster;
import ch.mikailgedik.kzn.matur.backend.calculator.remote.SocketAdapter;
import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.Region;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;
import ch.mikailgedik.kzn.matur.backend.render.ColorFunction;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreator;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreatorCPU;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreatorGPU;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.function.Consumer;

/** This class connects the frontend with the backend */
public class Connector {
    private final SettingsManager settingsManager;
    private Screen image;
    private DataSet dataSet;
    private CalculatorMandelbrotArea calculatorMandelbrot;
    private CalculatorMandelbrotExternSlave calculatorMandelbrotExternSlave;
    private ImageCreator imageCreator;
    private boolean isSlave;
    private Thread thread;
    private ArrayList<CalculatorUnit> units;
    private String clKernelCalculate, clKernelRender;

    private double aspectRatio;
    private int pixelHeight;
    private final double[] renderCenter;
    private double renderHeight;

    private Thread videoCreator;
    private int[] videoCreationStage;

    public Connector() {
        image = null;
        settingsManager = SettingsManager.createDefaultSettingsManager();

        units = new ArrayList<>();

        clKernelCalculate = FileManager.getFileManager().readFile("/clkernels/mandelbrot.cl");
        clKernelRender =  FileManager.getFileManager().readFile("/clkernels/colorFunctionLog.cl");
        renderCenter = new double[2];
        renderHeight = 4;

        videoCreationStage = new int[2];
        videoCreator = null;
    }

    public void initSlave() throws IOException {
        isSlave = true;
        calculatorMandelbrotExternSlave = new CalculatorMandelbrotExternSlave(
                settingsManager.getS(Constants.CONNECTION_HOST), settingsManager.getI(Constants.CONNECTION_PORT), units);
    }

    public void initMaster(int renderDevice) {
        isSlave = false;
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

        aspectRatio = settingsManager.getD(Constants.RENDER_ASPECT_RATIO);

        calculatorMandelbrot = new CalculatorMandelbrotArea(units, new CalculatorUnit.Init(clKernelCalculate));
        CalculatorUnitGPU unit = null;
        if(renderDevice == -1) {
            for (CalculatorUnit u: units) {
                if(u instanceof CalculatorUnitGPU) {
                    unit = (CalculatorUnitGPU) u;
                    break;
                }
            }
        } else {
            unit = (CalculatorUnitGPU) units.get(renderDevice);
        }
        if(unit == null) {
            System.out.println("Warning: using CPU to render");
            imageCreator = new ImageCreatorCPU(dataSet, ColorFunction.MANDELBROT_COLOR_FUNCTION_HSB);
        } else {
            imageCreator = new ImageCreatorGPU(dataSet, unit.getDevice(), this.clKernelRender);
        }
    }

    public void sendAvailableUnitsTo(Consumer<CalculatorUnit> receiver) {
        for(long device: OpenCLHelper.getAllAvailableDevices()) {
            receiver.accept(new CalculatorUnitGPU(device));
        }
        if(getSettingI(Constants.CONNECTION_ACCEPTS_EXTERNAL) == 1) {
            thread = new Thread(() -> {
                try {
                    ServerSocket serverSocket = new ServerSocket(settingsManager.getI(Constants.CONNECTION_PORT));
                    while(true) {
                        receiver.accept(new CalculatorUnitExternMaster(new SocketAdapter(serverSocket.accept())));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            thread.setName("Socket acceptor");
            thread.start();
        }
    }

    public void useCalculatorUnits(ArrayList<CalculatorUnit> units) {
        if(thread != null) {
            thread.interrupt();
        }
        this.units = units;
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
            dataSet.saveAll(basePath.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void readData(File basePath) {
        try {
            dataSet.readAll(basePath.getAbsolutePath());
        } catch (IOException | ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public synchronized void createImage() {
        int h = this.pixelHeight;
        // The +.5 is for rounding
        int w = (int) (this.pixelHeight * aspectRatio + .5);
        double renderWidth = renderHeight * aspectRatio;

        long maxWaitingTime =settingsManager.getI(Constants.CALCULATION_MAX_WAITING_TIME_THREADS);
        double[] c = {
                renderCenter[0] - renderWidth/2,
                renderCenter[0] + renderWidth/2,
                renderCenter[1] - renderHeight/2,
                renderCenter[1] + renderHeight/2
        };

        Region region = new Region(c[0], c[2], c[1], c[3]);

        CalculableArea area = dataSet.createCalculableArea(region, Math.min(1.0 * region.getWidth()/w, 1.0 * region.getHeight()/h));

        if(!area.getClusters().isEmpty()) {
            calculatorMandelbrot.calculate(area, dataSet, maxWaitingTime);
            dataSet.returnCalculableArea(area);
        }

        image = imageCreator.createScreen(w, h, region, maxWaitingTime);
        image = image.getScaledScreen(w, h);
    }

    public Screen getImage() {
        return image;
    }

    public boolean isSlave() {
        return isSlave;
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
        double factor = Math.exp(settingsManager.getD(Constants.RENDER_ZOOM_FACTOR) * ticks);
        renderHeight *= factor;
    }

    /** @param dx relative distance
     * @param dy relative distance**/
    public void moveRenderZone(double dx, double dy) {
        double renderWidth = renderHeight * aspectRatio;
        renderCenter[0] += dx * renderWidth;
        renderCenter[1] += dy * renderHeight;
    }

    public void setRenderParameters(double[] center, double renderHeight) {
        this.renderCenter[0] = center[0];
        this.renderCenter[1] = center[1];
        this.renderHeight = renderHeight;
    }

    public double[] relativeToAbsoluteCenter(double[] center) {
        double renderWidth = renderHeight * aspectRatio;
        double[] ret = this.renderCenter.clone();
        ret[0] -= renderWidth / 2;
        ret[1] -= renderHeight / 2;
        ret[0] += renderWidth * center[0];
        ret[1] += renderHeight * center[1];

        return ret;
    }

    public double getRenderHeight() {
        return renderHeight;
    }

    public double[] getRenderCenter() {
        return renderCenter;
    }

    public void setImagePixelHeight(int h) {
        this.pixelHeight = h;
    }

    public int getImagePixelHeight() {
        return pixelHeight;
    }

    public double getAspectRatio() {
        return aspectRatio;
    }

    public String getClKernelCalculate() {
        return clKernelCalculate;
    }

    public void setClKernelCalculate(String clKernelCalculate) {
        this.clKernelCalculate = clKernelCalculate;
    }

    public String getClKernelRender() {
        return clKernelRender;
    }

    public void setClKernelRender(String clKernelRender) {
        this.clKernelRender = clKernelRender;
    }

    public void startVideoCreation(VideoPath path, OutputStream out) {
        this.videoCreationStage[0] = 0;
        this.videoCreationStage[1] = 0;
        //Query items to calculate
        ArrayList<Calculable> cl;

        videoCreator = new Thread(() -> {


        });
        videoCreator.setName("VideoCreator");
        videoCreator.start();
    }

    public int[] getVideoCreationStage() {
        return videoCreationStage;
    }
}
