__kernel void color(__global int* cluster,
                __global int* image, __global int* destinationOffset,
                __global int* logicClusterWidth, __global int* logicImageWidth, __global int* maxIterations) {

    int gid = get_global_id(0);
    int xInSource = gid % logicClusterWidth[0];
    int yInSource = gid / logicClusterWidth[0];

    int value = cluster[xInSource + yInSource * logicClusterWidth[0]];

    if(value == -1) {
        value = 0;
    } else {
        double f = log((double)value)/log((double)*maxIterations);
        value = 0xffffff * f;
    }

    image[(destinationOffset[0] + xInSource) +
        (destinationOffset[1] + yInSource) * (logicImageWidth[0])] = value;
}