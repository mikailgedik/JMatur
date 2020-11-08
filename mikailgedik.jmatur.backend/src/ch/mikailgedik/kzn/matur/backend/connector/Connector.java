package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorMandelbrotArea;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnitCPU;
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
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

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

    public Connector() {
        image = null;
        settingsManager = SettingsManager.createDefaultSettingsManager();

        units = new ArrayList<>();

        clKernelCalculate = FileManager.getFileManager().readFile("/clkernels/mandelbrot.cl");
        clKernelRender =  FileManager.getFileManager().readFile("/clkernels/colorFunctionLog.cl");
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
        assert unit != null: "No local GPU available";
        imageCreator = new ImageCreatorGPU(dataSet, unit.getDevice(), this.clKernelRender);
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

    public void setCalculatorUnits(ArrayList<CalculatorUnit> units) {
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
        //TODO
        //FileManager.getFileManager().saveImage("/home/mikail/Desktop/out/file" + (counter++) +".png", image);
    }

    static int counter = 0;

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
}
