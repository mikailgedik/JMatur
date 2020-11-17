package ch.mikailgedik.kzn.matur.core.render;

import java.awt.*;

public interface ColorFunction {
    int colorOf(int t, int maxIterations);

    ColorFunction MANDELBROT_COLOR_FUNCTION_IN_OUT =
            (v, i) -> v == -1 ? 0xffffff : 0x0;

    ColorFunction MANDELBROT_COLOR_FUNCTION_HSB =
            (v, i)->{
                if(v == -1) {
                    return 0x0;
                }
                double max = Math.log(i);
                double log = Math.log(v);

                return Color.HSBtoRGB((float) (log/max),1, 1) & 0xffffff;
            };

    static ColorFunction mandelbrotFromString(String name) {
        return switch (name) {
            case "mandelbrotInOut" -> MANDELBROT_COLOR_FUNCTION_IN_OUT;
            case "mandelbrotHSB" -> MANDELBROT_COLOR_FUNCTION_HSB;
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
