package ch.mikailgedik.kzn.matur.calculator;

import ch.mikailgedik.kzn.matur.settings.SettingsManager;

public class MandelbrotCalculator {
    private SettingsManager sm;

    public MandelbrotCalculator(SettingsManager settingsManager) {
        this.sm = settingsManager;
    }

    public CalculationResult<CalculationResult.DataMandelbrot> calculate() {
        SettingsManager.SettingViewport vp =
                (SettingsManager.SettingViewport) sm.get(SettingsManager.SettingViewport.IDENTIFIER);
        SettingsManager.SettingNumber num =
                (SettingsManager.SettingNumber) sm.get(SettingsManager.SettingNumber.IDENTIFIER);

        CalculationResult<CalculationResult.DataMandelbrot> res = new CalculationResult<>(vp.getxCo(), vp.getyCo(),
                (int) Math.ceil((vp.getxCo()[1] - vp.getxCo()[0]) * (vp.getyCo()[1] - vp.getyCo()[0])));
        
        for(double x = vp.getxCo()[0]; x < vp.getxCo()[1]; x += num.getSampleSizeX()) {
            for(double y = vp.getyCo()[0]; y < vp.getyCo()[1]; y += num.getSampleSizeY()) {
                res.add(new CalculationResult.DataMandelbrot(x, y, calc(x,y)));
            }
        }
        return res;
    }

    private boolean calc(double x, double y) {
        double a = 0, b = 0, ta, tb;
        int maxIterations = ((SettingsManager.SettingNumber) sm.get(SettingsManager.SettingNumber.IDENTIFIER)).getMaxIterations();
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
