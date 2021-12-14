#include <fcntl.h>

#define LOG_TAG "AvcEncoder"

#include <cstdio>
#include <cstdlib>
#include "AvcEncoder.h"


int64_t systemnanotime() {
    timespec now;
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

bool AvcEncoder::prepare(AvcArgs arguments) {
    fpsTime = 1000 / arguments.frame_rate;
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
    media_status_t videoConfigureStatus = AMediaCodec_configure(videoCodec, videoFormat, NULL, NULL,
                                                                AMEDIACODEC_CONFIGURE_FLAG_ENCODE);
    if (AMEDIA_OK != videoConfigureStatus) {
        return false;
    }
    AMediaFormat_delete(videoFormat);

    int fd = open(arguments.path_name, O_CREAT | O_RDWR, 0666);
    if (!fd) {
        return false;
    }
    mMuxer = AMediaMuxer_new(fd, AMEDIAMUXER_OUTPUT_FORMAT_MPEG_4);
    AMediaMuxer_setOrientationHint(mMuxer, 180);
    return true;
}

void AvcEncoder::feedData(void *data) {
    frame_queue.push(data);
}

bool AvcEncoder::start() {
    nanoTime = systemnanotime();

    pthread_mutex_lock(&media_mutex);

    media_status_t videoStatus = AMediaCodec_start(videoCodec);
    if (AMEDIA_OK != videoStatus) {
        return false;
    }

    mIsRecording = true;
    mStartFlag = true;

    if (mVideoThread) {
        pthread_join(mVideoThread, NULL);
        mVideoThread = NULL;
    }
    pthread_create(&mVideoThread, NULL, AvcEncoder::videoStep, this);
    pthread_mutex_unlock(&media_mutex);
    return true;
}

bool AvcEncoder::isRecording() {
    return mIsRecording;
}

void AvcEncoder::stop() {
    pthread_mutex_lock(&media_mutex);
    mIsRecording = false;
    mStartFlag = false;

    if (videoCodec != NULL) {
        AMediaCodec_stop(videoCodec);
        AMediaCodec_delete(videoCodec);
        videoCodec = NULL;
    }

    mVideoTrack = -1;

    if (mMuxer != NULL) {
        AMediaMuxer_stop(mMuxer);
        AMediaMuxer_delete(mMuxer);
        mMuxer = NULL;
    }

    if (pthread_join(mVideoThread, NULL) != EXIT_SUCCESS) {
    }

    pthread_mutex_unlock(&media_mutex);
    pthread_mutex_destroy(&media_mutex);
}

void *AvcEncoder::videoStep(void *obj) {
    AvcEncoder *record = (AvcEncoder *) obj;
    while (record->mStartFlag) {
        if (record->frame_queue.empty()) continue;
        ssize_t index = AMediaCodec_dequeueInputBuffer(record->videoCodec, -1);
        size_t out_size;
        if (index >= 0) {
            uint8_t *buffer = AMediaCodec_getInputBuffer(record->videoCodec, index, &out_size);
            void *data = *record->frame_queue.wait_and_pop().get();
            if (data != NULL && out_size > 0) {
                memcpy(buffer, data, out_size);
                AMediaCodec_queueInputBuffer(record->videoCodec, index, 0, out_size,
                                             (systemnanotime() - record->nanoTime) / 1000,
                                             record->mIsRecording ? 0
                                                                  : AMEDIACODEC_BUFFER_FLAG_END_OF_STREAM);
            }
        }
        AMediaCodecBufferInfo *info = (AMediaCodecBufferInfo *) malloc(
                sizeof(AMediaCodecBufferInfo));
        ssize_t outIndex;
        do {
            outIndex = AMediaCodec_dequeueOutputBuffer(record->videoCodec, info, 0);
            size_t out_size;
            if (outIndex >= 0) {
                uint8_t *outputBuffer = AMediaCodec_getOutputBuffer(record->videoCodec, outIndex,
                                                                    &out_size);
                if (record->mVideoTrack >= 0 && info->size > 0 &&
                    info->presentationTimeUs > 0) {
                    AMediaMuxer_writeSampleData(record->mMuxer, record->mVideoTrack,
                                                outputBuffer,
                                                info);
                }
                AMediaCodec_releaseOutputBuffer(record->videoCodec, outIndex, false);
                if (record->mIsRecording) {
                    continue;
                }
            } else if (outIndex == AMEDIACODEC_INFO_OUTPUT_FORMAT_CHANGED) {
                AMediaFormat *outFormat = AMediaCodec_getOutputFormat(record->videoCodec);
                ssize_t track = AMediaMuxer_addTrack(record->mMuxer, outFormat);
                const char *s = AMediaFormat_toString(outFormat);
                record->mVideoTrack = 0;
                if (record->mVideoTrack >= 0) {
                    AMediaMuxer_start(record->mMuxer);
                }
            }
        } while (outIndex >= 0);
    }
    return 0;
}