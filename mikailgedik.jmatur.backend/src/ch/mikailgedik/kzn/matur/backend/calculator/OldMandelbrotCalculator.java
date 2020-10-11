package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.render.ImageResult;
import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class OldMandelbrotCalculator {
    private final SettingsManager sm;
    private OldCalculationResult.CalculationResultMandelbrot result;
    private ExecutorService executorService;

    private int maxIter;
    private int maxDepth;
    private AtomicInteger threadCounter;

    public OldMandelbrotCalculator(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    public OldCalculationResult.CalculationResultMandelbrot calculateBase() {
        long t = System.currentTimeMillis();

        initServices();

        int maxWaitTime = sm.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);
        maxIter = sm.getI(SettingsManager.CALCULATION_MAX_ITERATIONS);
        maxDepth = sm.getI(SettingsManager.CALCULATION_CLUSTER_INIT_DEPTH);
        int tiles = sm.getI(SettingsManager.CALCULATION_CLUSTER_TILES);

        double minx = sm.getD(SettingsManager.CALCULATION_MINX);
        double maxx = sm.getD(SettingsManager.CALCULATION_MAXX);
        double miny = sm.getD(SettingsManager.CALCULATION_MINY);
        double maxy = sm.getD(SettingsManager.CALCULATION_MAXY);

        result = new OldCalculationResult.CalculationResultMandelbrot(new double[]{minx, maxx}, new double[]{miny, maxy}, tiles);

        System.out.println("Init time: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();

        result.ensureDepth(maxDepth);

        for(int i = 0; i <= maxDepth; i++) {
            OldCalculationResult.Level<OldDataMandelbrot> l = result.getLevel(i);
            for(int j = 0; j < l.totalElements(); j++) {
                executorService.submit(new ThreadCalculator(i, j));
            }
        }

        try {
            executorService.shutdown();
            if(!executorService.awaitTermination(maxWaitTime, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Threadpool exceeded max wait time (" + maxWaitTime + "ms)");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            deleteServices();
        }


        System.out.println("Calc time: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();

        return result;
    }

    public void addTargetsFromImageResult(ImageResult<OldDataMandelbrot> imageResult) {
        initServices();
        result.ensureDepth(imageResult.getDepth());
        int maxWaitTime = sm.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);

        imageResult.forEachIncomplete(this::addTarget);

        try {
            executorService.shutdown();
            if(!executorService.awaitTermination(maxWaitTime, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Threadpool exceeded max wait time (" + maxWaitTime + "ms)");
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        } finally {
            deleteServices();
        }
        //assert false;
    }

    private void addTarget(Object o) {
        int[] param = (int[])o;
        executorService.submit(new ThreadCalculator(param[0], param[1] + param[2] * result.getLevel(param[0]).getSize()));
    }

    private void initServices() {
        assert executorService == null;
        assert threadCounter == null;

        executorService = Executors.newFixedThreadPool(sm.getI(SettingsManager.CALCULATION_MAX_THREADS));
        threadCounter = new AtomicInteger(0);
    }

    private void deleteServices() {
        executorService = null;
        threadCounter = null;
    }

    private class ThreadCalculator implements Runnable {
        private final int depth, cluster;
        private long calctime;

        public ThreadCalculator(int depth, int cluster) {
            this.cluster = cluster;
            this.depth = depth;
            this.calctime = -1;
        }

        @Override
        public void run() {
            threadCounter.incrementAndGet();

            try {
                long t = System.currentTimeMillis();
                OldDataMandelbrot[] data = result.getCluster(depth, cluster);

                double[] v;
                for(int i = 0; i < data.length; i++) {
                    v = result.centerCoordinates(depth, cluster, i);
                    data[i] = new OldDataMandelbrot(v[0], v[1], calc(v[0], v[1]));
                }


                calctime = (System.currentTimeMillis() - t);
            } catch (RuntimeException e) {
                e.printStackTrace();
            } finally {
                threadCounter.decrementAndGet();
            }
        }

        private int calc(double x, double y) {
            double a = 0, b = 0, ta, tb;
            for(int i = 0; i < maxIter; ++i) {
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

}
