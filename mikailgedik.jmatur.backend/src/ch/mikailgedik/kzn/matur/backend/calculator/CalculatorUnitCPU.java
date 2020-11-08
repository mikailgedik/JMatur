package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.connector.CalculatorUnit;

@Deprecated
public class CalculatorUnitCPU implements CalculatorUnit {
    private int logicClusterWidth, logicClusterHeight, maxIterations;
    private double precision;
    private WorkThread[] threads;
    private int thread;

    private CalculatorMandelbrot calculatorMandelbrot;
    public CalculatorUnitCPU() {
        assert false: "Class out of use";
        setThreads(0);
    }

    @Override
    public void init(Init init) {
        //Do nothing
    }

    public void setThreads(int threads) {
        this.thread = threads;
    }

    @Override
    public synchronized void configureAndStart(CalculatorConfiguration configuration) {
        this.logicClusterWidth = configuration.getLogicClusterWidth();
        this.logicClusterHeight = configuration.getLogicClusterHeight();
        this.maxIterations = configuration.getMaxIterations();
        this.precision = configuration.getPrecision();
        this.calculatorMandelbrot = configuration.getCalculatorMandelbrot();

        this.threads = new WorkThread[thread];

        for(int i = 0; i < threads.length; i++) {
            threads[i] = new WorkThread();
            threads[i].start();
        }
    }

    @Override
    public void awaitTerminationAndCleanup(long maxWaitingTime) throws InterruptedException {
        for (WorkThread thread : threads) {
            thread.join(maxWaitingTime);
        }
        threads = null;
        calculatorMandelbrot = null;
    }

    @Override
    public synchronized void abort(int calcId) {
        if(threads != null) {
            for(WorkThread t: threads) {
                assert t != null;
                if(t.calculable != null && t.calculable.getCalculatorId() == calcId) {
                    t.interrupt();
                }
            }
        }
    }

    private class WorkThread extends Thread {
        private volatile Calculable calculable;

        public WorkThread() {
        }

        @Override
        public void run() {
            calculable = calculatorMandelbrot.get();
            while(calculable != null) {
                try {
                    double startX = calculable.getStartX(), startY = calculable.getStartY();
                    int[] val = new int[logicClusterHeight * logicClusterWidth];
                    for(int y = 0; y < logicClusterHeight; y++) {
                        for(int x = 0; x < logicClusterWidth; x++) {
                            val[x + y * logicClusterWidth] = calc(startX + x * precision, startY + y * precision);
                            if(Thread.interrupted()) {
                                throw new InterruptedException();
                            }
                        }
                    }
                    if(!calculatorMandelbrot.accept(calculable, val)) {
                        throw new InterruptedException();
                    }
                } catch(InterruptedException e) {
                    //Thread is only interrupted if another CalculatorUnit has finished the cluster before this thread could.
                    //Just get the next calculable
                }
                calculable = calculatorMandelbrot.get();
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
