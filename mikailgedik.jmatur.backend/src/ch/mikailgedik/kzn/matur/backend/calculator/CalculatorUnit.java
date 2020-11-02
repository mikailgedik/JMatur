package ch.mikailgedik.kzn.matur.backend.calculator;

import java.io.Serializable;

public interface CalculatorUnit {
    void configureAndStart(CalculatorConfiguration configuration);
    void awaitTerminationAndCleanup(long maxWaitingTime) throws InterruptedException;
    void abort(int calcId);

    class CalculatorConfiguration implements Serializable {
        private final int logicClusterWidth, logicClusterHeight, maxIterations;
        private final double precision;
        private transient CalculatorMandelbrot calculatorMandelbrot;

        public CalculatorConfiguration(int logicClusterWidth, int logicClusterHeight,
                                       int maxIterations, double precision, CalculatorMandelbrot calculatorMandelbrot) {
            this.logicClusterWidth = logicClusterWidth;
            this.logicClusterHeight = logicClusterHeight;
            this.maxIterations = maxIterations;
            this.precision = precision;
            this.calculatorMandelbrot = calculatorMandelbrot;
        }

        public int getLogicClusterWidth() {
            return logicClusterWidth;
        }

        public int getLogicClusterHeight() {
            return logicClusterHeight;
        }

        public int getMaxIterations() {
            return maxIterations;
        }

        public double getPrecision() {
            return precision;
        }

        public CalculatorMandelbrot getCalculatorMandelbrot() {
            return calculatorMandelbrot;
        }

        public void setCalculatorMandelbrot(CalculatorMandelbrot calculatorMandelbrot) {
            this.calculatorMandelbrot = calculatorMandelbrot;
        }
    }

}
