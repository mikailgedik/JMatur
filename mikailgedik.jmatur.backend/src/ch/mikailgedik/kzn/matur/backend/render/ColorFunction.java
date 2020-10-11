package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;

public interface ColorFunction<T extends Value> {
    int colorOf(T t);
}
