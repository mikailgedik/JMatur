package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.connector.CalculatorUnit;
import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.MemMan;
import ch.mikailgedik.kzn.matur.backend.opencl.CLDevice;

import java.util.ArrayList;

public class CalculatorMandelbrotArea implements CalculatorMandelbrot {
    private int logicClusterWidth, logicClusterHeight;
    private DataSet currentDataSet;
    private boolean[] hasDone;
    private boolean done;
    private ArrayList<Cluster> clusters;
    private ArrayList<CalculatorUnit> units;
    private int index;
    private final Object lock = new Object();

    public CalculatorMandelbrotArea(ArrayList<CalculatorUnit> units, CalculatorUnit.Init init) {
        cleanUp();
        this.units = units;
        units.forEach(u -> u.init(init));
    }

    public void calculate(CalculableArea area, DataSet dataSet, long maxWaitingTime) {
        calculate(area.getClusters(), dataSet, maxWaitingTime);
    }

    public void calculate(ArrayList<Cluster> list, DataSet dataSet, long maxWaitingTime) {
        setCurrentDataSet(dataSet);
        this.clusters = list;

        setHasDone(new boolean[clusters.size()]);
        setDone(false);
        setIndex(getHasDone().length -1);
        prepare();

        getUnits().forEach(u -> u.configureAndStart(new CalculatorUnit.CalculatorConfiguration(getLogicClusterWidth(), getLogicClusterHeight(),this)));

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
                Cluster c = clusters.get(cal.getCalculatorId());
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
                clusters.get(cal.getCalculatorId()).setDevice(device, address);
                acceptInternal(cal.getCalculatorId());
                MemMan.moveToRAM(clusters.get(cal.getCalculatorId()), currentDataSet.getLogicClusterHeight() * currentDataSet.getLogicClusterWidth());
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

    public double getProgress() {
        if(hasDone == null) {
            return 1;
        }
        int c = 0;
        for(boolean b: hasDone) {
            if(b) {
                c++;
            }
        }
        return 1.0 * c / hasDone.length;
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
            Cluster c = clusters.get(index);
            double[] coo = currentDataSet.levelGetStartCoordinatesOfCluster(c.getDepth(),
                    c.getId());
            return new Calculable(index, coo[0], coo[1], c.getIterations(), currentDataSet.levelGetPrecisionAtDepth(c.getDepth()));
        }
    }

    private void cleanUp() {
        hasDone = null;
        currentDataSet = null;
        logicClusterWidth = 0;
        logicClusterHeight = 0;
    }

    private void prepare() {
        logicClusterWidth = currentDataSet.getLogicClusterWidth();
        logicClusterHeight = currentDataSet.getLogicClusterHeight();
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

    public DataSet getCurrentDataSet() {
        return currentDataSet;
    }

    public void setCurrentDataSet(DataSet currentDataSet) {
        this.currentDataSet = currentDataSet;
    }

    public boolean[] getHasDone() {
        return hasDone;
    }

    public boolean isDone() {
        return done;
    }

    public void setHasDone(boolean[] hasDone) {
        this.hasDone = hasDone;
    }

    public void setDone(boolean done) {
        this.done = done;
    }

    public void setIndex(int index) {
        this.index = index;
    }
}
