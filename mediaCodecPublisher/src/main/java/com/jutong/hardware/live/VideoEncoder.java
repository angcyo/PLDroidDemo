package com.jutong.hardware.live;

import java.nio.ByteBuffer;
import java.util.Arrays;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class VideoEncoder {

	private final static String MINE_TYPE = "video/avc";
	private MediaCodec mediaCodec;
	private int mWidth;
	private int mHeight;
	private int mBit;
	private int mFps;
	private byte[] sps;
	private byte[] pps;
	private byte[] h264;
	private String codecName;

	

	public VideoEncoder() {
		initialize();
	}

	protected void initialize() {
		for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
			MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
			for (String type : mediaCodecInfo.getSupportedTypes()) {
				if (TextUtils.equals(type, MINE_TYPE)
						&& mediaCodecInfo.isEncoder()) {
					CodecCapabilities codecCapabilities = mediaCodecInfo
							.getCapabilitiesForType(MINE_TYPE);
					for (int format : codecCapabilities.colorFormats) {
						if (format == MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar) {
							codecName = mediaCodecInfo.getName();
							return;
						}
					}
				}
			}
		}
	}

	public boolean isSupport() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

		}
		return false;
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	public void setVideoOptions(int width, int height, int bit, int fps) {
		mWidth = width;
		mHeight = height;
		mBit = bit;
		mFps = fps;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
			try {
				mediaCodec = MediaCodec.createByCodecName(codecName);
				MediaFormat mediaFormat = MediaFormat.createVideoFormat(
						MINE_TYPE, width, height);

				mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, bit);
				mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
				mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1); // 关键帧间隔时间
																				// 单位s
				mediaFormat
						.setInteger(
								MediaFormat.KEY_COLOR_FORMAT,
								MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
				// mediaFormat.setInteger(MediaFormat.KEY_PROFILE,
				// CodecProfileLevel.AVCProfileBaseline);
				// mediaFormat.setInteger(MediaFormat.KEY_LEVEL,
				// CodecProfileLevel.AVCLevel52);
				mediaCodec.configure(mediaFormat, null, null,
						MediaCodec.CONFIGURE_FLAG_ENCODE);
				mediaCodec.start();
			} catch (Exception e) {
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
	}

	public void fireVideo(byte[] data) {
		h264 = new byte[mWidth * mHeight * 3 / 2];
		byte[] rawData = nv212nv12(data);
		// 获得编码器输入输出数据缓存区 API:21之后可以使用
		// mediaCodec.getInputBuffer(mediaCodec.dequeueInputBuffer(-1));直接获得缓存数据
		ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
		ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
		// 获得有效输入缓存区数组下标 -1表示一直等待
		int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
		Log.d("DEMO", "输入:" + inputBufferIndex);
		if (inputBufferIndex >= 0) {
			// 将原始数据填充 inputbuffers
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(rawData);
			//将此数据加入编码队列 参数3：需要一个增长的时间戳，不然无法持续编码
			mediaCodec.queueInputBuffer(inputBufferIndex, 0, rawData.length,
					System.nanoTime(), 0);
		}
		//获得编码后的数据
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		//有效数据下标
		int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
		Log.d("DEMO", "输出:" + outputBufferIndex);
		while (outputBufferIndex >= 0) {
			ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
			byte[] outData = new byte[bufferInfo.size];
			outputBuffer.get(outData);
			System.out.println("type:" + outData[4]);
			if ((outData[4] & 0x1f) == 7) { // sps pps MediaCodec会在编码第一帧之前输出sps+pps sps pps加在一起
				// sps = new byte[outData.length - 4];
				// System.arraycopy(outData, 4, sps, 0, outData.length - 4);
				Log.d("DEMO", "sps pps:" + Arrays.toString(outData));
				for (int i = 0; i < outData.length; i++) {
					if (i + 4 < outData.length) { // 保证不越界
						if (outData[i] == 0x00 && outData[i + 1] == 0x00
								&& outData[i + 2] == 0x00
								&& outData[i + 3] == 0x01) {
							//在这里将sps pps分开
							// if ((outData[i + 4] & 0x1f) == 7) { // & 0x1f =7
							// sps
							//
							// } else
							//sps pps数据如下: 0x00 0x00 0x00 0x01 7 sps 0x00 0x00 0x00 0x01 8 pps
							if ((outData[i + 4] & 0x1f) == 8) {// & 0x1f =8 pps
								//去掉界定符
								sps = new byte[i - 4];
								System.arraycopy(outData, 4, sps, 0, sps.length);
								pps = new byte[outData.length
										- (4 + sps.length) - 4];
								System.arraycopy(outData, 4 + sps.length + 4,
										pps, 0, pps.length);
								break;
							}
						}
					}
				}
				Log.d("DEMO", "sps :" + Arrays.toString(sps));
				Log.d("DEMO", "sps :" + Arrays.toString(pps));
			} else {
				// (outData[4] & 0x1f) == 5) 关键帧 outData[4] == 0x65
				System.arraycopy(outData, 4, h264, 0, outData.length - 4);
				Log.d("DEMO", outData.length + "");
				Log.d("DEMO", "帧数据 sps:" + sps.length + "  pps:" + pps.length
						+ " 264:" + h264.length);
				send(sps, pps, h264, sps.length, pps.length, outData.length - 4);
			}
			// if (sps_pps != null) { // 已经获得过sps pps
			// System.arraycopy(outData, 0, h264, 0, outData.length);
			// if (h264[4] == 0x65) {// 关键帧 h264[4]&0x1f==5 关键帧
			// Log.d("DEMO", "关键帧");
			// } else {
			// Log.d("DEMO", "不是关键帧");
			// }
			// send(sps_pps, h264);
			// } else {
			// ByteBuffer spsPpsBuffer = ByteBuffer.wrap(outData);
			// // 0x00 0x00 0x00 0x01
			// if (spsPpsBuffer.getInt() == 0x00000001) {
			// sps_pps = new byte[outData.length];
			// System.arraycopy(outData, 0, sps_pps, 0, outData.length);
			// Log.d("DEMO", "sps pps信息");
			// } else {
			// Log.d("DEMO", "错误 未获得sps pps信息 丢帧");
			// }
			// }
			// 释放编码后的数据
			mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
			// 重新获得编码bytebuffer下标
			outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
			Log.d("DEMO", "完成 输出:" + outputBufferIndex);
		}
		// send(sps_pps, h264);
		// mediaCodec.flush();
	}

	private native void send(byte[] sps, byte[] pps, byte[] h264, int sps_len,
			int pps_len, int h264_len);

	private native void conversjni(byte[] data, byte[] rawData);

	public native void connect();

	private byte[] nv212nv12(byte[] data) {
		int len = mWidth * mHeight;
		byte[] buffer = new byte[len * 3 / 2];
		byte[] y = new byte[len];
		byte[] uv = new byte[len / 2];
		System.arraycopy(data, 0, y, 0, len);
		for (int i = 0; i < len / 4; i++) {
			uv[i * 2] = data[len + i * 2 + 1];
			uv[i * 2 + 1] = data[len + i * 2];
		}
		System.arraycopy(y, 0, buffer, 0, y.length);
		System.arraycopy(uv, 0, buffer, y.length, uv.length);
		return buffer;
	}

}
