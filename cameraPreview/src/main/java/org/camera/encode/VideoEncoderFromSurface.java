package org.camera.encode;

import android.annotation.SuppressLint;
import android.graphics.SurfaceTexture;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.util.Log;
import android.view.Surface;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;

public class VideoEncoderFromSurface {
	private static final String TAG = "VideoEncoderFromBuffer";
	private static final boolean VERBOSE = true; // lots of logging
	private static final String DEBUG_FILE_NAME_BASE = "/sdcard/Movies/h264";
	// parameters for the encoder
	private static final String MIME_TYPE = "video/avc"; // H.264 Advanced Video
	private static final int FRAME_RATE = 30; // 15fps
	private static final int IFRAME_INTERVAL = FRAME_RATE; // 10 between
															// I-frames
	private static final int TIMEOUT_USEC = 10000;
	private static final int BIT_RATE = 6000000; // bit rate
	private int mWidth;
	private int mHeight;
	private MediaCodec mMediaCodec;
	byte[] mFrameData;
	FileOutputStream mFileOutputStream = null;
	private int mColorFormat;
	private Surface mSurface;

	@SuppressLint("NewApi")
	public VideoEncoderFromSurface(int width, int height) {
		Log.i(TAG, "VideoEncoder()");
		this.mWidth = width;
		this.mHeight = height;
		mFrameData = new byte[this.mWidth * this.mHeight * 3 / 2];
		MediaFormat mediaFormat = MediaFormat.createVideoFormat(MIME_TYPE,this.mWidth, this.mHeight);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, FRAME_RATE);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL,
				IFRAME_INTERVAL);
		if (VERBOSE)
			Log.d(TAG, "format: " + mediaFormat);
		try {
			mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		mMediaCodec.configure(mediaFormat, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		mSurface = mMediaCodec.createInputSurface();
		mMediaCodec.start();

		String fileName = DEBUG_FILE_NAME_BASE + this.mWidth + "x"
				+ this.mHeight + ".mp4";
		Log.i(TAG, "videofile: " + fileName);
		try {
			mFileOutputStream = new FileOutputStream(fileName);
		} catch (IOException e) {
			System.out.println(e);
		} catch (Exception e) {
			System.out.println(e);
		}
	}

	public synchronized long encodeFrame(SurfaceTexture surface/* , byte[] output */) {
		Log.i(TAG, "encodeFrame()");
		long encodedSize = 0;
		
//		mSurface = surface;

		ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
		ByteBuffer[] outputBuffers = mMediaCodec.getOutputBuffers();
		int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
		if (VERBOSE)
			Log.i(TAG, "inputBufferIndex-->" + inputBufferIndex);
		if (inputBufferIndex >= 0) {
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(mFrameData);
			mMediaCodec.queueInputBuffer(inputBufferIndex, 0, mFrameData.length,
					0, 0);
		} else {
			// either all in use, or we timed out during initial setup
			if (VERBOSE)
				Log.d(TAG, "input buffer not available");
		}

		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		int outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,
				0);
		Log.i(TAG, "outputBufferIndex-->" + outputBufferIndex);
		do {
			if (outputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
				// no output available yet
				if (VERBOSE)
					Log.d(TAG, "no output from encoder available");
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
				// not expected for an encoder
				outputBuffers = mMediaCodec.getOutputBuffers();
				if (VERBOSE)
					Log.d(TAG, "encoder output buffers changed");
			} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
				// not expected for an encoder
				MediaFormat newFormat = mMediaCodec.getOutputFormat();
				if (VERBOSE)
					Log.d(TAG, "encoder output format changed: " + newFormat);
			} else if (outputBufferIndex < 0) {
				Log.d(TAG,
						"unexpected result from encoder.dequeueOutputBuffer: "
								+ outputBufferIndex);
			} else {
				ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
				outputBuffer.position(bufferInfo.offset);
				outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
				encodedSize += bufferInfo.size;

				byte[] outData = new byte[bufferInfo.size];
				outputBuffer.get(outData);
				outputBuffer.position(bufferInfo.offset);
				try {
					mFileOutputStream.write(outData);
					Log.i(TAG, "output data size -- > " + outData.length);
				} catch (IOException ioe) {
					Log.w(TAG, "failed writing debug data to file");
					throw new RuntimeException(ioe);
				}
				mMediaCodec.releaseOutputBuffer(outputBufferIndex, false);
			}
			outputBufferIndex = mMediaCodec.dequeueOutputBuffer(bufferInfo,
					TIMEOUT_USEC);
		} while (outputBufferIndex >= 0);

		return encodedSize;
	}

	@SuppressLint("NewApi")
	public void close() {
		try {
			mFileOutputStream.close();
		} catch (IOException e) {
			System.out.println(e);
		} catch (Exception e) {
			System.out.println(e);
		}
		Log.i(TAG, "close()");
		try {
			mMediaCodec.stop();
			mMediaCodec.release();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * NV21 is a 4:2:0 YCbCr, For 1 NV21 pixel: YYYYYYYY VUVU
	 * I420YUVSemiPlanar is a 4:2:0 YUV, For a single I420 pixel: YYYYYYYY UVUV
	 * Apply NV21 to I420YUVSemiPlanar(NV12)
	 * Refer to https://wiki.videolan.org/YUV/
	 */
	private void NV21toI420SemiPlanar(byte[] nv21bytes, byte[] i420bytes, int width,
			int height) {
		System.arraycopy(nv21bytes, 0, i420bytes, 0, width * height);
		for (int i = width * height; i < nv21bytes.length; i += 2) {
			i420bytes[i] = nv21bytes[i + 1];
			i420bytes[i + 1] = nv21bytes[i];
		}
	}

	/**
	 * Returns a color format that is supported by the codec and by this test
	 * code. If no match is found, this throws a test failure -- the set of
	 * formats known to the test should be expanded for new platforms.
	 */
	private static int selectColorFormat(MediaCodecInfo codecInfo,
			String mimeType) {
		MediaCodecInfo.CodecCapabilities capabilities = codecInfo
				.getCapabilitiesForType(mimeType);
		for (int i = 0; i < capabilities.colorFormats.length; i++) {
			int colorFormat = capabilities.colorFormats[i];
			if (isRecognizedFormat(colorFormat)) {
				return colorFormat;
			}
		}
		Log.e(TAG,
				"couldn't find a good color format for " + codecInfo.getName()
						+ " / " + mimeType);
		return 0; // not reached
	}

	/**
	 * Returns true if this is a color format that this test code understands
	 * (i.e. we know how to read and generate frames in this format).
	 */
	private static boolean isRecognizedFormat(int colorFormat) {
		switch (colorFormat) {
		// these are the formats we know how to handle for this test
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			return true;
		default:
			return false;
		}
	}

	/**
	 * Returns the first codec capable of encoding the specified MIME type, or
	 * null if no match was found.
	 */
	private static MediaCodecInfo selectCodec(String mimeType) {
		int numCodecs = MediaCodecList.getCodecCount();
		for (int i = 0; i < numCodecs; i++) {
			MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
			if (!codecInfo.isEncoder()) {
				continue;
			}
			String[] types = codecInfo.getSupportedTypes();
			for (int j = 0; j < types.length; j++) {
				if (types[j].equalsIgnoreCase(mimeType)) {
					return codecInfo;
				}
			}
		}
		return null;
	}

	/**
	 * Generates the presentation time for frame N, in microseconds.
	 */
	private static long computePresentationTime(int frameIndex) {
		return 132 + frameIndex * 1000000 / FRAME_RATE;
	}

	/**
	 * Returns true if the specified color format is semi-planar YUV. Throws an
	 * exception if the color format is not recognized (e.g. not YUV).
	 */
	private static boolean isSemiPlanarYUV(int colorFormat) {
		switch (colorFormat) {
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
			return false;
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
		case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
			return true;
		default:
			throw new RuntimeException("unknown format " + colorFormat);
		}
	}
}
