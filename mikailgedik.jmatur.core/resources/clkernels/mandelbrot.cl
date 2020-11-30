//This function calculates how many iterations it takes for the value specified by x and y to diverge from the mandelbrot
//Feel free to make changes to this function to make your own fractal
//As soon as the abort parameter is set to 1, the program should to stop
int calc(double x, double y, int maxIter, __global int* abort);

//If JULIA_SET is defined, the julia set will be generated instead of the mandelbrot
//#define JULIA_SET
#define JULIA_SET_C_REAL (0.0)
#define JULIA_SET_C_IMAG (0.0)

#ifdef JULIA_SET
    int calc(double x, double y, int maxIter, __global int* abort) {
        double a = x, b = y, ta, tb;
        int i = 0;
        for(;i < maxIter  & !(a * a + b * b > 4 | *abort); i++) {
            ta = a*a - b*b + JULIA_SET_C_REAL;
            tb = 2 * a * b + JULIA_SET_C_IMAG;
            a = ta;
            b = tb;
        }
        return i == maxIter ? -1 : i;
    }
#endif

#ifndef JULIA_SET
    int calc(double x, double y, int maxIter, __global int* abort) {
        double a = 0, b = 0, ta, tb;
        int i = 0;
        for(;i < maxIter  & !(a * a + b * b > 4 | *abort); i++) {
            ta = a*a - b*b + x;
            tb = 2 * a * b + y;
            a = ta;
            b = tb;
        }
        return i == maxIter ? -1 : i;
    }
#endif

//The kernel called by the program; this section does not have to be modified
__kernel void fractal(__global const double *coordi, __global int *res,
                __global int *it, __global int* cD, __global double *precision, __global int* abort, const int maxGlobal) {

    int gid = get_global_id(0);
    if(gid >= maxGlobal) {
        return;
    }
    int x = gid % cD[0];
    int y = (int) (gid / cD[0]);

    double c, d;

    c = coordi[0] + (x + 0.5) * *precision;
    d = coordi[1] + (y + 0.5) * *precision;

    res[gid] = calc(c, d, *it, abort);
}