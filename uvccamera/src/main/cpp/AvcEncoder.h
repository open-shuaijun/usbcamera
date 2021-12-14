//
// Created by sjh on 2021/12/11.
//

#ifndef USBCAMERA_AVCENCODER_H
#define USBCAMERA_AVCENCODER_H


#define SAMPLE_RATE (48000)//音频采样率
#define AUDIO_BIT_RATE (128000)//音频编码的比特率
#define CHANNEL_COUNT (2)//音频编码通道数
#define CHANNEL_CONFIG (12)//音频录制通道,默认为立体声
#define AAC_PROFILE (2)
#define AUDIO_FORMAT (2)//音频录制格式，默认为PCM16Bit
#define AUDIO_SOURCE (1)


#define I_FRAME_INTERVAL (1)

#include <media/NdkMediaMuxer.h>
#include <media/NdkMediaCodec.h>
#include <media/NdkMediaFormat.h>
#include <pthread.h>
#include <jni.h>
#include "AvcArgs.h"
#include "AvcQueue.h."

class AvcEncoder {
private:
    AvcEncoder() {
        pthread_mutex_init(&media_mutex, nullptr);
    }

    AMediaMuxer *mMuxer;  //多路复用器，用于音视频混合
    AMediaCodec *videoCodec;   //编码器，用于视频编码
    AvcQueue<void *> frame_queue;

    const char *VIDEO_MIME = "video/avc";

    int mVideoTrack = -1;

    pthread_t mVideoThread;
    pthread_mutex_t media_mutex;

    int64_t fpsTime;
    uint sleepTime = 20 * 1000;

    bool mIsRecording = false, mStartFlag = false;

    int64_t nanoTime;

    static unsigned char *yuv420_buf;

public:


    ~AvcEncoder() {

    }

    AvcEncoder(const AvcEncoder &) = delete;

    AvcEncoder &operator=(const AvcEncoder &) = delete;

    static AvcEncoder &getInstance() {
        static AvcEncoder instance;
        return instance;
    }

    bool prepare(AvcArgs arguments);

    bool start();

    bool isRecording();

    void stop();

    void feedData(void *data);

    static int yuyv_to_yuv420p(const unsigned char *in, unsigned char *out, unsigned int width,
                               unsigned int height);

    static void *videoStep(void *obj);

};


#endif //USBCAMERA_AVCENCODER_H


