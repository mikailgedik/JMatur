package ch.mikailgedik.kzn.matur.backend.data.value;

public class ValueMandelbrot implements Value {
    private final int value;

    public ValueMandelbrot(int value) {
        this.value = value;
    }

    @Override
    public int getValue() {
        return value;
    }
}
