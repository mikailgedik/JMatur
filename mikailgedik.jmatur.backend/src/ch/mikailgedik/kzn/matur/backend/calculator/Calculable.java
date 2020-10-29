package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.Cluster;

public class Calculable {
    private final int calculatorId, clusterId;

    public Calculable(int calculatorId, int clusterId) {
        this.calculatorId = calculatorId;
        this.clusterId = clusterId;
    }

    public int getCalculatorId() {
        return calculatorId;
    }

    public int getClusterId() {
        return clusterId;
    }
}
