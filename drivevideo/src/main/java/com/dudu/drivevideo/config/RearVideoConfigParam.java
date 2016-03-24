package com.dudu.drivevideo.config;

import android.media.CamcorderProfile;

/**
 * Created by dengjun on 2016/1/26.
 * Description :
 */
public class RearVideoConfigParam {
    public static final String VIDEO_STORAGE_PATH = "/rearVideo";
    /* 默认录像间隔*/
    public static final int DEFAULT_VIDEO_INTERVAL = 1*30*1000;

    public static final   int DEFAULT_VIDEOBITRATE = 2 * 1024 * 1024;//2M
    public static  final  int DEFAULT_WIDTH = 640;
    public static  final  int DEFAULT_HEIGHT = 480;
    public static  final  int DEFAULT_RATE= 30;
    public static  final  int DEFAULT_QUALITY= CamcorderProfile.QUALITY_HIGH;

    private   int videoInterval;
    /*video output bit rate */
    private int videoBitRate;
    /* */
    private int width;
    /* */
    private int height;
    /* */
    private int rate;
    /* */
    private int quality;

    public RearVideoConfigParam() {
        videoInterval = DEFAULT_VIDEO_INTERVAL;
        videoBitRate = DEFAULT_VIDEOBITRATE;
        width = DEFAULT_WIDTH;
        height = DEFAULT_HEIGHT;
        rate = DEFAULT_RATE;
        quality = DEFAULT_QUALITY;
    }


    public int getVideoInterval() {
        return videoInterval;
    }

    public void setVideoInterval(int videoInterval) {
        this.videoInterval = videoInterval;
    }

    public int getVideoBitRate() {
        return videoBitRate;
    }

    public void setVideoBitRate(int videoBitRate) {
        this.videoBitRate = videoBitRate;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public int getQuality() {
        return quality;
    }

    public void setQuality(int quality) {
        this.quality = quality;
    }
}
