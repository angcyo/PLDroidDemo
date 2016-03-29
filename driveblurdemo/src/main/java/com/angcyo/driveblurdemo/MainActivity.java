package com.angcyo.driveblurdemo;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.WindowManager;

import org.wysaid.camera.CameraInstance;
import org.wysaid.view.CameraRecordGLSurfaceView;

public class MainActivity extends AppCompatActivity {

    public static final String effectConfigs[] = {
            "",
            "#unpack @blur lerp 0.8" //可调节模糊强度
    };
    private VideoEncoderFromBuffer videoEncoder = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final CameraRecordGLSurfaceView surfaceView = (CameraRecordGLSurfaceView) findViewById(R.id.surfaceView);

        surfaceView.postDelayed(new Runnable() {
            @Override
            public void run() {
                surfaceView.setFilterWithConfig(effectConfigs[1]);
            }
        }, 100);


        surfaceView.setCameraOpenCallback(new CameraInstance.CameraOpenCallback() {
            @Override
            public void cameraReady() {
                initVideoEncoder(surfaceView.cameraInstance().previewWidth(), surfaceView.cameraInstance().previewHeight());

                surfaceView.cameraInstance().setPreviewCallback(new Camera.PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
//                Log.e(MainActivity.this.getClass().getSimpleName(), " " + data.length);
                        videoEncoder.encodeFrame(data);
                    }
                });
            }
        });

    }

    private void initVideoEncoder(int width, int height) {
//        videoEncoder = new VideoEncoderFromBuffer(CameraInstance.mPreferPreviewWidth, CameraInstance.mPreferPreviewHeight);
        videoEncoder = new VideoEncoderFromBuffer(width, height);
    }
}
