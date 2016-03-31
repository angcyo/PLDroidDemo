package com.angcyo.audiovideorecordingdemo;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import com.angcyo.audiovideorecordingdemo.encoder.MediaAudioEncoder;
import com.angcyo.audiovideorecordingdemo.encoder.MediaEncoder;
import com.angcyo.audiovideorecordingdemo.encoder.MediaMuxerWrapper;
import com.angcyo.audiovideorecordingdemo.encoder.MediaVideoEncoder;

import java.io.IOException;
import java.util.List;

@SuppressLint("NewApi")
public class CameraWrapper {
    public static final int IMAGE_HEIGHT = 1080;
    public static final int IMAGE_WIDTH = 1920;
    private static final String TAG = "CameraWrapper";
    private static CameraWrapper mCameraWrapper;
    Camera.PreviewCallback previewCallback;
    private Camera mCamera;
    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private float mPreviewRate = -1.0f;
    private CameraPreviewCallback mCameraPreviewCallback;
    private byte[] mImageCallbackBuffer = new byte[CameraWrapper.IMAGE_WIDTH
            * CameraWrapper.IMAGE_HEIGHT * 3 / 2];
    private boolean isBlur = false;

    private static final boolean DEBUG = true;	// TODO set false on release

    private int openCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;

    private CameraWrapper() {
    }

    public static CameraWrapper getInstance() {
        if (mCameraWrapper == null) {
            synchronized (CameraWrapper.class) {
                if (mCameraWrapper == null) {
                    mCameraWrapper = new CameraWrapper();
                }
            }
        }
        return mCameraWrapper;
    }

