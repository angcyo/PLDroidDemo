package org.camera.activity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
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

import org.camera.Blur;
import org.camera.camera.CameraWrapper;
import org.camera.camera.CameraWrapper.CamOpenOverCallback;
import org.camera.encode.VideoEncoderFromBuffer;
import org.camera.preview.CameraTexturePreview;

import java.util.concurrent.ArrayBlockingQueue;

@SuppressLint("NewApi")
public class CameraSurfaceTextureActivity extends Activity implements CamOpenOverCallback {
    private static final String TAG = "CameraPreviewActivity";
    public static int MSG_BITMAP = 1;
    public MainHandler mainHandler;
    ImageView imageView;
    Bitmap bitmap;
    volatile boolean isBlur = false;
    BlurRunnable blurRunnable;
    BlurRunnable2 blurRunnable2;
    private CameraTexturePreview mCameraTexturePreview;
    private float mPreviewRate = -1f;
    private Matrix blurMatrix;
    private volatile boolean isDestroy = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_preview);
        initUI();
        initViewParams();

        mainHandler = new MainHandler();
//        blurRunnable = new BlurRunnable();
        blurRunnable2 = new BlurRunnable2();

//        new Thread(blurRunnable2).start();
//        new Thread(blurRunnable).start();

        Blur.init(this);

        blurMatrix = new Matrix();
        float[] blur = new float[9];
        blur[0] = -1.2f;
        blur[1] = -1f;
        blur[2] = -1.2f;

        blur[3] = -1f;
        blur[4] = 20f;
        blur[5] = -1f;

        blur[6] = -1.2f;
        blur[7] = -1f;
        blur[8] = -1.2f;
        blurMatrix.setValues(blur);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        blurRunnable.exit();
        isDestroy = true;
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
                isBlur = !isBlur;

                if (isBlur) {
//                    mCameraTexturePreview.setVisibility(View.INVISIBLE);
//                    imageView.setScaleType(ImageView.ScaleType.CENTER);
//                    mCameraTexturePreview.setTransform(blurMatrix);

//                    Matrix matrix = new Matrix();
//                    mCameraTexturePreview.getTransform(matrix);
//                    matrix.postScale(4, 4);
//                    mCameraTexturePreview.setTransform(matrix);

                } else {
//                    mCameraTexturePreview.setVisibility(View.INVISIBLE);
//                    imageView.setScaleType(ImageView.ScaleType.FIT_XY);
//                    mCameraTexturePreview.setTransform(null);
                }

//                CameraWrapper.getInstance().setBlur(isBlur);

            }
        });

        imageView = (ImageView) findViewById(R.id.imageView);
//        imageView.setRotation(90);
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

        CameraWrapper.getInstance().setMainHandler(mainHandler);


//        CameraWrapper.getInstance().setPreviewCallback(new Camera.PreviewCallback() {
//            @Override
//            public void onPreviewFrame(byte[] bytes, Camera camera) {
//                blurRunnable.add(bytes);
//            }
//        });
    }

    public class MainHandler extends Handler {
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

                    }
                }

                if (!isExit) {
                    byte[] bytes = this.bytes.poll();

                    int cw = CameraWrapper.IMAGE_WIDTH;
                    int ch = CameraWrapper.IMAGE_HEIGHT;

                    int[] rgb = new int[cw * ch];

                    long lastTime = System.currentTimeMillis();
                    byte[] mFrameData = new byte[bytes.length];

                    VideoEncoderFromBuffer.NV21toI420SemiPlanar(bytes, mFrameData, cw, ch);
                    GPUImageNativeLibrary.YUVtoRBGA(mFrameData, cw, ch, rgb);

                    Log.i(TAG, "decodeYUV420SP time:" + (System.currentTimeMillis() - lastTime));

                    Bitmap bitmap = Bitmap.createBitmap(rgb, cw, ch, Bitmap.Config.ARGB_8888);

                    int width = cw;
                    int height = ch;

//                        mainHandler.sendMessage(mainHandler.obtainMessage(MSG_BITMAP, isBlur ? FastBlur.blur(bitmap, width, height) : bitmap));
                    mainHandler.sendMessage(mainHandler.obtainMessage(MSG_BITMAP, isBlur ? Blur.fastBlur(bitmap, 25) : bitmap));
                }
            }
        }
    }

    class BlurRunnable2 implements Runnable {

        @Override
        public void run() {
            while (!isDestroy) {
                long lastTime = System.currentTimeMillis();
                Bitmap temp = mCameraTexturePreview.getBitmap(200, 300);
                if (isBlur) {
                    temp = Blur.fastBlur(temp, 25);
                }
                mainHandler.sendMessage(mainHandler.obtainMessage(MSG_BITMAP, temp));
                Log.i(TAG, "getBitmap time:" + (System.currentTimeMillis() - lastTime));
            }
        }
    }
}
