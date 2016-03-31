package com.angcyo.audiovideorecordingdemo;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

public class MainActivity extends AppCompatActivity implements CameraWrapper.CamOpenOverCallback {

    CameraTexturePreview mCameraTexturePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCameraTexturePreview = (CameraTexturePreview) findViewById(R.id.camera_textureview);
    }

    public void switchCamera(View view) {
        CameraWrapper.getInstance().doStopCamera();
        CameraWrapper.getInstance().switchCameraId();
        openCamera();
    }

    @Override
    protected void onStart() {
        super.onStart();

        openCamera();
    }

    private void openCamera() {
        Thread openThread = new Thread() {
            @Override
            public void run() {
                CameraWrapper.getInstance().doOpenCamera(MainActivity.this);
            }
        };
        openThread.start();
    }

    @Override
    public void cameraHasOpened() {
        SurfaceTexture surface = this.mCameraTexturePreview.getSurfaceTexture();
        CameraWrapper.getInstance().doStartPreview(surface);
    }
}
