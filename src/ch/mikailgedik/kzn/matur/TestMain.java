package ch.mikailgedik.kzn.matur;

import ch.mikailgedik.kzn.matur.calculator.CalculationResult;
import ch.mikailgedik.kzn.matur.calculator.MandelbrotCalculator;
import ch.mikailgedik.kzn.matur.filemanager.FileManager;
import ch.mikailgedik.kzn.matur.render.ImageCreator;
import ch.mikailgedik.kzn.matur.settings.SettingsManager;

import java.awt.image.BufferedImage;
import java.text.DecimalFormat;

public class TestMain {
    public static final String VERSION = "Java 0.0.1";

    private static final String OUTPUT_FILE = "/home/mikail/Desktop/File.png";

    public static void main(String[] args) {
        System.out.println("Version: " + VERSION);
        DecimalFormat format = new DecimalFormat("00000");
        long t = System.currentTimeMillis();

        SettingsManager man = SettingsManager.createDefaultSettingsManager();
        MandelbrotCalculator b = new MandelbrotCalculator(man);
        CalculationResult<CalculationResult.DataMandelbrot> data = b.calculate();

        System.out.println("Time to calculate fractal: " + format.format((System.currentTimeMillis()-t)) + " ms");
        t = System.currentTimeMillis();

        ImageCreator imageCreator = new ImageCreator();
        BufferedImage image = imageCreator.createImage(data, 50, 50, man);

        System.out.println("Time to calculate image:   " + format.format((System.currentTimeMillis()-t)) + " ms");
        t = System.currentTimeMillis();

        FileManager manager = new FileManager();
        manager.saveImage(OUTPUT_FILE, image);
        System.out.println("Saved image to: " + OUTPUT_FILE);
        System.out.println("Time to save image:        " + format.format((System.currentTimeMillis()-t)) + " ms");
        //t = System.currentTimeMillis();
    }
}
