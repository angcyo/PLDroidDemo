package org.camera.camera;

import android.annotation.SuppressLint;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.util.Log;
import android.view.SurfaceHolder;

import org.camera.FileSwapHelper;
import org.camera.encode.VideoEncoderFromBuffer;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;

@SuppressLint("NewApi")
public class CameraWrapper {
    public static final int IMAGE_HEIGHT = 1080;
    public static final int IMAGE_WIDTH = 1920;
    private static final String TAG = "CameraWrapper";
    private static CameraWrapper mCameraWrapper;
    private Camera mCamera;
    private Camera.Parameters mCameraParamters;
    private boolean mIsPreviewing = false;
    private float mPreviewRate = -1.0f;
    private CameraPreviewCallback mCameraPreviewCallback;
    private byte[] mImageCallbackBuffer = new byte[CameraWrapper.IMAGE_WIDTH
            * CameraWrapper.IMAGE_HEIGHT * 3 / 2];

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

    public void doOpenCamera(CamOpenOverCallback callback) {
        Log.i(TAG, "Camera open....");
        int numCameras = Camera.getNumberOfCameras();
        Camera.CameraInfo info = new Camera.CameraInfo();
        for (int i = 0; i < numCameras; i++) {
            Camera.getCameraInfo(i, info);
            if (info.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
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

    public void doStartPreview(SurfaceTexture surface, float previewRate) {
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
            this.mCamera.setDisplayOrientation(90);
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

    public interface CamOpenOverCallback {
        public void cameraHasOpened();
    }

    Camera.PreviewCallback previewCallback;

    public void setPreviewCallback(Camera.PreviewCallback callback) {
        previewCallback = callback;
    }

    class CameraPreviewCallback implements Camera.PreviewCallback {
        VideoEncoderRunnable encoderRunnable;

        private CameraPreviewCallback() {
            encoderRunnable = new VideoEncoderRunnable();
            new Thread(encoderRunnable).start();
        }

        public void close() {
            encoderRunnable.exit();
        }

        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            Log.i(TAG, "onPreviewFrame " + data.length);
//            long startTime = System.currentTimeMillis();

//            long endTime = System.currentTimeMillis();
//            Log.i(TAG, Integer.toString((int) (endTime - startTime)) + " ms ");
            encoderRunnable.add(data);
            if (previewCallback != null) {
                previewCallback.onPreviewFrame(data, camera);
            }
            camera.addCallbackBuffer(data);
        }
    }

    class VideoEncoderRunnable implements Runnable {
        ArrayBlockingQueue<byte[]> bytes = new ArrayBlockingQueue<byte[]>(100);
        VideoEncoderFromBuffer curVideoEncoder;
        VideoEncoderFromBuffer nextVideoEncoder;
        private boolean isExit = false;
        private Object lock = new Object();
        private FileSwapHelper fileSwapHelper;

        public VideoEncoderRunnable() {
            fileSwapHelper = new FileSwapHelper();
        }

        public void exit() {
            synchronized (lock) {
                isExit = true;
                lock.notifyAll();
            }
        }

        public void add(byte[] data) {
            synchronized (lock) {
                bytes.add(data);
                lock.notifyAll();
            }
        }

        VideoEncoderFromBuffer getEncoder(String fileName) {
//            if (curVideoEncoder == null) {
//                curVideoEncoder = new VideoEncoderFromBuffer(fileName, IMAGE_WIDTH, IMAGE_HEIGHT);
//            } else {
//                curVideoEncoder.close();
//            }
            if (curVideoEncoder != null) {
                curVideoEncoder.close();
            }

            curVideoEncoder = new VideoEncoderFromBuffer(fileName, IMAGE_WIDTH, IMAGE_HEIGHT);

            return curVideoEncoder;
        }

        void close() {
            if (curVideoEncoder != null) {
                curVideoEncoder.close();
            }
            if (nextVideoEncoder != null) {
                nextVideoEncoder.close();
            }
        }

        @Override
        public void run() {
            while (!isExit) {

                synchronized (lock) {
                    if (bytes.isEmpty()) {
                        try {
                            lock.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }

                    } else {
                        byte[] bytes = this.bytes.poll();

                        if (fileSwapHelper.requestSwapFile()) {
                            //如果需要切换文件
                            getEncoder(fileSwapHelper.getNextFileName()).encodeFrame(bytes);
                        } else {
                            curVideoEncoder.encodeFrame(bytes);
                        }
                    }
                }
            }
            close();
        }
    }
}
