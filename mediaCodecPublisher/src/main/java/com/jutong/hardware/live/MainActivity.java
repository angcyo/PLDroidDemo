package com.jutong.hardware.live;

import android.annotation.TargetApi;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class MainActivity extends Activity implements Callback, PreviewCallback {
	private SurfaceView surface;
	private Camera mCamera;
	private VideoEncoder avcEncoder;
	private AudioEncoder audioEncoder;

	static {
		System.loadLibrary("myjni");
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		surface = (SurfaceView) findViewById(R.id.surface);
		surface.getHolder().setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		surface.getHolder().addCallback(this);
		avcEncoder = new VideoEncoder();
		avcEncoder.connect();
		audioEncoder = new AudioEncoder();
		new Thread() {
			public void run() {
				int minBufferSize = AudioRecord.getMinBufferSize(44100,
						AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT);
				AudioRecord audioRecord = new AudioRecord(
						MediaRecorder.AudioSource.MIC, 44100,
						AudioFormat.CHANNEL_IN_MONO,
						AudioFormat.ENCODING_PCM_16BIT, minBufferSize);
				audioRecord.startRecording();
				while (true) {
					byte[] buffer = new byte[2048];
					int len = audioRecord.read(buffer, 0, buffer.length);
					if (0 < len) {
//						audioEncoder.fireAudio(buffer, len);
					}
				}
			};
		}.start();
	}


	@Override
	public void surfaceCreated(SurfaceHolder holder) {


		
	}
	

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		try {
			mCamera = Camera.open(0);
			Camera.Parameters parameters = mCamera.getParameters();
			parameters.setPreviewSize(320, 240);
			avcEncoder.setVideoOptions(parameters.getPreviewSize().width,
					parameters.getPreviewSize().height, 320000, 25);
			parameters.setPreviewFormat(ImageFormat.NV21);
			mCamera.setParameters(parameters);
			mCamera.setPreviewCallback(this);
			mCamera.setPreviewDisplay(holder);
			mCamera.startPreview();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {

	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		 avcEncoder.fireVideo(data);
	}

}
