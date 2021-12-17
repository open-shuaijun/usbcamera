#include "libuvc.h"
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
    const char *VIDEO_MIME = "video/avc";
    pthread_t videoThread = NULL;
    AMediaMuxer *muxer{};
    AMediaCodec *videoCodec{};
    unsigned char *yuv420_buf{};
    AvcQueue<void *> frame_queue;
    static bool recording;
    int64_t nanoTime{};
    AvcEncoder();
    ~AvcEncoder();

public:

    AvcEncoder(const AvcEncoder &) = delete;

    AvcEncoder &operator=(const AvcEncoder &) = delete;

    static AvcEncoder &getInstance() {
        static AvcEncoder instance;
        return instance;
    }

    bool prepare_start(AvcArgs arguments);

    static void stop() ;

    void feedData(void *data);

    static int yuyvToYuv420P(const unsigned char *in, unsigned char *out, unsigned int width,
                             unsigned int height);

    static void *videoStep(void *obj);

};


#endif //USBCAMERA_AVCENCODER_H


