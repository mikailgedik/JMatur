package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.MemMan;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;
import ch.mikailgedik.kzn.matur.backend.opencl.OpenCLHelper;

import java.util.ArrayList;

public class CalculatorMandelbrot {
    private int logicClusterWidth, logicClusterHeight, iterations;
    private DataSet currentDataSet;
    private CalculableArea area;
    private boolean[] hasDone;
    private boolean done;
    private ArrayList<CalculatorUnit> units;
    private int index;
    private final Object lock = new Object();
    public CalculatorMandelbrot() {
        cleanUp();
        units = new ArrayList<>();
        setDefaultUnits();
    }

    private void setDefaultUnits() {
        for(long device: OpenCLHelper.getAllAvailableDevices()) {
            units.add(new CalculatorUnitGPU(device));
        }

        units.add(new CalculatorUnitCPU());
    }

    public void calculate(CalculableArea area, DataSet dataSet, int threads, long maxWaitingTime) {
        currentDataSet = dataSet;
        this.area = area;
        hasDone = new boolean[area.getClusters().size()];
        done = false;
        index = hasDone.length -1;
        prepare();

        units.forEach(u -> {
            if(u instanceof CalculatorUnitCPU) {
                ((CalculatorUnitCPU) u).setThreads(threads);
            }
            u.configureAndStart(logicClusterWidth, logicClusterHeight, iterations, area.getDepth(), area.getPrecision(), dataSet, this);
        });

        units.forEach(u -> {
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
                area.getClusters().get(cal.getCalculatorId()).setValue(clusterData);
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
            return new Calculable(index, area.getClusters().get(index).getId());
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
}
