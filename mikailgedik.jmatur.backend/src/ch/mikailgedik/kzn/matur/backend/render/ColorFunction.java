package ch.mikailgedik.kzn.matur.backend.render;

import ch.mikailgedik.kzn.matur.backend.data.value.Value;
import ch.mikailgedik.kzn.matur.backend.data.value.ValueMandelbrot;

import java.awt.*;

public interface ColorFunction<T extends Value> {
    int colorOf(T t, int maxIterations);

    ColorFunction<ValueMandelbrot> MANDELBROT_COLOR_FUNCTION_IN_OUT =
            (v, i) -> v.getValue() == -1 ? 0xffffff : 0x0;

    ColorFunction<ValueMandelbrot> MANDELBROT_COLOR_FUNCTION_HSB =
            (v, i)->{
                if(v.getValue() == -1) {
                    return 0x0;
                }
                double max = Math.log(i);
                double log = Math.log(v.getValue());

                return Color.HSBtoRGB((float) (log/max),1, 1) & 0xffffff;
            };

    static ColorFunction<ValueMandelbrot> mandelbrotFromString(String name) {
        return switch (name) {
            case "mandelbrotInOut" -> MANDELBROT_COLOR_FUNCTION_IN_OUT;
            case "mandelbrotHSB" -> MANDELBROT_COLOR_FUNCTION_HSB;
            default -> throw new IllegalStateException("Unexpected value: " + name);
        };
    }
}
