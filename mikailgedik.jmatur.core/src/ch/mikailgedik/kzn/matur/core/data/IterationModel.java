package ch.mikailgedik.kzn.matur.core.data;

import java.io.Serializable;

public interface IterationModel extends Serializable {
    int getIterations(int startIterations, int depth, double precision, double startPrecision);
}
