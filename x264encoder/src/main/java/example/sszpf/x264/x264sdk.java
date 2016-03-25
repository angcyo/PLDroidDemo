package example.sszpf.x264;

import java.nio.ByteBuffer;

public class x264sdk {
	
	public interface listener
	{
		void h264data(byte[] buffer, int length);
	}
	
	private listener _listener;
	
	public x264sdk(listener l){
		_listener = l;
	}
	
	static {
		System.loadLibrary("x264encoder");
	}
	
	private ByteBuffer mVideobuffer;
	
	
	public void PushOriStream(byte[] buffer, int length, long time)
	{
		if (mVideobuffer == null || mVideobuffer.capacity() < length) {
			mVideobuffer = ByteBuffer.allocateDirect(((length / 1024) + 1) * 1024);
		}
		mVideobuffer.rewind();
		mVideobuffer.put(buffer, 0, length);
		encoderH264(length, time);
	}
	
	public native void initX264Encode(int width, int height, int fps, int bite);

	public native int encoderH264(int length, long time);

	public native void CloseX264Encode();
	
	private void H264DataCallBackFunc(byte[] buffer, int length){
		_listener.h264data(buffer, length);
	}

}
