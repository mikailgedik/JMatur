package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;

public interface CalculatorUnit {
    void addCluster(Cluster<ValueMandelbrot> cluster);
    void startCalculation(int logicClusterWidth, int logicClusterHeight,
                          int maxIterations, int depth, double precision, DataSet<ValueMandelbrot> dataSet);
    void awaitTermination(long maxWaitingTime);
}
