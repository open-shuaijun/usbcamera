//
// Created by sjh on 2021/12/11.
//
#include <fcntl.h>
#include <cstdio>
#include <cstdlib>
#include <utilbase.h>
#include "Avc.h"

AvcEncoder::AvcEncoder(AvcArgs _args) {
    args.width = _args.width;
    args.height = _args.height;
    args.bit_rate = _args.bit_rate;
    args.color_format = _args.color_format;
    args.path_name = _args.path_name;
    args.frame_rate = _args.frame_rate;
    fpsTime = 1000 / _args.frame_rate;
    pthread_mutex_init(&media_mutex, nullptr);
}

AvcEncoder::~AvcEncoder() {

}

bool AvcEncoder::prepare() {
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
    videoCodec = AMediaCodec_createEncoderByType(VIDEO_MIME);
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
        LOGI("open videoCodec status-->%d", videoStatus);
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
    frame_queue.push(data);
    return false;
}

bool AvcEncoder::stop() {
    LOGI("stop");
    pthread_mutex_lock(&media_mutex);
    recording = false;
    startFlag = false;
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

bool AvcEncoder::isRecording() {
    return recording;
}

int64_t AvcEncoder::system_nano_time() {
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

void *AvcEncoder::videoStep(void *obj) {
    LOGI("videoStep");
    auto *record = (AvcEncoder *) obj;
    FILE *file = fopen(record->args.path_name, "w+");

    if (file) {
        while (record->startFlag) {
            if (record->frame_queue.empty()) continue;
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
                        fwrite(outputBuffer, 0, outSize, file);
//                        AMediaMuxer_writeSampleData(record->mMuxer, record->videoTrack,
//                                                    outputBuffer,
//                                                    info);
                    }
                    AMediaCodec_releaseOutputBuffer(record->videoCodec, outIndex, false);
                    if (record->recording) {
                        continue;
                    }
                } else if (outIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
                    AMediaFormat *outFormat = AMediaCodec_getOutputFormat(record->videoCodec);
//                    ssize_t track = AMediaMuxer_addTrack(record->mMuxer, outFormat);
                    const char *s = AMediaFormat_toString(outFormat);
                    record->videoTrack = 0;
//                    LOGI("video out format %s", s);
//                    LOGE("add video track status-->%zd", track);
//                    if (record->videoTrack >= 0) {
//                        AMediaMuxer_start(record->mMuxer);
//                    }
                }
            } while (outIndex >= 0);
//        int64_t lt = systemnanotime() - time;
//        if (record->fpsTime > lt) {
//            usleep(record->sleepTime);
//        }
        }
        fflush(file);
        fclose(file);
    } else {
        LOGE("open media file failed");
    }
    return nullptr;
}

