package org.camera.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.ImageView;

import com.angcyo.yuvtorbga.GPUImageNativeLibrary;
import com.example.camerapreview.R;

import org.camera.FastBlur;
import org.camera.camera.CameraWrapper;
import org.camera.camera.CameraWrapper.CamOpenOverCallback;
import org.camera.preview.CameraTexturePreview;

import java.util.concurrent.ArrayBlockingQueue;

@SuppressLint("NewApi")
public class CameraSurfaceTextureActivity extends Activity implements CamOpenOverCallback {
    private static final String TAG = "CameraPreviewActivity";
    public static int MSG_BITMAP = 1;
    public MainHandler mainHandler;
    ImageView imageView;
    Bitmap bitmap;
    boolean isBlur = false;
    BlurRunnable blurRunnable;
    private CameraTexturePreview mCameraTexturePreview;
    private float mPreviewRate = -1f;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        initUI();
        initViewParams();

        mainHandler = new MainHandler();
        blurRunnable = new BlurRunnable();
        new Thread(blurRunnable).start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        blurRunnable.exit();
    }

    @Override
    protected void onStart() {
        Log.i(TAG, "onStart");
        super.onStart();

        Thread openThread = new Thread() {
            @Override
            public void run() {
                CameraWrapper.getInstance().doOpenCamera(CameraSurfaceTextureActivity.this);
            }
        };
        openThread.start();
    }

    private void initUI() {
        mCameraTexturePreview = (CameraTexturePreview) findViewById(R.id.camera_textureview);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        findViewById(R.id.blur).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isBlur) {
                    mCameraTexturePreview.setVisibility(View.VISIBLE);
                    imageView.setScaleType(ImageView.ScaleType.CENTER);
                } else {
                    mCameraTexturePreview.setVisibility(View.INVISIBLE);
                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
                }

                isBlur = !isBlur;
            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);
        imageView.setRotation(90);
    }

    private void initViewParams() {
        LayoutParams params = mCameraTexturePreview.getLayoutParams();
        DisplayMetrics displayMetrics = this.getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        params.width = screenWidth;
        params.height = screenHeight;
        this.mPreviewRate = (float) screenHeight / (float) screenWidth;
        mCameraTexturePreview.setLayoutParams(params);
    }

    @Override
    public void cameraHasOpened() {
        SurfaceTexture surface = this.mCameraTexturePreview.getSurfaceTexture();
        CameraWrapper.getInstance().doStartPreview(surface, mPreviewRate);

        CameraWrapper.getInstance().setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] bytes, Camera camera) {
                blurRunnable.add(bytes);
            }
        });
    }

    class MainHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what == MSG_BITMAP) {
                if (bitmap != null) {
                    bitmap.recycle();
                }
                bitmap = (Bitmap) msg.obj;
                imageView.setImageBitmap(bitmap);
            }
        }
    }

    class BlurRunnable implements Runnable {

        ArrayBlockingQueue<byte[]> bytes = new ArrayBlockingQueue<byte[]>(30);
        private boolean isExit = false;
        private Object lock = new Object();

        public void add(byte[] data) {
            synchronized (lock) {
                bytes.add(data);
                lock.notifyAll();
            }
        }

        public void exit() {
            synchronized (lock) {
                isExit = true;
                lock.notifyAll();
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

                        int cw = CameraWrapper.IMAGE_WIDTH;
                        int ch = CameraWrapper.IMAGE_HEIGHT;

                        int[] rgb = new int[cw * ch];

                        long lastTime = System.currentTimeMillis();
                        GPUImageNativeLibrary.YUVtoRBGA(bytes, cw, ch, rgb);
                        Log.i(TAG, "decodeYUV420SP time:" + (System.currentTimeMillis() - lastTime));

                        Bitmap bitmap = Bitmap.createBitmap(rgb, cw, ch, Bitmap.Config.ARGB_8888);

                        int width = cw;
                        int height = ch;

                        mainHandler.sendMessage(mainHandler.obtainMessage(MSG_BITMAP, isBlur ? FastBlur.blur(bitmap, width, height) : bitmap));
                    }
                }
            }
        }
    }
}
