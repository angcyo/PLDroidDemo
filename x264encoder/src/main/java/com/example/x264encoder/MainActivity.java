package com.example.x264encoder;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import example.sszpf.x264.x264sdk;
import example.sszpf.x264.x264sdk.listener;

public class MainActivity extends Activity implements SurfaceHolder.Callback, PreviewCallback {

    private static String path = Environment.getExternalStorageDirectory().getAbsolutePath() + "/zpf_test.h264";
    x264sdk x264;
    FileOutputStream outStream;
    private SurfaceView surfaceview;
    private SurfaceHolder surfaceHolder;
    private Camera camera;
    private Parameters parameters;
    private int width = 1280;
    private int height = 720;
    private int fps = 25;
    private int bitrate = 90000;
    private int timespan = 90000 / fps;
    private long time;
    private BufferedOutputStream outputStream;
    private listener l = new listener() {

        @Override
        public void h264data(byte[] buffer, int length) {
            // TODO Auto-generated method stub
            try {
                Log.e(Thread.currentThread().getId() + " h264data", " " + buffer.length + " " + length);
                outputStream.write(buffer, 0, buffer.length);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        surfaceview = (SurfaceView) findViewById(R.id.surfaceview);
        surfaceHolder = surfaceview.getHolder();
        surfaceHolder.addCallback(this);
        x264 = new x264sdk(l);
        createfile();
    }

    private void createfile() {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
        try {
            outputStream = new BufferedOutputStream(new FileOutputStream(file));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        // TODO Auto-generated method stub
        Log.e(Thread.currentThread().getId() + " onPreviewFrame", " " + data.length);
        time += timespan;
        byte[] yuv420 = new byte[width * height * 3 / 2];
        YUV420SP2YUV420(data, yuv420, width, height);
        x264.PushOriStream(yuv420, yuv420.length, time);
    }


    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        x264.initX264Encode(width, height, fps, bitrate);
        camera = getBackCamera();
        startcamera(camera);
    }


    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        // TODO Auto-generated method stub

    }


    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        // TODO Auto-generated method stub
        if (null != camera) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
        x264.CloseX264Encode();
        try {
            outputStream.flush();
            outputStream.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void startcamera(Camera mCamera) {
        if (mCamera != null) {
            try {
                mCamera.setPreviewCallback(this);
                mCamera.setDisplayOrientation(90);
                if (parameters == null) {
                    parameters = mCamera.getParameters();
                }
                parameters = mCamera.getParameters();
                parameters.setPreviewFormat(ImageFormat.NV21);
                parameters.setPreviewSize(width, height);
                parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
                mCamera.setParameters(parameters);
                mCamera.setPreviewDisplay(surfaceHolder);
                mCamera.startPreview();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @TargetApi(9)
    private Camera getBackCamera() {
        Camera c = null;
        try {
            c = Camera.open(0); // attempt to get a Camera instance
        } catch (Exception e) {
            e.printStackTrace();
        }
        return c; // returns null if camera is unavailable
    }

    private void YUV420SP2YUV420(byte[] yuv420sp, byte[] yuv420, int width, int height) {
        if (yuv420sp == null || yuv420 == null) return;
        int framesize = width * height;
        int i = 0, j = 0;
        //copy y
        for (i = 0; i < framesize; i++) {
            yuv420[i] = yuv420sp[i];
        }
        i = 0;
        for (j = 0; j < framesize / 2; j += 2) {
            yuv420[i + framesize * 5 / 4] = yuv420sp[j + framesize];
            i++;
        }
        i = 0;
        for (j = 1; j < framesize / 2; j += 2) {
            yuv420[i + framesize] = yuv420sp[j + framesize];
            i++;
        }
    }

}
