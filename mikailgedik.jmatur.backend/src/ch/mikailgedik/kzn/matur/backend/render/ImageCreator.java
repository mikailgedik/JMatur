package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.connector.Screen;
import ch.mikailgedik.kzn.matur.backend.data.Region;

public abstract class ImageCreator {
    public abstract Screen createScreen(int minPixelWidth, int minPixelHeight, Region region, long maxWaitingTime);
}
