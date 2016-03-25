package com.example.mediaencodeanddecodedemo;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Log;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.SurfaceHolder.Callback;


@SuppressWarnings("deprecation")
public class EncodeActivity extends Activity implements SurfaceHolder.Callback, PreviewCallback 
{
	DatagramSocket socket;
	InetAddress address;
	
	AvcEncoder avcCodec;
    public Camera m_camera;  
    SurfaceView   m_prevewview;
    SurfaceHolder m_surfaceHolder;
    int width = 1280;
    int height = 720;
    int framerate = 5;
    int bitrate = 2500000;
    //int bitRate = camera.getFpsRange()[1] * currentSize.width * currentSize.height / 15;
     
    byte[] h264 = new byte[width*height*3/2];
    
    private FileOutputStream file = null;
    private String filename = "/sdcard/camera.h264";
    private int byteOffset = 0;
    private long lastTime = 0;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
        .detectDiskReads()
        .detectDiskWrites()
        .detectAll()   // or .detectAll() for all detectable problems
        .penaltyLog()
        .build());
StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
        .detectLeakedSqlLiteObjects()
        .detectLeakedClosableObjects()
        .penaltyLog()
        .penaltyDeath()
        .build());
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_encode);
		 
		Log.d("Fuck", "width=" + width+ ", height=" + height + ", framerate=" + framerate + ", bitrate=" + bitrate);
		try {
			avcCodec = new AvcEncoder(width,height,framerate,bitrate);
		} catch (IOException e1) {
			Log.d("Fuck", "Fail to AvcEncoder");
		}
		m_prevewview = (SurfaceView) findViewById(R.id.surfaceViewPlay);
		m_surfaceHolder = m_prevewview.getHolder(); 
		m_surfaceHolder.setFixedSize(width, height);
		m_surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		m_surfaceHolder.addCallback((Callback) this);	
		
		try {
			socket = new DatagramSocket();
			address = InetAddress.getByName("192.169.1.222");
		} catch (SocketException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) 
	{
	
	}

	@SuppressLint("NewApi")
	@Override
	public void surfaceCreated(SurfaceHolder arg0) 
	{
		try 
		{
			m_camera = Camera.open();
			m_camera.setPreviewDisplay(m_surfaceHolder);
			Camera.Parameters parameters = m_camera.getParameters();
			parameters.setPreviewSize(width, height);
			parameters.setPictureSize(width, height);
			parameters.setPreviewFormat(ImageFormat.YV12);	
			parameters.set("rotation", 90);
			//parameters.set("orientation", "portrait");
			m_camera.setParameters(parameters);	
			m_camera.setDisplayOrientation(90);
			m_camera.setPreviewCallback((PreviewCallback) this);
			m_camera.startPreview();
			
		} catch (IOException e) 
		{
			e.printStackTrace();
		}	
		
		//
		try {
			file = new FileOutputStream(filename);
		} catch (FileNotFoundException e) {
			Log.d("Fuck", "File open error");
			e.printStackTrace();
		}
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) 
	{
		m_camera.setPreviewCallback(null); //m_camera.stopPreview(); 
		m_camera.release();
		m_camera = null; 
		avcCodec.close();
		try {
			file.flush();
			file.close();
		} catch (IOException e) {
			Log.d("Fuck", "File close error");
			e.printStackTrace();
		}
	}

	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) 
	{
		//Log.v("h264", "h264 start");
		long newTime = System.currentTimeMillis();
		long diff = newTime - lastTime;
		lastTime = newTime;
		//Log.d("Fuck", "                                                      ");
		//Log.d("Fuck", "Time Past: " + diff);
		
		int ret = avcCodec.offerEncoder(data,h264);
		//Log.d("Fuck", "ret length: " + ret);
		//Log.d("Fuck", "h264.length: " + h264.length);
		
		/*try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
		
		if(ret > 0)
		{
			try {		
				byte[] length_bytes = intToBytes(ret);
				file.write(length_bytes);
				file.write(h264, 0, ret);
				//file.flush();
				//byteOffset += h264.length;
				//DatagramPacket packet=new DatagramPacket(h264,ret, address,5000);
				//socket.send(packet);
			} catch (IOException e)
			{
				Log.d("Fuck", "@@@@@@@@ exception: " + e.toString());
			}
		}
		//Log.v("h264", "h264 end");	
	}
	
	public static byte[] intToBytes( int value )   
	{   
	    byte[] src = new byte[4];  
	    src[3] =  (byte) ((value>>24) & 0xFF);  
	    src[2] =  (byte) ((value>>16) & 0xFF);  
	    src[1] =  (byte) ((value>>8) & 0xFF);    
	    src[0] =  (byte) (value & 0xFF);                  
	    return src;   
	}
}