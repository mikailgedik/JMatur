package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;

import java.util.ArrayList;

public class CalculatorMandelbrot {
    private int logicClusterWidth, logicClusterHeight, iterations;
    private DataSet<ValueMandelbrot> currentDataSet;
    private CalculableArea<ValueMandelbrot> area;
    private ArrayList<CalculatorUnit> units;

    public CalculatorMandelbrot() {
        cleanUp();
        units = new ArrayList<>();
        setDefaultUnits();
    }

    private void setDefaultUnits() {
        //units.add(new CalculatorUnitCPU(1));
        for(long device: CalculatorUnitGPU.getAllAvailableDevices()) {
            CalculatorUnitGPU gpu = new CalculatorUnitGPU(device);
            units.add(gpu);
            //System.out.println(gpu);
        }
    }

    public void calculate(CalculableArea<ValueMandelbrot> area, DataSet<ValueMandelbrot> dataSet, int threads, long maxWaitingTime) {
        currentDataSet = dataSet;
        this.area = area;
        prepare();

        int unit = 0;
        for(Cluster<ValueMandelbrot> c: area) {
            units.get(unit).addCluster(c);
            unit++;
            unit %= units.size();
        }


        units.forEach(u -> {
            if(u instanceof CalculatorUnitCPU) {
                ((CalculatorUnitCPU) u).setThreads(threads);
            }
            u.startCalculation(logicClusterWidth, logicClusterHeight, iterations, area.getDepth(), area.getPrecision(), dataSet);
        });
        units.forEach(u -> u.awaitTermination(maxWaitingTime));
        cleanUp();
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
}
