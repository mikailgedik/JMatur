package ch.mikailgedik.kzn.matur.core.connector;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;

public class VideoPath implements Iterable<VideoPath.VideoPoint> {
    private final ArrayList<VideoPoint> points;
    private final ArrayList<Integer> frameCount;

    public VideoPath(VideoPoint start) {
        this();
        points.add(start);
    }

    //Only used internally
    private VideoPath() {
        points = new ArrayList<>();
        frameCount = new ArrayList<>();
    }

    public void addNext(VideoPoint point, int frameSinceLast) {
        points.add(point);
        frameCount.add(frameSinceLast);
    }

    @Override
    public Iterator<VideoPoint> iterator() {
        return new Iter();
    }

    private class Iter implements Iterator<VideoPoint> {
        private int frameCountIndex;
        private int frame;

        public Iter() {
            frameCountIndex = 0;
            frame = 0;
        }

        @Override
        public boolean hasNext() {
            return frameCountIndex < frameCount.size();
        }

        @Override
        public VideoPoint next() {
            VideoPoint p1 = points.get(frameCountIndex);
            VideoPoint p2 = points.get(frameCountIndex + 1);
            int frames = frameCount.get(frameCountIndex);
            if(frames == 0) {
                //Do not worry about frame, is already 0
                frameCountIndex++;
            }
            double frac;
            if(frames == 1) {
                frac = .5;
            } else {
                frac = (frame) / (frames - 1.0);
            }

            double[] d = {p2.center[0] - p1.center[0],
                    p2.center[1] - p1.center[1]};

            VideoPoint ret = new VideoPoint(new double[]{
                    p1.center[0] + d[0] * frac,
                    p1.center[1] + d[1] * frac,
            }, p1.height + frac * (p2.height - p1.height));

            frame++;
            if(frame >= frames) {
                frame = 0;
                frameCountIndex++;
            }
            return ret;
        }
    }

    public static class VideoPoint{
        private final double[] center;
        private final double height;
        public VideoPoint(double[] center, double height) {
            this.center = center.clone();
            this.height = height;
        }

        public double[] getCenter() {
            return center;
        }

        public double getHeight() {
            return height;
        }
    }

    public static void write(OutputStream out, VideoPath path) throws IOException {
        BufferedWriter w = new BufferedWriter(new OutputStreamWriter(out));
        for(int i = 0; i < path.frameCount.size(); i++) {
            VideoPoint p = path.points.get(i);
            w.write(p.getCenter()[0] + " " + p.getCenter()[1] + " " + p.getHeight() + " " + path.frameCount.get(i));
            w.newLine();
        }
        VideoPoint p = path.points.get(path.points.size()-1);
        w.write(p.getCenter()[0] + " " + p.getCenter()[1] + " " + p.getHeight());
        w.flush();
    }

    public static VideoPath read(InputStream in) throws IOException {
        BufferedReader r = new BufferedReader(new InputStreamReader(in));
        String line;
        VideoPath path = new VideoPath();
        while((line = r.readLine()) != null) {
            line = line.trim();
            if(line.equals("") || line.startsWith("#")) {
                continue;
            }
            String[] split = line.split(" ");
            switch (split.length) {
                case 4 -> path.addNext(new VideoPoint(new double[]{Double.parseDouble(split[0]),
                        Double.parseDouble(split[1])}, Double.parseDouble(split[2])), Integer.parseInt(split[3]));
                case 3 -> {
                    path.points.add(new VideoPoint(new double[]{Double.parseDouble(split[0]),
                            Double.parseDouble(split[1])}, Double.parseDouble(split[2])));
                    return path;
                }
                default -> throw new RuntimeException("Unexpected line content: " + line);
            }
        }
        throw new RuntimeException("Unexpected file end");
    }
}