    public void switchCameraId() {
        if (openCameraId == Camera.CameraInfo.CAMERA_FACING_BACK) {
            openCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        } else {
            openCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
    }

    public void doOpenCamera(CamOpenOverCallback callback) {
        Log.i(TAG, "Camera open....");
        int numCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == openCameraId) {
                mCamera = Camera.open(i);
                break;
            }
        }
        if (mCamera == null) {
            Log.d(TAG, "No front-facing camera found; opening default");
            mCamera = Camera.open();    // opens first back-facing camera
        }
        if (mCamera == null) {
            throw new RuntimeException("Unable to open camera");
        }
        Log.i(TAG, "Camera open over....");
        callback.cameraHasOpened();
    }

    public void doStartPreview(SurfaceHolder holder, float previewRate) {
        Log.i(TAG, "doStartPreview...");
        if (mIsPreviewing) {
            this.mCamera.stopPreview();
            return;
        }

        try {
            this.mCamera.setPreviewDisplay(holder);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initCamera();
    }

    public void doStartPreview(SurfaceTexture surface) {
        Log.i(TAG, "doStartPreview()");
        if (mIsPreviewing) {
            this.mCamera.stopPreview();
            return;
        }

        try {
            this.mCamera.setPreviewTexture(surface);
        } catch (IOException e) {
            e.printStackTrace();
        }
        initCamera();
    }

    public void doStopCamera() {
        Log.i(TAG, "doStopCamera");
        if (this.mCamera != null) {
            mCameraPreviewCallback.close();
            this.mCamera.setPreviewCallback(null);
            this.mCamera.stopPreview();
            this.mIsPreviewing = false;
            this.mPreviewRate = -1f;
            this.mCamera.release();
            this.mCamera = null;
        }
    }

    private void initCamera() {
        if (this.mCamera != null) {
            this.mCameraParamters = this.mCamera.getParameters();
            this.mCameraParamters.setPreviewFormat(ImageFormat.NV21);
            this.mCameraParamters.setFlashMode("off");
            this.mCameraParamters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            this.mCameraParamters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            this.mCameraParamters.setPreviewSize(IMAGE_WIDTH, IMAGE_HEIGHT);
//            this.mCamera.setDisplayOrientation(90);
            mCameraPreviewCallback = new CameraPreviewCallback();
            mCamera.addCallbackBuffer(mImageCallbackBuffer);
            mCamera.setPreviewCallbackWithBuffer(mCameraPreviewCallback);
            List<String> focusModes = this.mCameraParamters.getSupportedFocusModes();
            if (focusModes.contains("continuous-video")) {
                this.mCameraParamters
                        .setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            }
            this.mCamera.setParameters(this.mCameraParamters);
            this.mCamera.startPreview();

            this.mIsPreviewing = true;
        }
    }

    public void setBlur(boolean blur) {
        isBlur = blur;
    }


    public void setPreviewCallback(Camera.PreviewCallback callback) {
        previewCallback = callback;
    }

    public interface CamOpenOverCallback {
        public void cameraHasOpened();
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {
//        VideoEncoderRunnable encoderRunnable;
        private MediaMuxerWrapper mMuxer;

        private CameraPreviewCallback() {
//            encoderRunnable = new VideoEncoderRunnable();
//            new Thread(encoderRunnable).start();
            startRecording();
        }

        public void close() {
//            encoderRunnable.exit();
            stopRecording();
        }

        private void startRecording() {
            if (DEBUG) Log.v(TAG, "startRecording:");
            try {
                mMuxer = new MediaMuxerWrapper(".mp4");	// if you record audio only, ".m4a" is also OK.
                if (true) {
                    // for video capturing
                    new MediaVideoEncoder(mMuxer, mMediaEncoderListener, IMAGE_WIDTH, IMAGE_HEIGHT);
                }
                if (true) {
                    // for audio capturing
                    new MediaAudioEncoder(mMuxer, mMediaEncoderListener);
                }
                mMuxer.prepare();
                mMuxer.startRecording();
            } catch (final IOException e) {
                Log.e(TAG, "startCapture:", e);
            }
        }

        private void stopRecording() {
            if (DEBUG) Log.v(TAG, "stopRecording:mMuxer=" + mMuxer);
            if (mMuxer != null) {
                mMuxer.stopRecording();
                mMuxer = null;
                // you should not wait here
            }
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.i(TAG, "onPreviewFrame " + data.length);
//            long startTime = System.currentTimeMillis();

//            long endTime = System.currentTimeMillis();
//            Log.i(TAG, Integer.toString((int) (endTime - startTime)) + " ms ");
//            encoderRunnable.add(data);
//            if (previewCallback != null) {
//                previewCallback.onPreviewFrame(data, camera);
//            }
            ((MediaVideoEncoder) mMuxer.mVideoEncoder).frameAvailableSoon(data);
            camera.addCallbackBuffer(data);
        }
    }

    /**
     * callback methods from encoder
     */
    private final MediaEncoder.MediaEncoderListener mMediaEncoderListener = new MediaEncoder.MediaEncoderListener() {
        @Override
        public void onPrepared(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onPrepared:encoder=" + encoder);
//            if (encoder instanceof MediaVideoEncoder)
//                mCameraView.setVideoEncoder((MediaVideoEncoder)encoder);
        }

        @Override
        public void onStopped(final MediaEncoder encoder) {
            if (DEBUG) Log.v(TAG, "onStopped:encoder=" + encoder);
//            if (encoder instanceof MediaVideoEncoder)
//                mCameraView.setVideoEncoder(null);
        }
    };

//    class VideoEncoderRunnable implements Runnable {
//        Vector<byte[]> bytes = new Vector<byte[]>(100);
//        VideoEncoderFromBuffer curVideoEncoder;
//        VideoEncoderFromBuffer nextVideoEncoder;
//        private boolean isExit = false;
//        private Object lock = new Object();
//        private FileSwapHelper fileSwapHelper;
//        BlurRunnable blurRunnable;
//
//        public VideoEncoderRunnable() {
//            fileSwapHelper = new FileSwapHelper();
//            blurRunnable = new BlurRunnable();
//            new Thread(blurRunnable).start();
//        }
//
//        public void exit() {
//            isExit = true;
//        }
//
//        public void add(byte[] data) {
//            bytes.add(data);
//        }
//
//        VideoEncoderFromBuffer getEncoder(String fileName) {
//            if (curVideoEncoder != null) {
//                curVideoEncoder.close();
//            }
//
//            curVideoEncoder = new VideoEncoderFromBuffer(fileName, IMAGE_WIDTH, IMAGE_HEIGHT);
//
//            return curVideoEncoder;
//        }
//
//        void close() {
//            if (curVideoEncoder != null) {
//                curVideoEncoder.close();
//            }
//            if (nextVideoEncoder != null) {
//                nextVideoEncoder.close();
//            }
//            blurRunnable.exit();
//        }
//
//        @Override
//        public void run() {
//
//            int frameIndex = 0;//保存帧的索引
//            int frameBlur = 10;//第几帧, 进行模糊处理
//            while (!isExit) {
//                if (!bytes.isEmpty()) {
//                    byte[] bytes = this.bytes.remove(0);
//
//                        /*模糊处理*/
//                    if (isBlur) {// && (frameIndex % frameBlur) == 0
//                        blurRunnable.add(bytes);
//                        frameIndex = 0;//防止数据过大越界
//                    }
//
//                        /*录像存储*/
//                    if (fileSwapHelper.requestSwapFile()) {
//                        //如果需要切换文件
//                        getEncoder(fileSwapHelper.getNextFileName()).encodeFrame(bytes);
//                    } else {
//                        curVideoEncoder.encodeFrame(bytes);
//                    }
//
//                    frameIndex++;
//                }
//            }
//            close();
//        }
//    }

}
