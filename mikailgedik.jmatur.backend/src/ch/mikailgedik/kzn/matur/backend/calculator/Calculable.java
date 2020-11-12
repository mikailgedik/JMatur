package ch.mikailgedik.kzn.matur.backend.calculator;

import java.io.Serializable;

public class Calculable implements Serializable {
    private final int calculatorId, maxIterations;
    private final double startX, startY, precision;

    public Calculable(int calculatorId, double startX, double startY, int maxIterations, double precision) {
        this.calculatorId = calculatorId;
        this.startX = startX;
        this.startY = startY;
        this.maxIterations = maxIterations;
        this.precision = precision;
    }

    public int getCalculatorId() {
        return calculatorId;
    }

    public double getStartX() {
        return startX;
    }

    public double getStartY() {
        return startY;
    }

    public int getMaxIterations() {
        return maxIterations;
    }

    public double getPrecision() {
        return precision;
    }

    public static class CalculableResult extends Calculable {
        private final int[] data;

        public CalculableResult(int calculatorId, double startX, double startY, int maxIterations, double precision, int[] data) {
            super(calculatorId, startX, startY,maxIterations, precision);
            this.data = data;
        }

        public int[] getData() {
            return data;
        }
    }

}
