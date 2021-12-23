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
    int width;
    int height;
    int frameRate;
    private MediaMuxer muxer;
    private final byte[] yuv420p;


    public AvcEncoder(int width, int height, int framerate, int bitrate, File pathFile) {

        this.width = width;
        this.height = height;
        frameRate = framerate;
        yuv420p = new byte[this.width * this.height * 3 / 2];

        MediaFormat mediaFormat = MediaFormat.createVideoFormat("video/avc", width, height);
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar);
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 800);
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 20);
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

    private void checkFile(File file) {
        if (file.exists()) {
            boolean b = file.delete();
            Log.d(TAG, "delete old file " + b);
        }
        try {
            muxer = new MediaMuxer(file.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
            muxer.setOrientationHint(0);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isRunning = false;

    public void StopThread() {
        isRunning = false;
    }

    public void StartEncoderThread() {
        Thread EncoderThread = new Thread(() -> {
            isRunning = true;
            byte[] input;
            int track = 0;

            while (isRunning) {
                if (MainActivity.Companion.getYUVQueue().size() > 0) {
                    int deqInput = mediaCodec.dequeueInputBuffer(-1);
                    input = MainActivity.Companion.getYUVQueue().poll();
                    yuyvToYuv420P(input, yuv420p, width, height);
                    ByteBuffer inputBuffer = mediaCodec.getInputBuffer(deqInput);
                    inputBuffer.put(yuv420p);
                    mediaCodec.queueInputBuffer(deqInput, 0, yuv420p.length, System.nanoTime() / 1000, 0);

                    MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
                    int deOutput = mediaCodec.dequeueOutputBuffer(bufferInfo, TIMEOUT_USEC);
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
            muxer.stop();
            muxer.release();
            mediaCodec.stop();
            mediaCodec.release();
        });
        EncoderThread.start();
    }

    /**
     * Generates the presentation time for frame N, in microseconds.
     */
    private long computePresentationTime(long frameIndex) {
        return 132 + frameIndex * 1000000 / frameRate;
    }


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