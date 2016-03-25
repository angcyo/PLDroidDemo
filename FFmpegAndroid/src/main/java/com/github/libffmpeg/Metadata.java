package com.github.libffmpeg;

/**
 * @author Egor Makovsky (yahor.makouski@gmail.com).
 */
public class Metadata {
    private double fps = -1;

    private long duration = -1;

    public double getFps() {
        return fps;
    }

    public void setFps(double fps) {
        this.fps = fps;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }
}
