//
// Created by sjh on 2021/12/11.
//

#ifndef USBCAMERA_AVCARGS_H
#define USBCAMERA_AVCARGS_H


class AvcArgs {
public:
    AvcArgs();
    ~AvcArgs();
    uint16_t width = 640;
    uint16_t height = 480;
    uint8_t frame_rate = 20;
    uint64_t bit_rate = 19200;

};


#endif //USBCAMERA_AVCARGS_H
