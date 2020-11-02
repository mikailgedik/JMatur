package ch.mikailgedik.kzn.matur.backend.calculator;

import java.io.Serializable;

public class Calculable implements Serializable {
    private final int calculatorId;
    private final double startX, startY;

    public Calculable(int calculatorId, double startX, double startY) {
        this.calculatorId = calculatorId;
        this.startX = startX;
        this.startY = startY;
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

    public static class CalculableResult extends Calculable {
        private final int[] data;

        public CalculableResult(int calculatorId, double startX, double startY, int[] data) {
            super(calculatorId, startX, startY);
            this.data = data;
        }

        public int[] getData() {
            return data;
        }
    }

}
