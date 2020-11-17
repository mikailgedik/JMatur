package ch.mikailgedik.kzn.matur.core.connector;

import ch.mikailgedik.kzn.matur.core.calculator.CalculatorMandelbrot;

import java.io.Serializable;

public interface CalculatorUnit {
    void init(Init init);
    void configureAndStart(CalculatorConfiguration configuration);
    void awaitTerminationAndCleanup(long maxWaitingTime) throws InterruptedException;
    void abort(int calcId);

    class Init implements Serializable {
        private final String clKernelSource;

        public Init(String clKernelSource) {
            this.clKernelSource = clKernelSource;
        }

        public String getClKernelSource() {
            return clKernelSource;
        }
    }

    class CalculatorConfiguration implements Serializable {
        private final int logicClusterWidth, logicClusterHeight;
        private transient CalculatorMandelbrot calculatorMandelbrot;

        public CalculatorConfiguration(int logicClusterWidth, int logicClusterHeight, CalculatorMandelbrot calculatorMandelbrot) {
            this.logicClusterWidth = logicClusterWidth;
            this.logicClusterHeight = logicClusterHeight;
            this.calculatorMandelbrot = calculatorMandelbrot;
        }

        public int getLogicClusterWidth() {
            return logicClusterWidth;
        }

        public int getLogicClusterHeight() {
            return logicClusterHeight;
        }

        public CalculatorMandelbrot getCalculatorMandelbrot() {
            return calculatorMandelbrot;
        }

        public void setCalculatorMandelbrot(CalculatorMandelbrot calculatorMandelbrot) {
            this.calculatorMandelbrot = calculatorMandelbrot;
        }
    }

}
