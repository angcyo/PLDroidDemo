package com.angcyo.audiovideorecordingdemo.rencoder;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.media.MediaFormat;
import android.media.MediaRecorder;
import android.util.Log;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;

/**
 * Created by robi on 2016-04-01 10:50.
 */
public class AudioRunnable extends Thread {
    public static final boolean DEBUG = true;
    public static final String TAG = "AudioRunnable";
    public static final int SAMPLES_PER_FRAME = 1024;    // AAC, frameBytes/frame/channel
    public static final int FRAMES_PER_BUFFER = 25;    // AAC, frame/buffer/sec
    protected static final int TIMEOUT_USEC = 10000;    // 10[msec]
    private static final String MIME_TYPE = "audio/mp4a-latm";
    //    private static final String MIME_TYPE = "audio/amr-wb";
//    private static final int SAMPLE_RATE = 44100;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    private static final int SAMPLE_RATE = 16000;    // 44.1[KHz] is only setting guaranteed to be available on all devices.
    //    private static final int BIT_RATE = 16000;
    private static final int BIT_RATE = 64000;
    /*音轨数据源 mic就行吧?*/
    private static final int[] AUDIO_SOURCES = new int[]{
            MediaRecorder.AudioSource.DEFAULT
//            MediaRecorder.AudioSource.MIC,
//            MediaRecorder.AudioSource.CAMCORDER,
//            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
//            MediaRecorder.AudioSource.VOICE_RECOGNITION,
    };
    private final Object lock = new Object();
    private MediaCodec mMediaCodec;                // API >= 16(Android4.1.2)
    private volatile boolean isExit = false;
    private WeakReference<MediaMuxerRunnable> mediaMuxerRunnable;
    private AudioRecord audioRecord;
    private MediaCodec.BufferInfo mBufferInfo;        // API >= 16(Android4.1.2)
    private MediaCodecInfo audioCodecInfo;
    private volatile boolean isStart = false;
    private volatile boolean isMuxerReady = false;
    /**
     * previous presentationTimeUs for writing
     */
    private long prevOutputPTSUs = 0;
    private MediaFormat audioFormat;

    public AudioRunnable(WeakReference<MediaMuxerRunnable> mediaMuxerRunnable) {
        this.mediaMuxerRunnable = mediaMuxerRunnable;
        mBufferInfo = new MediaCodec.BufferInfo();
        prepare();
    }

    private static final MediaCodecInfo selectAudioCodec(final String mimeType) {
        if (DEBUG) if (DEBUG) Log.v(TAG, "selectAudioCodec:");

        MediaCodecInfo result = null;
        // get the list of available codecs
        final int numCodecs = MediaCodecList.getCodecCount();
        LOOP:
        for (int i = 0; i < numCodecs; i++) {
            final MediaCodecInfo codecInfo = MediaCodecList.getCodecInfoAt(i);
            if (!codecInfo.isEncoder()) {    // skipp decoder
                continue;
            }
            final String[] types = codecInfo.getSupportedTypes();
            for (int j = 0; j < types.length; j++) {
                if (DEBUG) if (DEBUG)
                    Log.i(TAG, "supportedType:" + codecInfo.getName() + ",MIME=" + types[j]);
                if (types[j].equalsIgnoreCase(mimeType)) {
                    if (result == null) {
                        result = codecInfo;
                        break LOOP;
                    }
                }
            }
        }
        return result;
    }

    private void prepare() {
        audioCodecInfo = selectAudioCodec(MIME_TYPE);
        if (audioCodecInfo == null) {
            if (DEBUG) Log.e(TAG, "Unable to find an appropriate codec for " + MIME_TYPE);
            return;
        }
        if (DEBUG) if (DEBUG) Log.i(TAG, "selected codec: " + audioCodecInfo.getName());

        audioFormat = MediaFormat.createAudioFormat(MIME_TYPE, SAMPLE_RATE, 1);
//        audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
//        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_MASK, AudioFormat.CHANNEL_IN_MONO);//CHANNEL_IN_STEREO 立体声
        audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, BIT_RATE);
        audioFormat.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1);
        audioFormat.setInteger(MediaFormat.KEY_SAMPLE_RATE, SAMPLE_RATE);
