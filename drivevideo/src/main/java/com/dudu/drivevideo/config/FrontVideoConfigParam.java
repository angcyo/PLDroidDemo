package com.dudu.drivevideo.config;

import android.media.CamcorderProfile;

/**
 * Created by dengjun on 2015/12/16.
 * Description : 前置录像视频参数
 */
public class FrontVideoConfigParam {
    public static final String VIDEO_STORAGE_PATH = "/frontVideo";

    /* 默认录像间隔*/
    public static final int DEFAULT_VIDEO_INTERVAL = 30*1000;

    public static final   int DEFAULT_VIDEOBITRATE = 2 * 1024 * 1024;//2M
    public static  final  int DEFAULT_WIDTH = 1920;
    public static  final  int DEFAULT_HEIGHT = 480;
    public static  final  int DEFAULT_RATE= 30;
    public static  final  int DEFAULT_QUALITY= CamcorderProfile.QUALITY_HIGH;

    public static final int DEFAULT_UPLOAD_VIDEO_INTERVAL = 1*10*1000;
    public static  final int DEFAULT_UPLOAD_VIDEOBITRATE = 300 * 1024;//512k
    public static  final  int DEFAULT_UPLOAD_WIDTH = 352;
    public static  final  int DEFAULT_UPLOAD_HEIGHT = 288;
    public static  final  int DEFAULT_UPLOAD_RATE = 15;
    public static  final  int DEFAULT_UPLOAD_QUALITY = CamcorderProfile.QUALITY_CIF;

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


    public FrontVideoConfigParam() {
        videoInterval = DEFAULT_VIDEO_INTERVAL;
        videoBitRate = DEFAULT_VIDEOBITRATE;
        width = DEFAULT_WIDTH;
        height = DEFAULT_HEIGHT;
        rate = DEFAULT_RATE;
        quality = DEFAULT_QUALITY;
    }

    /* 设置成正常录制视频的参数*/
    public void resetToDefault(){
        videoInterval = DEFAULT_VIDEO_INTERVAL;
        videoBitRate = DEFAULT_VIDEOBITRATE;
        width = DEFAULT_WIDTH;
        height = DEFAULT_HEIGHT;
        rate = DEFAULT_RATE;
        quality = DEFAULT_QUALITY;
    }

    /* 设置成实时上传录制视频的参数*/
    public void setToUploadParam(){
        videoInterval = DEFAULT_UPLOAD_VIDEO_INTERVAL;
        videoBitRate = DEFAULT_UPLOAD_VIDEOBITRATE;
        width = DEFAULT_UPLOAD_WIDTH;
        height = DEFAULT_UPLOAD_HEIGHT;
        rate = DEFAULT_UPLOAD_RATE;
        quality = DEFAULT_UPLOAD_QUALITY;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
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

    public int getVideoBitRate() {
        return videoBitRate;
    }

    public void setVideoBitRate(int videoBitRate) {
        this.videoBitRate = videoBitRate;
    }

    public int getVideoInterval() {
        return videoInterval;
    }

    public void setVideoInterval(int videoInterval) {
        this.videoInterval = videoInterval;
    }
}
