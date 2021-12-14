//
// Created by sjh on 2021/12/11.
//
#include <fcntl.h>
#include <cstdio>
#include <cstdlib>
#include <utilbase.h>
#include "AvcEncoder.h"

bool AvcEncoder::prepare(AvcArgs args) {
    fpsTime = 1000 / args.frame_rate;
    path_name = args.path_name;

    videoCodec = AMediaCodec_createEncoderByType(VIDEO_MIME);

    AMediaFormat *videoFormat = AMediaFormat_new();
    AMediaFormat_setString(videoFormat, AMEDIAFORMAT_KEY_MIME, VIDEO_MIME);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_WIDTH, args.width);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_HEIGHT, args.height);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_BIT_RATE, args.bit_rate);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_FRAME_RATE, args.frame_rate);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);

    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_COLOR_FORMAT, args.color_format);

    uint8_t sps[2] = {0x12, 0x12};
    uint8_t pps[2] = {0x12, 0x12};
    AMediaFormat_setBuffer(videoFormat, "csd-0", sps, 2); // sps
    AMediaFormat_setBuffer(videoFormat, "csd-1", pps, 2); // pps
    media_status_t videoConfigureStatus = AMediaCodec_configure(videoCodec, videoFormat, nullptr,
                                                                nullptr,
                                                                AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    if (AMEDIA_OK != videoConfigureStatus) {
        LOGE("set video configure failed status-->%d", videoConfigureStatus);
        return false;
    }
    AMediaFormat_delete(videoFormat);
    LOGI("init videoCodec success");

    FILE *file = fopen(args.path_name, "w+");
    if (!file) {
        LOGE("open media file failed-->%s", args.path_name);
        return false;
    }
    fclose(file);
    return true;
}

bool AvcEncoder::start() {
    nanoTime = system_nano_time();
    pthread_mutex_lock(&media_mutex);
    media_status_t videoStatus = AMediaCodec_start(videoCodec);
    if (AMEDIA_OK != videoStatus) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%s%d", "open videoCodec status-->", videoStatus);
        return false;
    }
    recording = true;
    startFlag = true;
    if (videoThread) {
        pthread_join(videoThread, nullptr);
        videoThread = 0;
    }
    pthread_create(&videoThread, nullptr, AvcEncoder::videoStep, this);
    pthread_mutex_unlock(&media_mutex);
    return true;
}

bool AvcEncoder::offerData(void *data) {
    if (startFlag) {
        frame_queue.push(data);
    } else {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "编码器未就绪");
    }
    return false;
}

bool AvcEncoder::stop() {
    LOGI("stop");
    recording = false;
    startFlag = false;
    pthread_mutex_lock(&media_mutex);
    if (videoCodec != nullptr) {
        AMediaCodec_stop(videoCodec);
        AMediaCodec_delete(videoCodec);
        videoCodec = nullptr;
    }
    videoTrack = -1;
    if (pthread_join(videoThread, nullptr) != EXIT_SUCCESS) {
        LOGE("NativeRecord::terminate video thread: p_thread_join failed");
    }
    pthread_mutex_unlock(&media_mutex);
    pthread_mutex_destroy(&media_mutex);
    LOGI("finish");
    return false;
}

int64_t AvcEncoder::system_nano_time() {
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

void *AvcEncoder::videoStep(void *obj) {
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "开始编码视频");
    auto *record = (AvcEncoder *) obj;

    FILE *file = fopen(record->path_name, "w+");

    if (file) {
        while (record->startFlag) {

            if (record->frame_queue.empty()) continue;
            __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "-");

            ssize_t index = AMediaCodec_dequeueInputBuffer(record->videoCodec, -1);
            size_t out_size;
            if (index >= 0) {
                uint8_t *buffer = AMediaCodec_getInputBuffer(record->videoCodec, index, &out_size);
                void *data = *record->frame_queue.wait_and_pop();
                if (data != nullptr && out_size > 0) {
                    memcpy(buffer, data, out_size);
                    AMediaCodec_queueInputBuffer(record->videoCodec, index, 0, out_size,
                                                 (system_nano_time() - record->nanoTime) / 1000,
                                                 record->recording ? 0
                                                                   : AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
                }
            }
            auto *info = (AMediaCodecBufferInfo *) malloc(sizeof(AMediaCodecBufferInfo));
            ssize_t outIndex;
            do {
                outIndex = AMediaCodec_dequeueOutputBuffer(record->videoCodec, info, 0);
                size_t outSize;
                if (outIndex >= 0) {
                    uint8_t *outputBuffer = AMediaCodec_getOutputBuffer(record->videoCodec,
                                                                        outIndex,
                                                                        &outSize);
                    if (record->videoTrack >= 0 && info->size > 0 && info->presentationTimeUs > 0) {
                        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "%d",outSize);
                        fwrite(outputBuffer,  outSize,1, file);
                        fflush(file);
                    }
                    AMediaCodec_releaseOutputBuffer(record->videoCodec, outIndex, false);
                    if (record->recording) {
                        continue;
                    }
                } else if (outIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
                    AMediaFormat *outFormat = AMediaCodec_getOutputFormat(record->videoCodec);
                    const char *s = AMediaFormat_toString(outFormat);
                    record->videoTrack = 0;
                }
            } while (outIndex >= 0);
        }
        fflush(file);
        fclose(file);
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "视频编码结束了1");

    } else {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "视频文件错误2");

    }
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "编码视频结束3");
    return nullptr;
}
