package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataSet;

public interface CalculatorUnit {
    void addCluster(Cluster cluster);
    void startCalculation(int logicClusterWidth, int logicClusterHeight,
                          int maxIterations, int depth, double precision, DataSet dataSet);
    void awaitTermination(long maxWaitingTime);
}
