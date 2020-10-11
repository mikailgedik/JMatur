package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.data.Cluster;
import ch.mikailgedik.kzn.matur.backend.data.DataAcceptor;
import ch.mikailgedik.kzn.matur.backend.data.value.Value;

public class Calculator<T extends Value> extends DataAcceptor<T> {
    @Override
    public void accept(Cluster<T> t, double startX, double startY) {

    }
}
