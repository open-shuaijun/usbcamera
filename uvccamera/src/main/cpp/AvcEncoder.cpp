#include <fcntl.h>

#define LOG_TAG "AvcEncoder"

#include <cstdio>
#include <cstdlib>
#include <utilbase.h>
#include "AvcEncoder.h"

int64_t system_nano_time() {
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

bool AvcEncoder::prepare(AvcArgs arguments) {
    pthread_mutex_init(&media_mutex, nullptr);
    fpsTime = 1000 / arguments.frame_rate;
    yuv420_buf = new unsigned char[3 * 640 * 480 / 2 * sizeof(unsigned char)];
    AMediaFormat *videoFormat = AMediaFormat_new();
    AMediaFormat_setString(videoFormat, AMEDIAFORMAT_KEY_MIME, VIDEO_MIME);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_WIDTH, 640);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_HEIGHT, 480);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_BIT_RATE, arguments.bit_rate);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_FRAME_RATE, arguments.frame_rate);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_COLOR_FORMAT, arguments.color_format);
    uint8_t sps[2] = {0x12, 0x12};
    uint8_t pps[2] = {0x12, 0x12};
    AMediaFormat_setBuffer(videoFormat, "csd-0", sps, 2); // sps
    AMediaFormat_setBuffer(videoFormat, "csd-1", pps, 2); // pps
    videoCodec = AMediaCodec_createEncoderByType(VIDEO_MIME);
    media_status_t videoConfigureStatus = AMediaCodec_configure(videoCodec, videoFormat, nullptr, nullptr,
                                                                AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    if (AMEDIA_OK != videoConfigureStatus) {
        return false;
    }
    AMediaFormat_delete(videoFormat);

    int fd = open(arguments.path_name, O_CREAT | O_RDWR, 0666);
    if (!fd) {
        return false;
    }
    muxer = AMediaMuxer_new(fd, AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4);
    AMediaMuxer_setOrientationHint(muxer, 180);
    return true;
}

void AvcEncoder::feedData(void *data) {
    if (startFlag) {
        frame_queue.push(data);
    }
}

bool AvcEncoder::start() {
    nanoTime = system_nano_time();

    pthread_mutex_lock(&media_mutex);

    media_status_t videoStatus = AMediaCodec_start(videoCodec);
    if (AMEDIA_OK != videoStatus) {
        return false;
    }

    mIsRecording = true;
    startFlag = true;

    if (videoThread) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "videoThread ZZZZZZZZZZZZZZZZZZZZZ");
        pthread_join(videoThread, nullptr);
        videoThread = NULL;
    }
    pthread_create(&videoThread, nullptr, videoStep, this);
    pthread_mutex_unlock(&media_mutex);
    return true;
}

bool AvcEncoder::isRecording() const {
    return mIsRecording;
}

void AvcEncoder::releaseMediaCodec() {
    pthread_mutex_lock(&media_mutex);
    if (AvcEncoder::muxer != nullptr) {
        AMediaMuxer_stop(AvcEncoder::muxer);
        AMediaMuxer_delete(AvcEncoder::muxer);
        AvcEncoder::muxer = nullptr;
    }
    pthread_mutex_unlock(&media_mutex);
    pthread_mutex_destroy(&media_mutex);
    if (AvcEncoder::videoCodec != nullptr) {
        AMediaCodec_stop(AvcEncoder::videoCodec);
        AMediaCodec_delete(AvcEncoder::videoCodec);
        AvcEncoder::videoCodec = nullptr;
    }
    if (pthread_join(videoThread, nullptr) != EXIT_SUCCESS) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "pthread_join failed");
    }
    delete (AvcEncoder::yuv420_buf);
    AvcEncoder::yuv420_buf = nullptr;
}

void AvcEncoder::stop() {
    mIsRecording = false;
    startFlag = false;
    mVideoTrack = -1;
    AvcEncoder::getInstance().releaseMediaCodec();
}


