package com.example.h264codecstreamer;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.util.Log;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;

public class AvcEncoder {

	// Encoder
	private MediaCodec mediaCodec;

	// Networking variables
	private int DATAGRAM_PORT = 4002;
	private int TCP_SERVER_PORT = DATAGRAM_PORT + 1;
	private static final int MAX_UDP_DATAGRAM_LEN = 1400;
	private InetAddress clientIp;
	private int clientPort;
	private static boolean isClientConnected = false;

	// FIFO queue
	private static final int MAX_BUFFER_QUEUE_SIZE = 1000000;
	private BufferQueue bufferQueue = new BufferQueue(MAX_BUFFER_QUEUE_SIZE);
	private byte[] sendData = new byte[MAX_UDP_DATAGRAM_LEN];

	// File variables
	File file = new File("/sdcard/sample_" + MainStreamerActivity.frameRate
			+ "_" + MainStreamerActivity.width + "_"
			+ MainStreamerActivity.height + "_" + MainStreamerActivity.bitrate
			+ ".h264");
	FileOutputStream outStream = null;

	public AvcEncoder() {

		Thread udpThread = new Thread() {

			private DatagramPacket dp;
			private DatagramSocket ds;

			@Override
			public void run() {
				try {
					ds = new DatagramSocket(DATAGRAM_PORT);
					dp = new DatagramPacket(sendData, sendData.length);
					ds.receive(dp);
					clientPort = dp.getPort();
					clientIp = dp.getAddress();
					ds.connect(dp.getAddress(), dp.getPort());
					Log.i("Message", " Connected to: " + clientIp + ":"
							+ clientPort);
					isClientConnected = true;

					while (isClientConnected) {
						if (MainStreamerActivity.getPreviewStatus()) {

							if (bufferQueue.getCount() > MAX_UDP_DATAGRAM_LEN) {
								bufferQueue.read(sendData, 0,
										sendData.length);
								DatagramPacket packet = new DatagramPacket(
										sendData, sendData.length, clientIp,
										clientPort);
								ds.send(packet);
							}
						}
					}
					ds.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		udpThread.start();

		Thread tcpThread = new Thread() {

			private ServerSocket acceptSocket;

			@Override
			public void run() {
				try {
					acceptSocket = new ServerSocket(TCP_SERVER_PORT);
					Socket connectionSocket = acceptSocket.accept();
					BufferedReader inFromClient = new BufferedReader(
							new InputStreamReader(
									connectionSocket.getInputStream()));
					DataOutputStream outToClient = new DataOutputStream(
							connectionSocket.getOutputStream());
					String clientSentence = inFromClient.readLine();
					Log.i("TAG", clientSentence);
					isClientConnected = true;

					while (isClientConnected) {

						if (MainStreamerActivity.getPreviewStatus()) {
							if (bufferQueue.getCount() > MAX_UDP_DATAGRAM_LEN) {
								bufferQueue.read(sendData, 0,
										sendData.length);
								outToClient.write(sendData, 0, sendData.length);
							}
						}
					}
					connectionSocket.close();
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		};

		tcpThread.start();

		try {
			mediaCodec = MediaCodec.createEncoderByType("video/avc");
		} catch (IOException e) {
			e.printStackTrace();
		}

		MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc",
				MainStreamerActivity.width, MainStreamerActivity.height);
		mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE,
				MainStreamerActivity.bitrate);
		mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE,
				MainStreamerActivity.frameRate);
		mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT,
				MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar);
		mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
		mediaCodec.configure(mediaFormat, null, null,
				MediaCodec.CONFIGURE_FLAG_ENCODE);
		mediaCodec.start();
	}

	public void close() {
		try {
			mediaCodec.stop();
			mediaCodec.release();
			// outputStream.flush();
			// outputStream.close();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// called from Camera.setPreviewCallbackWithBuffer(...) in other class

	public synchronized void frameEncode(byte[] input, int counter) {
		try {

			ByteBuffer[] inputBuffers = mediaCodec.getInputBuffers();
			ByteBuffer[] outputBuffers = mediaCodec.getOutputBuffers();
			int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
			if (inputBufferIndex >= 0) {
				ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
				inputBuffer.clear();
				inputBuffer.put(input);
				mediaCodec.queueInputBuffer(inputBufferIndex, 0, input.length,
						0, 0);
			} else {
				return;
			}

			MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
			int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo,
					0);
			do {
				if (outputBufferIndex >= 0) {
					ByteBuffer outBuffer = outputBuffers[outputBufferIndex];
					System.out.println("buffer info-->" + bufferInfo.offset
							+ "--" + bufferInfo.size + "--" + bufferInfo.flags
							+ "--" + bufferInfo.presentationTimeUs);
					byte[] outData = new byte[bufferInfo.size];	

					if (bufferInfo.offset != 0) {
						outBuffer.get(outData, bufferInfo.offset,
								outData.length - bufferInfo.offset);
						bufferQueue.append(outData, bufferInfo.offset,
								outData.length - bufferInfo.offset);
					} else {
						outBuffer.get(outData, 0, outData.length);
						bufferQueue.append(outData, 0, outData.length);
					}
					Log.i("TAG", "out data -- > " + outData.length);
					mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
					outputBufferIndex = mediaCodec.dequeueOutputBuffer(
							bufferInfo, 0);
				}

				else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
					outputBuffers = mediaCodec.getOutputBuffers();
				} else if (outputBufferIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
					MediaFormat format = mediaCodec.getOutputFormat();
				}
			} while (outputBufferIndex >= 0);

		} catch (Throwable t) {
			t.printStackTrace();
		}
	}

	private byte[] swapYV12toI420(byte[] yv12bytes, int width, int height) {
		byte[] i420bytes = new byte[yv12bytes.length];
		for (int i = 0; i < width * height; i++)
			i420bytes[i] = yv12bytes[i];
		for (int i = width * height; i < width * height
				+ (width / 2 * height / 2); i++)
			i420bytes[i] = yv12bytes[i + (width / 2 * height / 2)];
		for (int i = width * height + (width / 2 * height / 2); i < width
				* height + 2 * (width / 2 * height / 2); i++)
			i420bytes[i] = yv12bytes[i - (width / 2 * height / 2)];
		return i420bytes;
	}

	private static byte[] YV12toYUV420Planar(byte[] input, int width, int height) {
		/*
		 * COLOR_FormatYUV420Planar is I420 which is like YV12, but with U and V
		 * reversed. So we just have to reverse U and V.
		 */
		byte[] output = new byte[input.length];
		;
		final int frameSize = width * height;
		final int qFrameSize = frameSize / 4;

		System.arraycopy(input, 0, output, 0, frameSize); // Y
		System.arraycopy(input, frameSize, output, frameSize + qFrameSize,
				qFrameSize); // Cr (V)
		System.arraycopy(input, frameSize + qFrameSize, output, frameSize,
				qFrameSize); // Cb (U)

		return output;
	}
}