//		audioFormat.setLong(MediaFormat.KEY_MAX_INPUT_SIZE, inputFile.length());
//      audioFormat.setLong(MediaFormat.KEY_DURATION, (long)durationInMs );
        if (DEBUG) if (DEBUG) Log.i(TAG, "format: " + audioFormat);
    }

    private void startMediaCodec() throws IOException {
        if (mMediaCodec != null) {
            return;
        }
        mMediaCodec = MediaCodec.createEncoderByType(MIME_TYPE);
        mMediaCodec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mMediaCodec.start();
        if (DEBUG) Log.i(TAG, "prepare finishing");

        prepareAudioRecord();

        isStart = true;
    }

    private void stopMediaCodec() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
            }
        }
        if (mMediaCodec != null) {
            mMediaCodec.stop();
            mMediaCodec.release();
            mMediaCodec = null;
        }
        isStart = false;
        if (DEBUG) Log.e("angcyo-->", "stop audio 录制...");
    }

    public synchronized void restart() {
        isStart = false;
        isMuxerReady = false;
    }

    private void prepareAudioRecord() {
        if (audioRecord != null) {
            audioRecord.stop();
            audioRecord.release();
            audioRecord = null;
        }
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_AUDIO);
        try {
            final int min_buffer_size = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT);
            int buffer_size = SAMPLES_PER_FRAME * FRAMES_PER_BUFFER;
            if (buffer_size < min_buffer_size)
                buffer_size = ((min_buffer_size / SAMPLES_PER_FRAME) + 1) * SAMPLES_PER_FRAME * 2;

            audioRecord = null;
            for (final int source : AUDIO_SOURCES) {
                try {
                    audioRecord = new AudioRecord(source, SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, buffer_size);
                    if (audioRecord.getState() != AudioRecord.STATE_INITIALIZED)
                        audioRecord = null;
                } catch (Exception e) {
                    audioRecord = null;
                }
                if (audioRecord != null) break;
            }
        } catch (final Exception e) {
            if (DEBUG) Log.e(TAG, "AudioThread#run", e);
        }

        if (audioRecord != null) {
            audioRecord.startRecording();
        }
    }

    public void exit() {
        isExit = true;
    }

    public void setMuxerReady(boolean muxerReady) {
        synchronized (lock) {
            if (DEBUG)
                Log.e("angcyo-->", Thread.currentThread().getId() + " audio -- setMuxerReady..." + muxerReady);
            isMuxerReady = muxerReady;
            lock.notifyAll();
        }
    }

    @Override
    public void run() {
        final ByteBuffer buf = ByteBuffer.allocateDirect(SAMPLES_PER_FRAME);
        int readBytes;
        while (!isExit) {

            /*启动或者重启*/
            if (!isStart) {
                stopMediaCodec();

                if (DEBUG)
                    Log.e("angcyo-->", Thread.currentThread().getId() + " audio -- run..." + isMuxerReady);

                if (!isMuxerReady) {
                    synchronized (lock) {
                        try {
                            if (DEBUG) Log.e("ang-->", "audio -- 等待混合器准备...");
                            lock.wait();
                        } catch (InterruptedException e) {
                        }
                    }
                }

                if (isMuxerReady) {
                    try {
                        if (DEBUG) Log.e("angcyo-->", "audio -- startMediaCodec...");
                        startMediaCodec();
                    } catch (IOException e) {
                        e.printStackTrace();
                        isStart = false;
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e1) {
                        }
                    }
                }
            } else if (audioRecord != null) {
                buf.clear();
                readBytes = audioRecord.read(buf, SAMPLES_PER_FRAME);
                if (readBytes > 0) {
                    // set audio data to encoder
                    buf.position(readBytes);
                    buf.flip();
//                    if(DEBUG) Log.e("ang-->", "解码音频数据:" + readBytes);
                    try {
                        encode(buf, readBytes, getPTSUs());
                    } catch (Exception e) {
                        if (DEBUG) Log.e("angcyo-->", "解码音频(Audio)数据 失败");
                        e.printStackTrace();
                    }
                }
            }

            /**/
        }
        if (DEBUG) Log.e("angcyo-->", "Audio 录制线程 退出...");
    }

    private void encode(final ByteBuffer buffer, final int length, final long presentationTimeUs) {
        if (isExit) return;
        final ByteBuffer[] inputBuffers = mMediaCodec.getInputBuffers();
        final int inputBufferIndex = mMediaCodec.dequeueInputBuffer(TIMEOUT_USEC);
            /*向编码器输入数据*/
        if (inputBufferIndex >= 0) {
            final ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
            inputBuffer.clear();
            if (buffer != null) {
                inputBuffer.put(buffer);
            }
//	            if (DEBUG) if(DEBUG) Log.v(TAG, "encode:queueInputBuffer");
            if (length <= 0) {
                // send EOS
//                    mIsEOS = true;
                if (DEBUG) if (DEBUG) Log.i(TAG, "send BUFFER_FLAG_END_OF_STREAM");
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, 0,
                        presentationTimeUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
            } else {
                mMediaCodec.queueInputBuffer(inputBufferIndex, 0, length,
                        presentationTimeUs, 0);
            }
        } else if (inputBufferIndex == MediaCodec.INFO_TRY_AGAIN_LATER) {
            // wait for MediaCodec encoder is ready to encode
            // nothing to do here because MediaCodec#dequeueInputBuffer(TIMEOUT_USEC)
            // will wait for maximum TIMEOUT_USEC(10msec) on each call
        }

        /*获取解码后的数据*/
        final MediaMuxerRunnable muxer = mediaMuxerRunnable.get();
        if (muxer == null) {
            if (DEBUG) Log.w(TAG, "MediaMuxerRunnable is unexpectedly null");
            return;
        }
        ByteBuffer[] encoderOutputBuffers = mMediaCodec.getOutputBuffers();
        int encoderStatus;

        do {
            encoderStatus = mMediaCodec.dequeueOutputBuffer(mBufferInfo, TIMEOUT_USEC);
            if (encoderStatus == MediaCodec.INFO_TRY_AGAIN_LATER) {
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED) {
                encoderOutputBuffers = mMediaCodec.getOutputBuffers();
            } else if (encoderStatus == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {

                final MediaFormat format = mMediaCodec.getOutputFormat(); // API >= 16
                MediaMuxerRunnable mediaMuxerRunnable = this.mediaMuxerRunnable.get();
                if (mediaMuxerRunnable != null) {
                    if (DEBUG)
                        Log.e("angcyo-->", "添加音轨 INFO_OUTPUT_FORMAT_CHANGED " + format.toString());
                    mediaMuxerRunnable.addTrackIndex(MediaMuxerRunnable.TRACK_AUDIO, format);
                }

            } else if (encoderStatus < 0) {
            } else {
                final ByteBuffer encodedData = encoderOutputBuffers[encoderStatus];
                if ((mBufferInfo.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) {
                    // You shoud set output format to muxer here when you target Android4.3 or less
                    // but MediaCodec#getOutputFormat can not call here(because INFO_OUTPUT_FORMAT_CHANGED don't come yet)
                    // therefor we should expand and prepare output format from buffer data.
                    // This sample is for API>=18(>=Android 4.3), just ignore this flag here
                    if (DEBUG) if (DEBUG) Log.d(TAG, "drain:BUFFER_FLAG_CODEC_CONFIG");
                    mBufferInfo.size = 0;
                }

                if (mBufferInfo.size != 0 && muxer != null && muxer.isMuxerStart()) {
                    mBufferInfo.presentationTimeUs = getPTSUs();
//                    if(DEBUG) Log.e("angcyo-->", "发送音频数据 " + mBufferInfo.size);
                    muxer.addMuxerData(new MediaMuxerRunnable.MuxerData(
                            MediaMuxerRunnable.TRACK_AUDIO, encodedData, mBufferInfo));
                    prevOutputPTSUs = mBufferInfo.presentationTimeUs;
                }
                // return buffer to encoder
                mMediaCodec.releaseOutputBuffer(encoderStatus, false);
            }
        } while (encoderStatus >= 0);
    }

    /**
     * get next encoding presentationTimeUs
     *
     * @return
     */
    private long getPTSUs() {
        long result = System.nanoTime() / 1000L;
        // presentationTimeUs should be monotonic
        // otherwise muxer fail to write
        if (result < prevOutputPTSUs)
            result = (prevOutputPTSUs - result) + result;
        return result;
    }
}
