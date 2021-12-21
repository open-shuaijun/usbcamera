package com.et.usbcamera;

import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.os.SystemClock;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;


public class AvcEncoder {
    private final static String TAG = "MeidaCodec";

    private final int TIMEOUT_USEC = 12000;

    private MediaCodec mediaCodec;
    int m_width;
    int m_height;
    int m_framerate;
    byte[] m_info = null;

    public byte[] configbyte;
    private MediaMuxer muxer;


    public AvcEncoder(int width, int height, int framerate, int bitrate, File pathFile) {

        m_width = width;
        m_height = height;
        m_framerate = framerate;

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, width * height * 5);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 30);
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1);
        try {
            mediaCodec = MediaCodec.createEncoderByType("video/avc");
        } catch (IOException e) {
            e.printStackTrace();
        }

        mediaCodec.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        mediaCodec.start();
        checkFile(pathFile);
    }

//    private BufferedOutputStream outputStream;
//    FileOutputStream outStream;

    private void checkFile(File file) {
        if (file.exists()) {
            boolean b = file.delete();
            Log.d(TAG, "delete old file " + b);
        }
        try {
            muxer = new MediaMuxer(file.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            muxer.setOrientationHint(90);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void StopEncoder() {
        try {
            mediaCodec.stop();
            mediaCodec.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    ByteBuffer[] inputBuffers;
    ByteBuffer[] outputBuffers;

    public boolean isRuning = false;

    public void StopThread() {
        isRuning = false;
        muxer.stop();
        muxer.release();
        StopEncoder();


    }

    int count = 0;

    public void StartEncoderThread() {
        Thread EncoderThread = new Thread(() -> {
            isRuning = true;
            byte[] input;
            long generateIndex = 0;
            int track = 0;

            while (isRuning) {
                if (MainActivity.Companion.getYUVQueue().size() > 0) {
                    int deqInput = mediaCodec.dequeueInputBuffer(-1);
                    input = MainActivity.Companion.getYUVQueue().poll();
                    byte[] yuv420p = new byte[m_width * m_height * 3 / 2];
                    yuyvToYuv420P(input, yuv420p, m_width, m_height);
                    input = yuv420p;
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(deqInput);
                    inputBuffer.put(input);
                    generateIndex++;
                    mediaCodec.queueInputBuffer(deqInput, 0, input.length, System.nanoTime() / 1000, 0);

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int deOutput = mediaCodec.dequeueOutputBuffer(bufferInfo, 10000);
                    if (deOutput >= 0) {
                        ByteBuffer outputBuffer = mediaCodec.getOutputBuffer(deOutput);
                        if (bufferInfo.flags == MediaCodec.BUFFER_FLAG_CODEC_CONFIG ) {
                            MediaFormat outFormat = mediaCodec.getOutputFormat();
                            track = muxer.addTrack(outFormat);
                            muxer.start();
                        } else {
                            muxer.writeSampleData(track, outputBuffer, bufferInfo);
                        }
                        mediaCodec.releaseOutputBuffer(deOutput, false);
                    }
                } else {
                    SystemClock.sleep(30);
                }
            }
        });
        EncoderThread.start();
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / m_framerate;
    }

//    private void NV21ToNV12(byte[] nv21, byte[] nv12, int width, int height) {
//        if (nv21 == null || nv12 == null) {
//            return;
//        }
//        int framesize = width * height;
//        int i = 0, j = 0;
//        System.arraycopy(nv21, 0, nv12, 0, framesize);
//        for (i = 0; i < framesize; i++) {
//            nv12[i] = nv21[i];
//        }
//        for (j = 0; j < framesize / 2; j += 2) {
//            nv12[framesize + j - 1] = nv21[j + framesize];
//        }
//        for (j = 0; j < framesize / 2; j += 2) {
//            nv12[framesize + j] = nv21[j + framesize - 1];
//        }
//    }


    void yuyvToYuv420P(byte[] yuyv, byte[] yuv420p, int width, int height) {
        int frame_size = width * height;
        for (int i = 0, g = 0; i < frame_size * 2; i += 2, g++) {
            yuv420p[g] = yuyv[i];
        }

        int base_h;
        boolean isU = true;
        int indexU = frame_size;
        int indexV = frame_size + frame_size / 4;
        for (int i = 0; i < height; i += 2) {
            base_h = i * width * 2;
            for (int j = base_h + 1; j < base_h + width * 2L; j += 2) {
                if (isU) {
                    yuv420p[indexU++] = yuyv[j];
                } else {
                    yuv420p[indexV++] = yuyv[j];
                }
                isU = !isU;
            }
        }
    }
}