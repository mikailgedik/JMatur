package ch.mikailgedik.kzn.matur.backend.calculator;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.function.Predicate;

public class CalculationResult<T> implements Iterable<T>{
    private ArrayList<T> data;

    public CalculationResult(double[] xBounds, double[] yBounds, int initialSize) {
        //TODO use bounds
        data = new ArrayList<>(initialSize);
    }

    public void add(T t) {
        data.add(t);
    }

    public T get(int i) {
        return data.get(i);
    }

    public boolean removeIf(Predicate<? super T> predicate) {
        return data.removeIf(predicate);
    }

    public int size() {
        return data.size();
    }

    public void addOtherResult(CalculationResult<T> other) {
        this.data.addAll(other.data);
    }

    @Override
    public Iterator<T> iterator() {
        return data.iterator();
    }

    public static class DataMandelbrot {
        private double x, y;
        private boolean value;

        public DataMandelbrot(double x, double y, boolean value) {
            this.x = x;
            this.y = y;
            this.value = value;
        }

        public double getX() {
            return x;
        }

        public double getY() {
            return y;
        }

        public boolean getValue() {
            return value;
        }
    }
}
