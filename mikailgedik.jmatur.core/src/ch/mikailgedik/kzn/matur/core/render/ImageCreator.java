package ch.mikailgedik.kzn.matur.core.render;

import ch.mikailgedik.kzn.matur.core.connector.Screen;
import ch.mikailgedik.kzn.matur.core.data.Region;

public abstract class ImageCreator {
    public abstract Screen createScreen(int minPixelWidth, int minPixelHeight, Region region, long maxWaitingTime);
}
