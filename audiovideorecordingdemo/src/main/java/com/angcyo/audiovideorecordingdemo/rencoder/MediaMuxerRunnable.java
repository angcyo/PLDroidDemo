package com.angcyo.audiovideorecordingdemo.rencoder;

import android.media.MediaCodec;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.support.annotation.IntDef;
import android.util.Log;

import com.angcyo.audiovideorecordingdemo.CameraWrapper;
import com.angcyo.audiovideorecordingdemo.FileSwapHelper;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Vector;

/**
 * Created by robi on 2016-04-01 10:45.
 */
public class MediaMuxerRunnable implements Runnable {

    public static final int TRACK_VIDEO = 0;
    public static final int TRACK_AUDIO = 1;
    private MediaMuxer mediaMuxer;
    private Vector<MuxerData> muxerDatas;
    private volatile boolean isExit = false;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private volatile boolean isVideoAdd;
    private volatile boolean isAudioAdd;
    private AudioRunnable audioRunnable;
    private VideoRunnable videoRunnable;
    private Object lock = new Object();
    private FileSwapHelper fileSwapHelper;

    public MediaMuxerRunnable() {
        muxerDatas = new Vector<>();
        fileSwapHelper = new FileSwapHelper();
    }

    public void setAudioRunnable(AudioRunnable audioRunnable) {
        this.audioRunnable = audioRunnable;
    }

    public VideoRunnable getVideoRunnable() {
        return videoRunnable;
    }

    public void setVideoRunnable(VideoRunnable videoRunnable) {
        this.videoRunnable = videoRunnable;
    }

    public boolean isAudioAdd() {
        return isAudioAdd;
    }

    public boolean isVideoAdd() {
        return isVideoAdd;
    }

    public void start() throws IOException {
        new AudioRunnable(new WeakReference<MediaMuxerRunnable>(this));
        new VideoRunnable(CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT,
                new WeakReference<MediaMuxerRunnable>(this));

        if (fileSwapHelper.requestSwapFile()) {
            start(fileSwapHelper.getNextFileName(), false);
        }
    }

    public void start(String filePath, boolean restart) throws IOException {
        isExit = false;
        isVideoAdd = false;
        isAudioAdd = false;
        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        if (audioRunnable != null) {
            audioRunnable.prepare();
            audioRunnable.prepareAudioRecord();
        }
        if (videoRunnable != null) {
            videoRunnable.prepare();
        }
        if (!restart) {
            new Thread(this).start();
        }
        if (audioRunnable != null) {
            new Thread(audioRunnable).start();
        }
        if (videoRunnable != null) {
            new Thread(videoRunnable).start();
        }

        Log.e("angcyo", "保存至:" + filePath);
    }

    public void addTrackIndex(@TrackIndex int index, MediaFormat mediaFormat) {
        if (isMuxerStart()) {
            return;
        }

        int track = mediaMuxer.addTrack(mediaFormat);

        if (index == TRACK_VIDEO) {
            videoTrackIndex = track;
            isVideoAdd = true;
            Log.e("angcyo-->", "添加视轨 完成");
        } else {
            audioTrackIndex = track;
            isAudioAdd = true;
            Log.e("angcyo-->", "添加音轨 完成");
        }
        requestStart();
    }

    public void exit() {
        exitAudioVideo();
        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }


    public void addMuxerData(MuxerData data) {
        if (!isMuxerStart()) {
            return;
        }

        muxerDatas.add(data);
        synchronized (lock) {
            lock.notify();
        }
    }

    private void restart(String filePath) {
        exitAudioVideo();
        stop();
        //-----------------
        new AudioRunnable(new WeakReference<MediaMuxerRunnable>(this));
        new VideoRunnable(CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT,
                new WeakReference<MediaMuxerRunnable>(this));
        try {
            start(filePath, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Log.e("angcyo-->", "重启混合器完成");
    }

    @Override
    public void run() {
        while (!isExit) {
            if (muxerDatas.isEmpty()) {
                synchronized (lock) {
                    try {
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (fileSwapHelper.requestSwapFile()) {
                    //需要切换文件
                    restart(fileSwapHelper.getNextFileName());
                } else {
                    if (isMuxerStart()) {
                        MuxerData data = muxerDatas.remove(0);
                        int track;
                        if (data.trackIndex == TRACK_VIDEO) {
                            track = videoTrackIndex;
                        } else {
                            track = audioTrackIndex;
                        }
                        Log.e("angcyo-->", "写入混合数据 " + data.bufferInfo.size);
                        try {
                            mediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }

            }
        }
        stop();
        Log.e("angcyo-->", "混合器退出...");
    }

    private void requestStart() {
        synchronized (lock) {
            if (isMuxerStart()) {
                mediaMuxer.start();
                Log.e("angcyo-->", "启动混合器");
                lock.notify();
            }
        }
    }

    private boolean isMuxerStart() {
        return isAudioAdd && isVideoAdd;
    }

    private void exitAudioVideo() {
        if (audioRunnable != null) {
            audioRunnable.exit();
        }
        if (videoRunnable != null) {
            videoRunnable.exit();
        }
    }

    private void stop() {
        try {
            mediaMuxer.stop();
            mediaMuxer.release();
            mediaMuxer = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
        videoTrackIndex = -1;
        audioTrackIndex = -1;
    }

    @IntDef({TRACK_VIDEO, TRACK_AUDIO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TrackIndex {
    }

    /**
     * 封装需要传输的数据类型
     */
    public static class MuxerData {
        int trackIndex;
        ByteBuffer byteBuf;
        MediaCodec.BufferInfo bufferInfo;

        public MuxerData(@TrackIndex int trackIndex, ByteBuffer byteBuf, MediaCodec.BufferInfo bufferInfo) {
            this.trackIndex = trackIndex;
            this.byteBuf = byteBuf;
            this.bufferInfo = bufferInfo;
        }
    }

}
