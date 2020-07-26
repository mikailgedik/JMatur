package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class MandelbrotCalculator {
    private final SettingsManager sm;
    private CalculationResult<CalculationResult.DataMandelbrot> result;
    private ExecutorService executorService;

    private int maxIter;

    private int maxDepth;
    private AtomicInteger threadCounter;
    private Phaser phaser;

    public MandelbrotCalculator(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    public CalculationResult<CalculationResult.DataMandelbrot> calculate() {
        long t = System.currentTimeMillis();
        executorService = Executors.newFixedThreadPool(sm.getI(SettingsManager.CALCULATION_MAX_THREADS));
        threadCounter = new AtomicInteger(0);
        phaser = new Phaser(1);

        int maxWaitTime = sm.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);
        maxIter = sm.getI(SettingsManager.CALCULATION_MAX_ITERATIONS);
        maxDepth = sm.getI(SettingsManager.CALCULATION_CLUSTER_INIT_DEPTH);
        int tiles = sm.getI(SettingsManager.CALCULATION_CLUSTER_TILES);

        double minx = sm.getD(SettingsManager.CALCULATION_MINX);
        double maxx = sm.getD(SettingsManager.CALCULATION_MAXX);
        double miny = sm.getD(SettingsManager.CALCULATION_MINY);
        double maxy = sm.getD(SettingsManager.CALCULATION_MAXY);

        result = new CalculationResult<>(new double[]{minx, maxx}, new double[]{miny, maxy}, tiles);

        System.out.println("Init time: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();

        Cluster<CalculationResult.DataMandelbrot> baseCluster = result.getCluster();


        phaser.register();
        executorService.submit(new ThreadCalculator(baseCluster));

        try {
            phaser.arriveAndAwaitAdvance();
            executorService.shutdown();
            if(!executorService.awaitTermination(maxWaitTime, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Threadpool exceeded max wait time (" + maxWaitTime + "ms)");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("Calc time: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();
        return result;
    }

    private class ThreadCalculator implements Runnable {
        private Cluster<CalculationResult.DataMandelbrot> cluster;
        private long calctime;

        public ThreadCalculator(Cluster<CalculationResult.DataMandelbrot> cluster) {
            this.cluster = cluster;
            this.calctime = -1;
        }

        @Override
        public void run() {
            threadCounter.incrementAndGet();

            try {
                long t = System.currentTimeMillis();

                for(int i = 0; i < cluster.getLength(); i++) {
                    double[] co = cluster.getCenterCoordinates(i);
                    cluster.setValue(i, new CalculationResult.DataMandelbrot(co[0], co[1], calc(co[0], co[1])));
                }

                if(cluster.getDepth() < maxDepth) {

                    cluster.createAllSubLevels();
                    phaser.bulkRegister(cluster.getLength());
                    for(Cluster<CalculationResult.DataMandelbrot> cl: cluster.clusterIterable()) {
                        if(cl != null) {
                            executorService.submit(new ThreadCalculator(cl));
                        }
                    }
                }

                calctime = (System.currentTimeMillis() - t);
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                threadCounter.decrementAndGet();
                phaser.arrive();
            }
        }

        private boolean calc(double x, double y) {
            double a = 0, b = 0, ta, tb;
            for(int i = 0; i < maxIter; ++i) {
                ta = a*a - b*b + x;
                tb = 2 * a * b + y;
                a = ta;
                b = tb;
                if(a*a + b*b > 4) {
                    return false;
                }
            }
            return true;
        }
    }

}
