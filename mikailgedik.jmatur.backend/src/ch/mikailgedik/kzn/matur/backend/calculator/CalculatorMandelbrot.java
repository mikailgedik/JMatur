package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.CalculableArea;
import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CalculatorMandelbrot {
    private ExecutorService service;
    private int logicClusterWidth, logicClusterHeight, iterations;
    private DataSet<ValueMandelbrot> currentDataSet;
    private CalculableArea<ValueMandelbrot> area;

    public CalculatorMandelbrot() {
        cleanUp();
    }


    public void calculate(CalculableArea<ValueMandelbrot> area, DataSet<ValueMandelbrot> dataSet, int t) {
        currentDataSet = dataSet;
        this.area = area;
        prepare(t);

        area.forEach(c -> service.submit(new MT(c)));
        service.shutdown();

        try {
            service.awaitTermination(100000, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            service.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            cleanUp();
        }
    }

    private void cleanUp() {
        service = null;
        currentDataSet = null;
        logicClusterWidth = 0;
        logicClusterHeight = 0;
        iterations = 0;
        area = null;
    }

    private void prepare(int t) {
        assert service == null;
        service = Executors.newFixedThreadPool(t);
        logicClusterWidth = currentDataSet.getLogicClusterWidth();
        logicClusterHeight = currentDataSet.getLogicClusterHeight();
        iterations = currentDataSet.levelGetIterationsForDepth(area.getDepth());
    }

    private class MT implements Runnable {
        private final Cluster<ValueMandelbrot> c;
        private final double startX, startY;

        public MT(Cluster<ValueMandelbrot> c) {
            this.c = c;
            double[] d = currentDataSet.levelGetStartCoordinatesOfCluster(area.getDepth(), c.getId());
            this.startX = d[0];
            this.startY = d[1];
        }

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            for(int y = 0; y < logicClusterHeight; y++) {
                for(int x = 0; x < logicClusterWidth; x++) {
                    c.getValue()[x + y * logicClusterWidth] =
                            new ValueMandelbrot(calc(startX + x * area.getPrecision(), startY + y * area.getPrecision()));
                }
            }
        }
    }

    private int calc(double x, double y) {
        double a = 0, b = 0, ta, tb;
        for(int i = 0; i < iterations; ++i) {
            ta = a*a - b*b + x;
            tb = 2 * a * b + y;
            a = ta;
            b = tb;
            if(a*a + b*b > 4) {
                return i;
            }
        }
        return -1;
    }
}
