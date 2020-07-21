package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

import java.util.Stack;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class MandelbrotCalculator {
    private SettingsManager sm;
    private CalculationResult<CalculationResult.DataMandelbrot> result;
    private ExecutorService executorService;

    private int maxIter;
    double xSampleSize, ySampleSize;
    public MandelbrotCalculator(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    public CalculationResult<CalculationResult.DataMandelbrot> calculate() {
        long t = System.currentTimeMillis();
        executorService = Executors.newFixedThreadPool(sm.getI(SettingsManager.CALCULATION_MAX_THREADS));
        int maxWaitTime = sm.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);
        maxIter = sm.getI(SettingsManager.CALCULATION_MAX_ITERATIONS);
        double minx = sm.getD(SettingsManager.CALCULATION_MINX);
        double maxx = sm.getD(SettingsManager.CALCULATION_MAXX);
        double miny = sm.getD(SettingsManager.CALCULATION_MINY);
        double maxy = sm.getD(SettingsManager.CALCULATION_MAXY);

        result = new CalculationResult<>(new double[]{minx, maxx}, new double[]{miny, maxy},
                (int) Math.ceil((maxx - minx) * (maxy - miny)));

        xSampleSize = (maxx - minx) / (sm.getI(SettingsManager.CALCULATION_TICKX) + 1);
        ySampleSize = (maxy - miny) / (sm.getI(SettingsManager.CALCULATION_TICKY) + 1);

        System.out.println("Init time: " + (System.currentTimeMillis() - t) + "ms");
        t = System.currentTimeMillis();

        for (CalculationResult.Cluster<CalculationResult.DataMandelbrot> c: result) {
            executorService.submit(new ThreadCalculator(c));
        }

        executorService.shutdown();
        try {
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
        private CalculationResult.Cluster<CalculationResult.DataMandelbrot> cluster;
        private long calctime;
        public ThreadCalculator(CalculationResult.Cluster<CalculationResult.DataMandelbrot> cluster) {
            this.cluster = cluster;
            this.calctime = -1;
        }

        @Override
        public void run() {
            long t = System.currentTimeMillis();
            for(double x = cluster.getXstart(); x < cluster.getXend(); x += xSampleSize) {
                for(double y = cluster.getYstart(); y < cluster.getYend(); y += ySampleSize) {
                    cluster.add(new CalculationResult.DataMandelbrot(x, y, calc(x,y)));
                }
            }
            calctime = (System.currentTimeMillis() - t);
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
