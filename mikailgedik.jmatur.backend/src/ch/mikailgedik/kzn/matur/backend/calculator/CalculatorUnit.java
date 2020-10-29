package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.DataSet;

public interface CalculatorUnit {
    void configureAndStart(int logicClusterWidth, int logicClusterHeight,
                           int maxIterations, int depth, double precision, DataSet dataSet, CalculatorMandelbrot calculatorMandelbrot);
    void awaitTerminationAndCleanup(long maxWaitingTime) throws InterruptedException;
    void abort(int calcId);
}
