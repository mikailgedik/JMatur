package ch.mikailgedik.kzn.matur.core.calculator;

import ch.mikailgedik.kzn.matur.core.opencl.CLDevice;

public interface CalculatorMandelbrot {
    boolean accept(Calculable cal, int[] clusterData);
    boolean accept(Calculable cal, CLDevice device, long address);
    Calculable get();
}