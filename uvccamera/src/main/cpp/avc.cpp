//
// Created by sjh on 2021/12/11.
//
#include <fcntl.h>
#include <cstdio>
#include <cstdlib>
#include "avc.h"

AvcEncoder::AvcEncoder(AvcArgs args) {
    fpsTime = 1000 / args.frame_rate;
}

AvcEncoder::~AvcEncoder() {

}

bool AvcEncoder::prepare() {
    AMediaFormat *videoFormat = AMediaFormat_new();
    AMediaFormat_setString(videoFormat, AMEDIAFORMAT_KEY_MIME, VIDEO_MINE);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_WIDTH, arguments->width);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_HEIGHT, arguments->height);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_BIT_RATE, arguments->bitRate);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_FRAME_RATE, arguments->frameRate);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_I_FRAME_INTERVAL, I_FRAME_INTERVAL);
    AMediaFormat_setInt32(videoFormat, AMEDIAFORMAT_KEY_COLOR_FORMAT, arguments->colorFormat);

}

bool AvcEncoder::start() {

}

bool AvcEncoder::offerData(void *data) {

}

bool AvcEncoder::stop() {
    if (videoCodec != nullptr) {
        AMediaCodec_stop(videoCodec);
        AMediaCodec_delete(videoCodec);
    }
}

bool AvcEncoder::isRecording() {

}

int64_t systemnanotime() {
    timespec now{};
    clock_gettime(CLOCK_MONOTONIC, &now);
    return now.tv_sec * 1000000000LL + now.tv_nsec;
}