int AvcEncoder::yuyv_to_yuv420p(const unsigned char *in, unsigned char *out, unsigned int width,
                                unsigned int height) {
    if (!in || !out) {
        __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "NULL-NULL-AAAAAAAAAAAAAAAAAAA");
        return -1;
    }
    unsigned char *y = out;
    unsigned char *u = out + width * height;
    unsigned char *v = out + width * height + width * height / 4;
    unsigned int i, j;
    unsigned int base_h;
    unsigned int is_y = 1, is_u = 1;
    unsigned int y_index = 0, u_index = 0, v_index = 0;
    unsigned long yuv422_length = 2 * width * height;
    //序列为YU YV YU YV，一个yuv422帧的长度 width * height * 2 个字节
    //丢弃偶数行 u v
    for (i = 0; i < yuv422_length; i += 2) {
        *(y + y_index) = *(in + i);
        y_index++;
    }
    for (i = 0; i < height; i += 2) {
        base_h = i * width * 2;
        for (j = base_h + 1; j < base_h + width * 2; j += 2) {
            if (is_u) {
                *(u + u_index) = *(in + j);
                u_index++;
                is_u = 0;
            } else {
                *(v + v_index) = *(in + j);
                v_index++;
                is_u = 1;
            }
        }
    }
    return 1;
}

void *AvcEncoder::videoStep(void *obj) {
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "videoStep start");

    auto *record = (AvcEncoder *) obj;
    while (record->startFlag) {
        if (record->frame_queue.empty()) continue;
        ssize_t index = AMediaCodec_dequeueInputBuffer(AvcEncoder::getInstance().videoCodec, -1);
        size_t out_size;
        if (index >= 0) {
            uint8_t *buffer = AMediaCodec_getInputBuffer(AvcEncoder::getInstance().videoCodec, index, &out_size);
            void *data = *record->frame_queue.wait_and_pop();
            AvcEncoder::getInstance().yuyv_to_yuv420p((const unsigned char *) data, AvcEncoder::getInstance().yuv420_buf, 640, 480);

            if (AvcEncoder::getInstance().yuv420_buf != nullptr && out_size > 0) {
                memcpy(buffer, AvcEncoder::getInstance().yuv420_buf, out_size);
                AMediaCodec_queueInputBuffer(AvcEncoder::getInstance().videoCodec, index, 0, out_size,
                                             (system_nano_time() - record->nanoTime) / 1000,
                                             record->mIsRecording ? 0
                                                                  : AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
            }
        }
        auto *info = (AMediaCodecBufferInfo *) malloc(
                sizeof(AMediaCodecBufferInfo));
        ssize_t outIndex;
        do {
            outIndex = AMediaCodec_dequeueOutputBuffer(AvcEncoder::getInstance().videoCodec, info, 0);
            size_t outSize;
            if (outIndex >= 0) {
                uint8_t *outputBuffer = AMediaCodec_getOutputBuffer(AvcEncoder::getInstance().videoCodec, outIndex,
                                                                    &outSize);
                if (record->mVideoTrack >= 0 && info->size > 0 &&
                    info->presentationTimeUs > 0) {
                    AMediaMuxer_writeSampleData(AvcEncoder::getInstance().muxer, record->mVideoTrack,
                                                outputBuffer,
                                                info);
                }
                AMediaCodec_releaseOutputBuffer(AvcEncoder::getInstance().videoCodec, outIndex, false);
                if (record->mIsRecording) {
                    continue;
                }
            } else if (outIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
                AMediaFormat *outFormat = AMediaCodec_getOutputFormat(AvcEncoder::getInstance().videoCodec);
                ssize_t track = AMediaMuxer_addTrack(AvcEncoder::getInstance().muxer, outFormat);
                const char *s = AMediaFormat_toString(outFormat);
                record->mVideoTrack = 0;
                if (record->mVideoTrack >= 0) {
                    AMediaMuxer_start(AvcEncoder::getInstance().muxer);
                }
            }
        } while (outIndex >= 0);
    }
    __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG, "videoStep end");

    return nullptr;
}