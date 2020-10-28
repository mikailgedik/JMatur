package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class CalculatorUnitCPU implements CalculatorUnit {
    private int threads;
    private ExecutorService service;
    private int logicClusterWidth, logicClusterHeight, maxIterations;
    private DataSet currentDataSet;
    private double precision;
    private int depth;

    private ArrayList<Cluster> clusters;

    public CalculatorUnitCPU(int threads) {
        setThreads(threads);
        this.clusters = new ArrayList<>();
    }

    public void setThreads(int threads) {
        this.threads = threads;
    }

    @Override
    public void addCluster(Cluster cluster) {
        clusters.add(cluster);
    }

    @Override
    public void startCalculation(int logicClusterWidth, int logicClusterHeight,
                                 int maxIterations, int depth, double precision, DataSet dataSet) {
        this.logicClusterWidth = logicClusterWidth;
        this.logicClusterHeight = logicClusterHeight;
        this.maxIterations = maxIterations;
        this.depth = depth;
        this.precision = precision;
        this.currentDataSet = dataSet;
        service = Executors.newFixedThreadPool(threads);

        clusters.forEach(c -> service.submit(new CalculatorUnitCPU.MT(c)));
        service.shutdown();
    }

    @Override
    public void awaitTermination(long maxWaitingTime) {
        try {
            service.awaitTermination(maxWaitingTime, TimeUnit.MILLISECONDS);
            clusters.clear();
        } catch (InterruptedException e) {
            //TODO correct behaviour when InterruptedException is thrown?
            service.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private class MT implements Runnable {
        private final Cluster c;
        private final double startX, startY;

        public MT(Cluster c) {
            this.c = c;
            double[] d = currentDataSet.levelGetStartCoordinatesOfCluster(depth, c.getId());
            this.startX = d[0];
            this.startY = d[1];
        }

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            for(int y = 0; y < logicClusterHeight; y++) {
                for(int x = 0; x < logicClusterWidth; x++) {
                    c.getValue()[x + y * logicClusterWidth] = calc(startX + x * precision, startY + y * precision);
                }
            }
        }
    }

    private int calc(double x, double y) {
        double a = 0, b = 0, ta, tb;
        for(int i = 0; i < maxIterations; ++i) {
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
