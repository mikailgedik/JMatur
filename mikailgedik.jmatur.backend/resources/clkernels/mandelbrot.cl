int calc(double c, double d, int maxIter, __global int* abort) {
    double a = 0, b = 0, ta, tb;
    for(int i = 0; i < maxIter; i++) {
        ta = a*a - b*b + c;
        tb = 2 * a * b + d;
        a = ta;
        b = tb;
        if(a * a + b * b > 4 || *abort) {
            return i;
        }
    }
    return -1;
}

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