package com.angcyo.pldroiddemo;

import android.hardware.Camera;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.WindowManager;

import com.pili.pldroid.streaming.CameraStreamingManager;
import com.pili.pldroid.streaming.CameraStreamingSetting;
import com.pili.pldroid.streaming.StreamingProfile;
import com.pili.pldroid.streaming.SurfaceTextureCallback;
import com.pili.pldroid.streaming.widget.AspectFrameLayout;

import java.util.Map;

public class MainActivity extends AppCompatActivity implements SurfaceTextureCallback
//        implements
//        CameraPreviewFrameView.Listener,
//        CameraStreamingManager.StreamingStateListener,
//        SurfaceTextureCallback,
//        CameraStreamingManager.StreamingSessionListener
{

    String TAG = "MainActivity-->";

    CameraStreamingManager mCameraStreamingManager;
    private StreamingProfile mProfile;
    private Map<Integer, Integer> mSupportVideoQualities;

    private FBO mFBO = new FBO();

    CameraPreviewFrameView cameraPreviewFrameView;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AspectFrameLayout afl = (AspectFrameLayout) findViewById(R.id.cameraPreview_afl);
        afl.setShowMode(AspectFrameLayout.SHOW_MODE.FULL);
        cameraPreviewFrameView = (CameraPreviewFrameView) findViewById(R.id.cameraPreview_surfaceView);

//        GPUImage gpuImage = new GPUImage(this);
//        gpuImage.setGLSurfaceView(cameraPreviewFrameView);
//        gpuImage.setFilter(new GPUImageContrastFilter(2.0f));

//        cameraPreviewFrameView.setListener(this);

        StreamingProfile.VideoProfile vProfile = new StreamingProfile.VideoProfile(30, 1000 * 1024);
        StreamingProfile.AVProfile avProfile = new StreamingProfile.AVProfile(vProfile, null);


        mProfile = new StreamingProfile();
        mProfile.setVideoQuality(StreamingProfile.VIDEO_QUALITY_LOW3)
                .setAudioQuality(StreamingProfile.AUDIO_QUALITY_MEDIUM2)
                .setEncodingSizeLevel(StreamingProfile.VIDEO_ENCODING_HEIGHT_480)
//                .setStream(stream)
                .setAVProfile(avProfile)
                .setSendingBufferProfile(new StreamingProfile.SendingBufferProfile(0.2f, 0.8f, 3.0f, 20 * 1000));
//
//        mSupportVideoQualities = mProfile.getSupportVideoQualities();

        CameraStreamingSetting setting = new CameraStreamingSetting();
        setting.setCameraId(Camera.CameraInfo.CAMERA_FACING_BACK)
                .setContinuousFocusModeEnabled(true)
                .setCameraPrvSizeLevel(CameraStreamingSetting.PREVIEW_SIZE_LEVEL.MEDIUM)//SMALL
                .setCameraPrvSizeRatio(CameraStreamingSetting.PREVIEW_SIZE_RATIO.RATIO_16_9);

        mCameraStreamingManager = new CameraStreamingManager(this, afl, cameraPreviewFrameView, CameraStreamingManager.EncodingType.HW_VIDEO_WITH_HW_AUDIO_CODEC);
        mCameraStreamingManager.prepare(setting, mProfile);
//        mCameraStreamingManager.setStreamingPreviewCallback(this);

//        mCameraStreamingManager.setStreamingStateListener(this);
        mCameraStreamingManager.setSurfaceTextureCallback(this);
//        mCameraStreamingManager.setStreamingSessionListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mCameraStreamingManager.resume();
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mCameraStreamingManager.pause();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraStreamingManager.destroy();
    }

//    @Override
//    public void onPreviewFrame(byte[] bytes, Camera camera) {
//        Log.e(TAG, "onPreviewFrame: ");
//    }
//
//    @Override
//    public boolean onPreviewFrame(byte[] bytes, int i, int i1) {
//        Log.e(TAG, "onPreviewFrame: 2");
//        return false;
//    }

//    @Override
//    public boolean onSingleTapUp(MotionEvent e) {
//        return false;
//    }
//
//    @Override
//    public boolean onZoomValueChanged(float factor) {
//        return false;
//    }

//    @Override
//    public void onStateChanged(int i, Object o) {
//    }
//
//    @Override
//    public boolean onStateHandled(int i, Object o) {
//        return false;
//    }

    @Override
    public void onSurfaceCreated() {
        Log.e(TAG, "onSurfaceCreated: ");
        mFBO.initialize(this);
    }

    @Override
    public void onSurfaceChanged(int width, int height) {
        mFBO.updateSurfaceSize(width, height);
        Log.e(TAG, "onSurfaceChanged: " + width + "  " + height);
    }

    @Override
    public void onSurfaceDestroyed() {
        Log.e(TAG, "onSurfaceDestroyed: ");

        mFBO.release();
    }

    @Override
    public int onDrawFrame(int texId, int texWidth, int texHeight) {
        Log.e(TAG, "onDrawFrame: --->" + texWidth + " " + texHeight);

//        return texId;
        return mFBO.drawFrame(texId, texWidth, texHeight);
    }

//    @Override
//    public boolean onRecordAudioFailedHandled(int i) {
//        return false;
//    }
//
//    @Override
//    public boolean onRestartStreamingHandled(int i) {
//        return false;
//    }
//
//    @Override
//    public Camera.Size onPreviewSizeSelected(List<Camera.Size> list) {
//        return null;
//    }
}
