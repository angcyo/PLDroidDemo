package com.example.h264codecstreamer;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;

import java.io.IOException;

public class MainStreamerActivity extends Activity implements
		SurfaceHolder.Callback {

	Camera camera;
	SurfaceView surfaceView;
	SurfaceHolder surfaceHolder;
	static boolean previewing = false;
	long currTime, oldTime = 0;

	public static int frameRate = 15;
	public static int width = 1920;
	public static int height = 1080;
	public static int bitrate = 3000000;

	private int mCount = 0;
	private AvcEncoder avcEncode = new AvcEncoder();

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button buttonStartCameraPreview = (Button) findViewById(R.id.startcamerapreview);
		Button buttonStopCameraPreview = (Button) findViewById(R.id.stopcamerapreview);

		getWindow().setFormat(PixelFormat.UNKNOWN);
		surfaceView = (SurfaceView) findViewById(R.id.surfaceview);
		surfaceHolder = surfaceView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		buttonStartCameraPreview
				.setOnClickListener(new Button.OnClickListener() {

				
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						
						if (!previewing) {
							camera = Camera.open();
							if (camera != null) {
								try {
									camera.setPreviewDisplay(surfaceHolder);

									Parameters parameters = camera
											.getParameters();
									parameters.setPreviewSize(width, height);
									parameters
											.setPreviewFormat(ImageFormat.YV12);
									parameters.setPreviewFrameRate(frameRate);
									parameters
											.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
									camera.setParameters(parameters);
									camera.setDisplayOrientation(90);

									camera.setPreviewCallback(new Camera.PreviewCallback() {
										@Override
										public void onPreviewFrame(
												byte[] bytes, Camera camera) {
										
												avcEncode.frameEncode(bytes, mCount);
											mCount++;
										}
									});

									camera.startPreview();
									previewing = true;

								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}
				});

		buttonStopCameraPreview
				.setOnClickListener(new Button.OnClickListener() {

					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						if (camera != null && previewing) {
							camera.stopPreview();
							camera.setPreviewCallback(null);
							camera.release();
							camera = null;
							avcEncode.close();
							previewing = false;
//							finish();
						}
					}
				});

	}

	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		// TODO Auto-generated method stub

	}
	
	public static boolean getPreviewStatus() {
		return previewing;
	}
}
