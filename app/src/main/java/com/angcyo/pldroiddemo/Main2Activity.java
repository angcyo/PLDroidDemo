package com.angcyo.pldroiddemo;

import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ImageView;

import com.angcyo.yuvtorbga.GPUImageNativeLibrary;

import org.wysaid.view.CameraRecordGLSurfaceView;

public class Main2Activity extends AppCompatActivity {

    CameraRecordGLSurfaceView cameraView;
    ImageView imageView;
    Bitmap bitmap;

    static String TAG = "Main2Activity";

    public static final String effectConfigs[] = {
            "",
            "@beautify bilateral 10 4 1 @style haze -0.5 -0.5 1 1 1 @curve RGB(0, 0)(94, 20)(160, 168)(255, 255) @curve R(0, 0)(129, 119)(255, 255)B(0, 0)(135, 151)(255, 255)RGB(0, 0)(146, 116)(255, 255)",
            "#unpack @blur lerp 0.5", //可调节模糊强度
            "@blur lerp 1", //可调节混合强度
            "#unpack @dynamic wave 1", //可调节速度
            "@dynamic wave 0.5",       //可调节混合
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);

        imageView = (ImageView) findViewById(R.id.image);


        cameraView = (CameraRecordGLSurfaceView) findViewById(R.id.myGLSurfaceView);
        cameraView.postDelayed(new Runnable() {
            @Override
            public void run() {
                cameraView.setFilterWithConfig(effectConfigs[2]);
            }
        }, 3000);

        cameraView.cameraInstance().setPreviewCallback(new Camera.PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                Log.e("onPreviewFrame-->", "" + data.length);

//                int cw = cameraView.getWidth();
//                int ch = cameraView.getHeight();
                int cw = camera.getParameters().getPreviewSize().width;
                int ch = camera.getParameters().getPreviewSize().height;

                int[] rgb = new int[cw * ch];

                long lastTime = System.currentTimeMillis();
                GPUImageNativeLibrary.YUVtoRBGA(data, cw, ch, rgb);
                Log.i(TAG, "decodeYUV420SP time:" + (System.currentTimeMillis() - lastTime));

                if (bitmap != null) {
                    bitmap.recycle();
                }
                bitmap = Bitmap.createBitmap(rgb, cw, ch, Bitmap.Config.ARGB_8888);
                imageView.setImageBitmap(bitmap);
            }
        });
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
    }
}
