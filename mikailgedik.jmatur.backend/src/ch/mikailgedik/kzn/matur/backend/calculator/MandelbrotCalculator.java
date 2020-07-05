package ch.mikailgedik.kzn.matur.backend.calculator;

import ch.mikailgedik.kzn.matur.backend.settings.SettingsManager;

public class MandelbrotCalculator {
    private SettingsManager sm;

    public MandelbrotCalculator(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    public CalculationResult<CalculationResult.DataMandelbrot> calculate() {
        double minx = sm.getD(SettingsManager.CALCULATION_MINX);
        double maxx = sm.getD(SettingsManager.CALCULATION_MAXX);
        double miny = sm.getD(SettingsManager.CALCULATION_MINY);
        double maxy = sm.getD(SettingsManager.CALCULATION_MAXY);

        double xSampleSize = (maxx - minx) / (sm.getI(SettingsManager.CALCULATION_TICKX) + 1);
        double ySampleSize = (maxy - miny) / (sm.getI(SettingsManager.CALCULATION_TICKY) + 1);

        CalculationResult<CalculationResult.DataMandelbrot> res = new CalculationResult<>(new double[]{minx, maxx}, new double[]{miny, maxy},
                (int) Math.ceil((maxx - minx) * (maxy- miny)));
        
        for(double x = minx; x < maxx; x += xSampleSize) {
            for(double y = miny; y < maxy; y += ySampleSize) {
                res.add(new CalculationResult.DataMandelbrot(x, y, calc(x,y)));
            }
        }
        return res;
    }

    private boolean calc(double x, double y) {
        double a = 0, b = 0, ta, tb;
        int maxIterations = sm.getI(SettingsManager.CALCULATION_MAX_ITERATIONS);
        for(int i = 0; i < maxIterations; ++i) {
            ta = a*a - b*b + x;
            tb = 2 * a * b + y;
            a = ta;
            b = tb;
            if(a*a + b*b > 4) {
                return false;
            }
        }
        return true;
    }
}
