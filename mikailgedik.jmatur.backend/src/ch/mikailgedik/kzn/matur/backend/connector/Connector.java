package ch.mikailgedik.kzn.matur.backend.connector;

import ch.mikailgedik.kzn.matur.backend.calculator.Calculable;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorMandelbrotArea;
import ch.mikailgedik.kzn.matur.backend.calculator.CalculatorUnitGPU;
import ch.mikailgedik.kzn.matur.backend.calculator.remote.CalculatorMandelbrotExternSlave;
import ch.mikailgedik.kzn.matur.backend.calculator.remote.CalculatorUnitExternMaster;
import ch.mikailgedik.kzn.matur.backend.calculator.remote.SocketAdapter;
import ch.mikailgedik.kzn.matur.backend.data.*;
import ch.mikailgedik.kzn.matur.backend.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;
import ch.mikailgedik.kzn.matur.backend.render.ColorFunction;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreator;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreatorCPU;
import ch.mikailgedik.kzn.matur.backend.render.ImageCreatorGPU;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.io.*;
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
    private int pixelWidth;

    private final double[] renderCenter;
    private double renderHeight;

    private Thread videoCreator;
    private int frames, currentFrame;

    public Connector() {
        image = null;
        settingsManager = SettingsManager.createDefaultSettingsManager();

        units = new ArrayList<>();

        clKernelCalculate = FileManager.getFileManager().readFile("/clkernels/mandelbrot.cl");
        clKernelRender =  FileManager.getFileManager().readFile("/clkernels/colorFunctionLog.cl");
        renderCenter = new double[]{0,0};
        renderHeight = 3.9;

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
        Region region = getRenderRegion();
        CalculableArea area = createCurrentArea(region);

        long maxWaitingTime =settingsManager.getI(Constants.CALCULATION_MAX_WAITING_TIME_THREADS);
        if(!area.getClusters().isEmpty()) {
            calculatorMandelbrot.calculate(area, dataSet, maxWaitingTime);
            dataSet.returnCalculableArea(area);
        }

        image = imageCreator.createScreen(this.pixelWidth, this.pixelHeight, region, maxWaitingTime);
        image = image.getScaledScreen(this.pixelWidth, this.pixelHeight);
    }

    private Region getRenderRegion() {
        double renderWidth = renderHeight * aspectRatio;
        double[] c = {
                renderCenter[0] - renderWidth/2,
                renderCenter[0] + renderWidth/2,
                renderCenter[1] - renderHeight/2,
                renderCenter[1] + renderHeight/2
        };

        return new Region(c[0], c[2], c[1], c[3]);
    }

    private CalculableArea createCurrentArea(Region region) {
        return dataSet.createCalculableArea(region, Math.min(1.0 * region.getWidth()/this.pixelWidth, 1.0 * region.getHeight()/this.pixelHeight));
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

    public double getRenderHeight() {
        return renderHeight;
    }

    public double[] getRenderCenter() {
        return renderCenter;
    }

    public void setImagePixelHeight(int h) {
        this.pixelHeight = h;
        this.pixelWidth = (int)(getAspectRatio() * h + .5);
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

    public void startVideoCreation(VideoPath path, final OutputStream out) {
        //Query items to calculate

        ArrayList<LogicalRegion> regions = new ArrayList<>();
        ArrayList<Cluster> cl = new ArrayList<>();
        frames = 0;
        loop: for(VideoPath.VideoPoint p: path) {
            frames++;
            this.setRenderParameters(p.getCenter(), p.getHeight());
            CalculableArea area = createCurrentArea(getRenderRegion());
            LogicalRegion r1 = area.getRegion();
            for(LogicalRegion r2: regions) {
                if(r1.getDepth() == r2.getDepth() &&
                        r1.getStartX() <= r2.getStartX() && r1.getStartY() <= r2.getStartY() &&
                r1.getEndX() >= r2.getEndX() && r1.getEndY() >= r2.getEndY()) {
                    continue loop;
                }
            }
            regions.add(r1);
            area.getClusters().forEach(nC -> {
                for(Cluster c: cl) {
                    if(c.getId() == nC.getId() && c.getDepth() == nC.getDepth()) {
                        return;
                    }
                }
                cl.add(nC);
            });
        }

        int tot = cl.size();
        cl.removeIf(u -> u.getValue() != null || u.getDevice() != null);
        System.out.println("Total " + tot + ", to calculate: " + cl.size());

        videoCreator = new Thread(() -> {
            long t = System.currentTimeMillis();
            long calcTime;

            long maxWaitingTime =settingsManager.getI(Constants.CALCULATION_MAX_WAITING_TIME_THREADS);
            calculatorMandelbrot.calculate(cl, dataSet, maxWaitingTime);

            calcTime = (System.currentTimeMillis() - t);
            t = System.currentTimeMillis();

            dataSet.addClusters(cl);

            int frameRate = getSettingI(Constants.RENDER_FRAMES_PER_SECOND);
            int h = getImagePixelHeight();
            int w = (int)(getAspectRatio() * h + .5);
            h += h%2;
            w += w%2;
            try {
                Process process = Runtime.getRuntime()
                        .exec("ffmpeg -f image2pipe -framerate " + frameRate
                                + " -i - -f mp4 -s " + w + "x" + h + " -pix_fmt yuv420p -c:v libx264 -movflags frag_keyframe+empty_moov pipe:1");
                Thread toFile = new Thread(() -> {
                    try {
                        process.getInputStream().transferTo(out);
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        if(out != null) {
                            try {
                                out.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
                toFile.start();
                Thread printErr = new Thread(() -> {
                    try {
                        InputStream in = process.getErrorStream();
                        int i;
                        while((i = in.read()) != -1) {
                            System.err.print((char)i);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });
                printErr.start();
                BufferedOutputStream processOut = new BufferedOutputStream(process.getOutputStream());

                this.currentFrame = 0;
                for(VideoPath.VideoPoint p: path) {
                    this.setRenderParameters(p.getCenter(), p.getHeight());
                    this.createImage();
                    ImageIO.write(getImage().toBufferedImage(), "png", processOut);
                    this.currentFrame++;
                }
                processOut.close();
                process.getOutputStream().close();

                toFile.join();
                if(process.waitFor() != 0) {
                    throw new RuntimeException("FFMPEG finished with exit code " + process.exitValue());
                }
                process.getOutputStream().close();
                process.getErrorStream().close();
                process.getInputStream().close();

                long renderTime = (System.currentTimeMillis() - t);
                System.out.println("Calculation time: " + calcTime);
                System.out.println("Rendered, time: " + renderTime);
                System.out.println("Total time: " + (calcTime + renderTime));
            } catch (IOException | InterruptedException e) {
                //throw new RuntimeException(e);
                JOptionPane.showMessageDialog(null,
                        "An error occurred during image creation\n" + e.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });

        videoCreator.setName("VideoCreator");
        videoCreator.start();
    }

    public Thread getVideoCreator() {
        return videoCreator;
    }

    public double[] getVideoCreationStage() {
        return new double[] {
                calculatorMandelbrot.getProgress(),
                1.0 * this.currentFrame / frames
        };
    }
}
