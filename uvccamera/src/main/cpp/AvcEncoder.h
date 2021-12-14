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


#include "media/NdkMediaCodec.h"
#include <pthread.h>
#include "AvcArgs.h"
#include "AvcQueue.h"


class AvcEncoder {
private:
    const char *VIDEO_MIME = "video/avc";
    AMediaCodec *videoCodec = nullptr;
    pthread_mutex_t media_mutex{};
    pthread_t videoThread{};
    int64_t fpsTime = 50;
    uint sleepTime = 20 * 1000;
    int64_t nanoTime{};
    bool recording = false;
    bool startFlag = false;
    int videoTrack = -1;
    AvcQueue<void *> frame_queue;

    static int64_t system_nano_time();

    AvcEncoder(){
        pthread_mutex_init(&media_mutex, nullptr);
    }

public:
    const char *TAG = "AvcEncoder";

    bool prepare(AvcArgs _args);

    bool start();

    bool offerData(void *data);

    bool stop();

    static void *videoStep(void *obj);

    const char *path_name;

    ~AvcEncoder(){
    }

    AvcEncoder(const AvcEncoder&)=delete;

    AvcEncoder& operator=(const AvcEncoder&)=delete;
    static AvcEncoder& getInstance(){
        static AvcEncoder instance;
        return instance;
    }
};

#endif //USBCAMERA_AVCENCODER_H


