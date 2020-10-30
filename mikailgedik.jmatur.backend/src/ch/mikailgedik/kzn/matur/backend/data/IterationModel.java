package ch.mikailgedik.kzn.matur.backend.data;

import java.io.Serializable;

public interface IterationModel extends Serializable {
    int getIterations(int startIterations, int depth, double precision, double startPrecision);
}
