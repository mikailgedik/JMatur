package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.calculator.remote.CalculatorUnitExternMaster;
import ch.mikailgedik.kzn.matur.backend.calculator.remote.SocketAdapter;
import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.MemMan;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.SocketImpl;
import java.util.ArrayList;

public class CalculatorMandelbrotArea implements CalculatorMandelbrot {
    private int logicClusterWidth, logicClusterHeight, iterations;
    private DataSet currentDataSet;
    private CalculableArea area;
    private boolean[] hasDone;
    private boolean done;
    private ArrayList<CalculatorUnit> units;
    private int index;
    private final Object lock = new Object();

    public CalculatorMandelbrotArea() {
        cleanUp();
        units = new ArrayList<>();
        setDefaultUnits();
    }

    private void setDefaultUnits() {
        /*
        for(long device: OpenCLHelper.getAllAvailableDevices()) {
            units.add(new CalculatorUnitGPU(device));
        }

        units.add(new CalculatorUnitCPU());
        */

        ServerSocket server = null;
        try {
            server = new ServerSocket(5000);

            server.setSoTimeout(5000);
            units.add(new CalculatorUnitExternMaster(new SocketAdapter(server.accept())));
        } catch (IOException e) {
            if(server != null && !server.isClosed()) {
                try {
                    server.close();
                } catch (IOException ioException) {
                    ioException.printStackTrace();
                }
            }
            e.printStackTrace();
        }
        if(server != null) {
            System.out.println("New unit");
        }
    }

    public void calculate(CalculableArea area, DataSet dataSet, int threads, long maxWaitingTime) {
        setCurrentDataSet(dataSet);
        this.area = area;
        setHasDone(new boolean[area.getClusters().size()]);
        setDone(false);
        setIndex(getHasDone().length -1);
        prepare();

        getUnits().forEach(u -> {
            if(u instanceof CalculatorUnitCPU) {
                ((CalculatorUnitCPU) u).setThreads(threads);
            }
            u.configureAndStart(new CalculatorUnit.CalculatorConfiguration(getLogicClusterWidth(), getLogicClusterHeight(),
                    getIterations(), area.getPrecision(), this));
        });

        getUnits().forEach(u -> {
            try {
                u.awaitTerminationAndCleanup(maxWaitingTime);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
        cleanUp();
    }

    public boolean accept(Calculable cal, int[] clusterData) {
        synchronized (lock) {
            if(!hasDone[cal.getCalculatorId()]) {
                Cluster c = area.getClusters().get(cal.getCalculatorId());
                c.setValue(clusterData);
                acceptInternal(cal.getCalculatorId());
                return true;
            } else {
                return false;
            }
        }
    }

    public boolean accept(Calculable cal, CLDevice device, long address) {
        synchronized (lock) {
            if(!hasDone[cal.getCalculatorId()]) {
                area.getClusters().get(cal.getCalculatorId()).setDevice(device, address);
                acceptInternal(cal.getCalculatorId());
                MemMan.moveToRAM(area.getClusters().get(cal.getCalculatorId()), currentDataSet.getLogicClusterHeight() * currentDataSet.getLogicClusterWidth());
                return true;
            } else {
                return false;
            }
        }
    }

    private void acceptInternal(int id) {
        hasDone[id] = true;
        units.forEach(u -> u.abort(id));
        done = true;
        for(boolean b: hasDone) {
            if(!b) {
                done = false;
                break;
            }
        }
    }

    public Calculable get() {
        synchronized (lock) {
            if(done) {
                return null;
            }
            do {
                index++;
                index%=hasDone.length;
            } while(hasDone[index]);
            double[] coo = currentDataSet.levelGetStartCoordinatesOfCluster(area.getDepth(),
                    area.getClusters().get(index).getId());
            return new Calculable(index, coo[0], coo[1]);
        }
    }

    private void cleanUp() {
        currentDataSet = null;
        logicClusterWidth = 0;
        logicClusterHeight = 0;
        iterations = 0;
        area = null;
    }

    private void prepare() {
        logicClusterWidth = currentDataSet.getLogicClusterWidth();
        logicClusterHeight = currentDataSet.getLogicClusterHeight();
        iterations = currentDataSet.levelGetIterationsForDepth(area.getDepth());
    }

    public ArrayList<CalculatorUnit> getUnits() {
        return this.units;
    }

    public int getLogicClusterWidth() {
        return logicClusterWidth;
    }

    public int getLogicClusterHeight() {
        return logicClusterHeight;
    }

    public int getIterations() {
        return iterations;
    }

    public DataSet getCurrentDataSet() {
        return currentDataSet;
    }

    public CalculableArea getArea() {
        return area;
    }

    public boolean[] getHasDone() {
        return hasDone;
    }

    public boolean isDone() {
        return done;
    }

    public int getIndex() {
        return index;
    }

    public Object getLock() {
        return lock;
    }

    public void setLogicClusterWidth(int logicClusterWidth) {
        this.logicClusterWidth = logicClusterWidth;
    }

    public void setLogicClusterHeight(int logicClusterHeight) {
        this.logicClusterHeight = logicClusterHeight;
    }

    public void setIterations(int iterations) {
        this.iterations = iterations;
    }

    public void setCurrentDataSet(DataSet currentDataSet) {
        this.currentDataSet = currentDataSet;
    }

    public void setArea(CalculableArea area) {
        this.area = area;
    }

    public void setHasDone(boolean[] hasDone) {
        this.hasDone = hasDone;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void setUnits(ArrayList<CalculatorUnit> units) {
        this.units = units;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
