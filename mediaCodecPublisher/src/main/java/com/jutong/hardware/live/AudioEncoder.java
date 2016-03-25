package com.jutong.hardware.live;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;

import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCrypto;
import android.media.MediaMuxer;
import android.media.MediaCodecInfo.AudioCapabilities;
import android.media.MediaFormat;
import android.media.MediaCodecInfo.CodecCapabilities;
import android.media.MediaCodecInfo.CodecProfileLevel;
import android.media.MediaCodecList;
import android.os.Build;
import android.text.TextUtils;
import android.util.Log;

public class AudioEncoder {

	private final static String MINE_TYPE = "audio/mp4a-latm";
	private MediaCodec mediaCodec;
	private String codecName;

	public AudioEncoder() {
		initialize();
	}

	@TargetApi(Build.VERSION_CODES.LOLLIPOP)
	protected void initialize() {
		for (int i = 0; i < MediaCodecList.getCodecCount(); i++) {
			MediaCodecInfo mediaCodecInfo = MediaCodecList.getCodecInfoAt(i);
			for (String type : mediaCodecInfo.getSupportedTypes()) {
				if (TextUtils.equals(type, MINE_TYPE)
						&& mediaCodecInfo.isEncoder()) {
					codecName = mediaCodecInfo.getName();
					break;
				}
			}
			if (null != codecName) {
				break;
			}
		}
		try {
			mediaCodec = MediaCodec.createByCodecName(codecName);
			MediaFormat mediaFormat = MediaFormat.createAudioFormat(MINE_TYPE,
					44100, 1);
			mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 64000);
			mediaFormat.setInteger(MediaFormat.KEY_AAC_PROFILE,
					CodecProfileLevel.AACObjectLC);
			mediaCodec.configure(mediaFormat, null, null,
					MediaCodec.CONFIGURE_FLAG_ENCODE);
			mediaCodec.start();
		} catch (IOException e) {
			e.printStackTrace();
		}
		AudioTag audioTag = AudioSpecificConfig.getTag(
				CodecProfileLevel.AACObjectLC, 44100, 1);
		sendAAcTag(audioTag.getData());
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void fireAudio(byte[] data, int len) {
		ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
		ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
		int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
		if (inputBufferIndex >= 0) {
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(data);
			mediaCodec.queueInputBuffer(inputBufferIndex, 0, data.length,
					System.nanoTime(), 0);
		}
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
		while (outputBufferIndex >= 0) {
			ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
			byte[] outData = new byte[bufferInfo.size];
			outputBuffer.get(outData);
			send(outData, bufferInfo.size);
			mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
			outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
		}
	}

	private native void send(byte[] data, int leng);

	private native void sendAAcTag(byte[] data);
}
