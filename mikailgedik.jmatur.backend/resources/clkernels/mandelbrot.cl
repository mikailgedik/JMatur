__kernel void mandelbrot(__global const double *coordi, __global int *res,
                __global int* it, __global int* cD, __global double* precision, __global int* abort) {

    int gid = get_global_id(0);

    int x = gid % cD[0];
    int y = (int) (gid / cD[0]);

    double a = 0, b = 0, ta, tb, c, d;

    c = coordi[0] + (x + 0.5) * *precision;
    d = coordi[1] + (y + 0.5) * *precision;

    for(int i = 0; i < *it; i++) {
        ta = a*a - b*b + c;
        tb = 2 * a * b + d;
        a = ta;
        b = tb;
        if(a * a + b * b > 4 || *abort) {
            res[gid] = i;
            return;
        }
        res[gid] = -1;
    }
}