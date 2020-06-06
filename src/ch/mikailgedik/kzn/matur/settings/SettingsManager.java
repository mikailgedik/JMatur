package ch.mikailgedik.kzn.matur.settings;

import java.util.LinkedHashMap;

public class SettingsManager {
    private LinkedHashMap<String, Setting> settings;

    private SettingsManager() {
        this.settings = new LinkedHashMap<>();
        makeDefault();
    }

    public static SettingsManager createDefaultSettingsManager() {
        return new SettingsManager();
    }

    /** Removes all the settings and loads the default values from Filemanager
     *
     * */
    public void makeDefault() {
        settings.clear();

        settings.put(SettingViewport.IDENTIFIER, new SettingViewport(new double[]{-2d, 2d}, new double[]{-2d, 2d}));
        settings.put(SettingNumber.IDENTIFIER, new SettingNumber());
        /* TODO
         readFromStream();
         */
    }

    public Setting get(String name) {
        return settings.get(name);
    }

    public interface Setting {
        String identifier();
    }

    /** The class for settings related to the viewport */
    public static class SettingViewport implements Setting {
        public static String IDENTIFIER = "setting.viewport";

        private double[] xCo, yCo;

        public SettingViewport(double[] xCo, double[] yCo) {
            this.xCo = xCo.clone();
            this.yCo = yCo.clone();
        }

        public double[] getxCo() {
            return xCo;
        }

        public double[] getyCo() {
            return yCo;
        }

        @Override
        public String identifier() {
            return IDENTIFIER;
        }
    }

    /** A class for settings related to numbers */
    public static class SettingNumber implements Setting {
        public static String IDENTIFIER = "setting.number";

        private int maxIterations;
        private double sampleSizeX, sampleSizeY;

        public SettingNumber() {
            this(1000,.01,.01);
        }

        public SettingNumber(int maxIterations, double sampleSizeX, double sampleSizeY) {
            this.maxIterations = maxIterations;
            this.sampleSizeX = sampleSizeX;
            this.sampleSizeY = sampleSizeY;
        }

        public int getMaxIterations() {
            return maxIterations;
        }

        public double getSampleSizeX() {
            return sampleSizeX;
        }

        public double getSampleSizeY() {
            return sampleSizeY;
        }

        @Override
        public String identifier() {
            return IDENTIFIER;
        }
    }
}
