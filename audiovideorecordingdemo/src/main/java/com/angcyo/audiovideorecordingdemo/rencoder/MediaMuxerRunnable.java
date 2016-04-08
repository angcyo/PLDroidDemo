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
    private static MediaMuxerRunnable mediaMuxerRunnable;
    private final Object lock = new Object();
    private MediaMuxer mediaMuxer;
    private Vector<MuxerData> muxerDatas;
    private volatile boolean isExit = false;
    private int videoTrackIndex = -1;
    private int audioTrackIndex = -1;
    private volatile boolean isVideoAdd;
    private volatile boolean isAudioAdd;
    private AudioRunnable audioThread;
    private VideoRunnable videoThread;
    private FileSwapHelper fileSwapHelper;
    private boolean isThreadStart = false;

    private MediaMuxerRunnable() {
        muxerDatas = new Vector<>();
        fileSwapHelper = new FileSwapHelper();

        audioThread = new AudioRunnable(new WeakReference<MediaMuxerRunnable>(this));
        videoThread = new VideoRunnable(CameraWrapper.IMAGE_WIDTH, CameraWrapper.IMAGE_HEIGHT, new WeakReference<MediaMuxerRunnable>(this));

        audioThread.start();
        videoThread.start();

        try {
            start();
        } catch (IOException e) {
            restart();
        }
    }

    public static void startMuxer() {
        if (mediaMuxerRunnable == null) {
            synchronized (MediaMuxerRunnable.class) {
                if (mediaMuxerRunnable == null) {
                    mediaMuxerRunnable = new MediaMuxerRunnable();
                }
            }
        }
    }

    public static void stopMuxer() {
        if (mediaMuxerRunnable != null) {
            mediaMuxerRunnable.exit();
        }
    }

    public static void addVideoFrameData(byte[] data) {
        if (mediaMuxerRunnable != null) {
            mediaMuxerRunnable.addVideoData(data);
        }
    }

    private void addVideoData(byte[] data) {
        if (videoThread != null) {
            videoThread.add(data);
        }
    }

    public boolean isAudioAdd() {
        return isAudioAdd;
    }

    public boolean isVideoAdd() {
        return isVideoAdd;
    }

    public void start() throws IOException {
        if (fileSwapHelper.requestSwapFile()) {
            start(fileSwapHelper.getNextFileName(), false);
        }
    }

    public void start(String filePath, boolean restart) throws IOException {
        isExit = false;
        isVideoAdd = false;
        isAudioAdd = false;
        muxerDatas.clear();

        mediaMuxer = new MediaMuxer(filePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
//        if (audioThread != null) {
//            Log.e("ang-->", "配置Audio解码.");
//            audioThread.prepare();
//            audioThread.prepareAudioRecord();
//        }
//        if (videoThread != null) {
//            Log.e("ang-->", "配置Video解码.");
//            videoThread.prepare();
//        }
        if (!restart) {
            Log.e("ang-->", "Start 混合线程.");
            new Thread(this).start();
        }
        if (audioThread != null) {
//            new Thread(audioThread).start();
            audioThread.setMuxerReady(true);
        }
        if (videoThread != null) {
//            new Thread(videoThread).start();
            videoThread.setMuxerReady(true);
        }

        Log.e("angcyo-->", "start(String filePath, boolean restart) 保存至:" + filePath);
    }

    public synchronized void addTrackIndex(@TrackIndex int index, MediaFormat mediaFormat) {
        if (isMuxerStart()) {
            return;
        }

        /*轨迹改变之后,重启混合器*/
        if ((index == TRACK_AUDIO && isAudioAdd())
                ||
                (index == TRACK_VIDEO && isVideoAdd())) {
            restart();
            return;
        }

        if (mediaMuxer != null) {
            int track = 0;
            try {
                track = mediaMuxer.addTrack(mediaFormat);
            } catch (Exception e) {
                e.printStackTrace();
                restart();
                return;
            }

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
    }

    private void exit() {
        if (videoThread != null) {
            videoThread.exit();
            try {
                videoThread.join();
            } catch (InterruptedException e) {

            }
        }
        if (audioThread != null) {
            audioThread.exit();
            try {
                audioThread.join();
            } catch (InterruptedException e) {

            }
        }

        isExit = true;
        synchronized (lock) {
            lock.notify();
        }
    }


    public void addMuxerData(MuxerData data) {
//        Log.e("ang-->", "收到混合数据..." + data.bufferInfo.size);

        if (!isMuxerStart()) {
            return;
        }

        muxerDatas.add(data);
        synchronized (lock) {
            lock.notify();
        }
    }

    private void restart() {
        fileSwapHelper.requestSwapFile(true);
        String nextFileName = fileSwapHelper.getNextFileName();
        restart(nextFileName);
    }

    private void restart(String filePath) {
        restartAudioVideo();
//        Log.e("angcyo-->", "退出音视频线程完成.");
        stop();
        //-----------------

        try {
            start(filePath, true);
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("angcyo-->", "start(filePath, true) " + "重启混合器失败 尝试再次重启!");
            restart();
            return;
        }
        Log.e("angcyo-->", "重启混合器完成");
    }

    @Override
    public void run() {
        isThreadStart = true;
        while (!isExit) {
            if (muxerDatas.isEmpty()) {
                synchronized (lock) {
                    try {
                        Log.e("ang-->", "等待混合数据...");
                        lock.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                if (fileSwapHelper.requestSwapFile()) {
                    //需要切换文件
                    String nextFileName = fileSwapHelper.getNextFileName();
                    Log.e("angcyo-->", "正在重启混合器..." + nextFileName);
                    restart(nextFileName);
                } else {
                    if (isMuxerStart()) {
                        MuxerData data = muxerDatas.remove(0);
                        int track;
                        if (data.trackIndex == TRACK_VIDEO) {
                            track = videoTrackIndex;
                        } else {
                            track = audioTrackIndex;
                        }
                        Log.e("ang-->", "写入混合数据 " + data.bufferInfo.size);
                        try {
                            mediaMuxer.writeSampleData(track, data.byteBuf, data.bufferInfo);
                        } catch (Exception e) {
                            e.printStackTrace();
                            Log.e("angcyo-->", "写入混合数据失败!");

                            restart();
                        }
                    } else {
                        synchronized (lock) {
                            try {
                                Log.e("angcyo-->", "等待音视轨添加...");
                                lock.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

            }
        }
        stop();
        isThreadStart = false;
        Log.e("angcyo-->", "混合器退出...");
    }

    private void requestStart() {
        synchronized (lock) {
            if (isMuxerStart()) {
                mediaMuxer.start();
                Log.e("angcyo-->", "requestStart 启动混合器 开始等待数据登入...");
                lock.notify();
            }
        }
    }

    public boolean isMuxerStart() {
        return isAudioAdd && isVideoAdd;
    }

    private void restartAudioVideo() {
        if (audioThread != null) {
            audioTrackIndex = -1;
            isAudioAdd = false;
            audioThread.restart();
        }
        if (videoThread != null) {
            videoTrackIndex = -1;
            isVideoAdd = false;
            videoThread.restart();
        }
    }

    private void stop() {
        if (mediaMuxer != null) {
            try {
                mediaMuxer.stop();
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                mediaMuxer.release();
            } catch (Exception e) {
                e.printStackTrace();
            }
            mediaMuxer = null;
        }
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
