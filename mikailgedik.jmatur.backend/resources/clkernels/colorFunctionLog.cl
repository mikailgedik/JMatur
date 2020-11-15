//This function returns the RGB color corresponding to the parameter value
//maxIterations is the maximal amount of iterations a point can go through before the program gives up
int color(int value, int maxIterations) {
    if(value == -1) {
        return 0x0;
    } else {
        return 0xffffff * (log((double)value)/log((double)maxIterations));
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