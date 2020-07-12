package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

public class MandelbrotCalculator {
    private SettingsManager sm;

    public MandelbrotCalculator(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    public CalculationResult<CalculationResult.DataMandelbrot> calculate() {
        long t = System.currentTimeMillis();
        ThreadCalculator[] threads = new ThreadCalculator[sm.getI(SettingsManager.CALCULATION_MAX_THREADS)];
        int maxWaitTime = sm.getI(SettingsManager.CALCULATION_MAX_WAITING_TIME_THREADS);
        int maxIter = sm.getI(SettingsManager.CALCULATION_MAX_ITERATIONS);
        double minx = sm.getD(SettingsManager.CALCULATION_MINX);
        double maxx = sm.getD(SettingsManager.CALCULATION_MAXX);
        double miny = sm.getD(SettingsManager.CALCULATION_MINY);
        double maxy = sm.getD(SettingsManager.CALCULATION_MAXY);

        double xSampleSize = (maxx - minx) / (sm.getI(SettingsManager.CALCULATION_TICKX) + 1);
        double ySampleSize = (maxy - miny) / (sm.getI(SettingsManager.CALCULATION_TICKY) + 1);

        double yFrac = (maxy - miny) / (threads.length);

        for(int i = 0; i < threads.length; i++) {
            threads[i] = new ThreadCalculator(minx,miny + i * yFrac ,xSampleSize,ySampleSize, maxx,miny + yFrac * (1 + i), maxIter);
            threads[i].start();
        }

        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();

        CalculationResult<CalculationResult.DataMandelbrot> res = new CalculationResult<>(new double[]{minx, maxx}, new double[]{miny, maxy},
                (int) Math.ceil((maxx - minx) * (maxy- miny)));

        try {
            for (ThreadCalculator thread : threads) {
                thread.join(maxWaitTime);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("This thread should not be interrupted", e);
        }

        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();

        for (ThreadCalculator thread : threads) {
            res.addOtherResult(thread.getResult());
        }

        System.out.println(System.currentTimeMillis() - t);
        t = System.currentTimeMillis();
        return res;
    }

    private static class ThreadCalculator extends Thread {
        private double xstart, ystart, xstep, ystep, xend, yend;
        private CalculationResult<CalculationResult.DataMandelbrot> result;
        private int maxIterations;

        public ThreadCalculator(double xstart, double ystart, double xstep, double ystep, double xend, double yend, int maxIterations) {
            this.xstart = xstart;
            this.ystart = ystart;
            this.xstep = xstep;
            this.ystep = ystep;
            this.xend = xend;
            this.yend = yend;
            this.maxIterations = maxIterations;
            result = new CalculationResult<>(new double[]{xstart, xend}, new double[]{ystart, yend},
                    (int) Math.ceil((xend - xstart) * (yend- ystart)));
        }

        @Override
        public void run() {
            for(double x = xstart; x < xend; x += xstep) {
                for(double y = ystart; y < yend; y += ystep) {
                    result.add(new CalculationResult.DataMandelbrot(x, y, calc(x,y)));
                }
            }
        }

        private boolean calc(double x, double y) {
            double a = 0, b = 0, ta, tb;
            for(int i = 0; i < maxIterations; ++i) {
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

        public CalculationResult<CalculationResult.DataMandelbrot> getResult() {
            return result;
        }
    }

}
