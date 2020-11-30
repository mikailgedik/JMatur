int convertHSBtoRGB(double hue, double saturation, double brightness);

//This function returns the RGB color corresponding to the parameter value
//maxIterations is the maximal amount of iterations a point can go through before the program gives up
int color(int value, int maxIterations) {
    if(value == -1) {
        return 0x0;
    } else if(value == 0) {
        return convertHSBtoRGB(0, 1,1);
    } else {
        return convertHSBtoRGB(1.0*value/maxIterations, 1,1);
        //return convertHSBtoRGB(1.0*log((double)value)/log((double)maxIterations), 1,1);
    }
}

//The kernel called by the program; this section does not have to be modified
__kernel void colorKernel(__global int* cluster,
                __global int* image, __global int* destinationOffset,
                __global int* logicClusterWidth, __global int* logicImageWidth, __global int* maxIterations, int maxGlobal) {

    int gid = get_global_id(0);
    if(gid >= maxGlobal) {
        return;
    }
    int xInSource = gid % logicClusterWidth[0];
    int yInSource = gid / logicClusterWidth[0];

    int value = cluster[xInSource + yInSource * logicClusterWidth[0]];

    image[(destinationOffset[0] + xInSource) +
        (destinationOffset[1] + yInSource) * (logicImageWidth[0])] =
        color(cluster[xInSource + yInSource * logicClusterWidth[0]], *maxIterations);
}


//This method is directly copied from the java.awt.Color class
int convertHSBtoRGB(double hue, double saturation, double brightness) {
    int r = 0, g = 0, b = 0;
    if (saturation == 0) {
        r = g = b = (int) (brightness * 255.0f + 0.5f);
    } else {
        double h = (hue - floor((double)hue)) * 6.0f;
        double f = h - floor((double)h);
        double p = brightness * (1.0f - saturation);
        double q = brightness * (1.0f - saturation * f);
        double t = brightness * (1.0f - (saturation * (1.0f - f)));
        switch ((int) h) {
        case 0:
            r = (int) (brightness * 255.0f + 0.5f);
            g = (int) (t * 255.0f + 0.5f);
            b = (int) (p * 255.0f + 0.5f);
            break;
        case 1:
            r = (int) (q * 255.0f + 0.5f);
            g = (int) (brightness * 255.0f + 0.5f);
            b = (int) (p * 255.0f + 0.5f);
            break;
        case 2:
            r = (int) (p * 255.0f + 0.5f);
            g = (int) (brightness * 255.0f + 0.5f);
            b = (int) (t * 255.0f + 0.5f);
            break;
        case 3:
            r = (int) (p * 255.0f + 0.5f);
            g = (int) (q * 255.0f + 0.5f);
            b = (int) (brightness * 255.0f + 0.5f);
            break;
        case 4:
            r = (int) (t * 255.0f + 0.5f);
            g = (int) (p * 255.0f + 0.5f);
            b = (int) (brightness * 255.0f + 0.5f);
            break;
        case 5:
            r = (int) (brightness * 255.0f + 0.5f);
            g = (int) (p * 255.0f + 0.5f);
            b = (int) (q * 255.0f + 0.5f);
            break;
        }
    }
    return 0xff000000 | (r << 16) | (g << 8) | (b << 0);
}