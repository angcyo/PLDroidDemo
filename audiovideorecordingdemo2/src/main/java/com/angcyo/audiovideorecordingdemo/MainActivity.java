package com.angcyo.audiovideorecordingdemo;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.SurfaceTexture;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import com.angcyo.audiovideorecordingdemo.rencoder.FileUtils;

import java.io.FileNotFoundException;

public class MainActivity extends AppCompatActivity implements CameraWrapper.CamOpenOverCallback {

    CameraTexturePreview mCameraTexturePreview;
    public static final String TAG = "MainActivity";

    /**
     * 创建缩略图
     */
    public static Bitmap createThumbnail(Bitmap source, int width, int height) {
        return ThumbnailUtils.extractThumbnail(source, width, height);
    }

    /**
     * 创建视频缩略图
     */
    public static Bitmap createVideoThumbnail(String filePath, int kind) {
        return ThumbnailUtils.createVideoThumbnail(filePath, kind);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.e(TAG, "onCreate");

        CrashHandler crashHandler = CrashHandler.getInstance();

        // 注册crashHandler
        crashHandler.init(getApplicationContext());

        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mCameraTexturePreview = (CameraTexturePreview) findViewById(R.id.camera_textureview);
        mCameraTexturePreview.setAlpha(0.5f);

        Paint blurPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ColorMatrix cm = new ColorMatrix();
        cm.setSaturation(0);
        blurPaint.setColorFilter(new ColorMatrixColorFilter(cm));
//        blurPaint.setMaskFilter(new BlurMaskFilter(25, BlurMaskFilter.Blur.NORMAL));
//        blurPaint.setShader(new RadialGradient(0.5f, 0.5f, 0.2f, Color.BLACK, Color.TRANSPARENT, Shader.TileMode.MIRROR));
        mCameraTexturePreview.setLayerType(View.LAYER_TYPE_HARDWARE, blurPaint);

        findViewById(R.id.crash).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                crash(v);
            }
        });

        findViewById(R.id.startAndStop).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startAndStop(v);
            }
        });
        findViewById(R.id.takePhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takePhoto(v);
            }
        });
        findViewById(R.id.takeVideoPhoto).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                takeVideoPhoto(v);
            }
        });
        findViewById(R.id.switchCamera).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switchCamera(v);
            }
        });

        findViewById(R.id.startActivity).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, Main2Activity.class);
                startActivity(intent);
            }
        });

        openCamera();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }

    public void switchCamera(View view) {
        CameraWrapper.getInstance().doStopCamera();
        CameraWrapper.getInstance().switchCameraId();
        openCamera();
    }

    public void takeVideoPhoto(View view) {
        new Thread() {
            @Override
            public void run() {
                Bitmap videoThumbnail = ThumbnailUtils.createVideoThumbnail("/storage/sdcard1/dudu/video4/2016_04_09_14_41.mp4", MediaStore.Video.Thumbnails.MINI_KIND);
                Bitmap videoThumbnail2 = ThumbnailUtils.createVideoThumbnail("/storage/sdcard1/dudu/video4/2016_04_09_14_41.mp4", MediaStore.Video.Thumbnails.MICRO_KIND);

                try {
                    FileUtils.saveBitmap(videoThumbnail, FileUtils.getPhotoSaveFilePath("MINI_KIND"));
                    FileUtils.saveBitmap(videoThumbnail2, FileUtils.getPhotoSaveFilePath("MICRO_KIND"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void takePhoto(View view) {
        final Bitmap bitmap = mCameraTexturePreview.getBitmap();
        new Thread() {
            @Override
            public void run() {
                try {
                    FileUtils.saveBitmap(bitmap, FileUtils.getPhotoSaveFilePath());
                    FileUtils.saveBitmap(ThumbnailUtils.extractThumbnail(bitmap, 20, 20), FileUtils.getPhotoSaveFilePath(
                            FileUtils.simpleDateFormat.format(System.currentTimeMillis()) + "_20_20"));
                    FileUtils.saveBitmap(ThumbnailUtils.extractThumbnail(bitmap, 60, 60), FileUtils.getPhotoSaveFilePath(
                            FileUtils.simpleDateFormat.format(System.currentTimeMillis()) + "_60_60"));
                    FileUtils.saveBitmap(ThumbnailUtils.extractThumbnail(bitmap, 100, 100), FileUtils.getPhotoSaveFilePath(
                            FileUtils.simpleDateFormat.format(System.currentTimeMillis()) + "_100_100"));
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    public void startAndStop(View view) {
        String tag = (String) view.getTag();
        if (tag.equalsIgnoreCase("stop")) {
            CameraWrapper.getInstance().doStopCamera();
            view.setTag("start");
            ((TextView) view).setText("开始");
        } else {
            openCamera();
            view.setTag("stop");
            ((TextView) view).setText("停止");
        }
    }

    public void crash(View view) {
        throw new IllegalArgumentException("崩溃测试...");
    }


    @Override
    protected void onStart() {
        super.onStart();

        Log.e(TAG, "onStart");
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
