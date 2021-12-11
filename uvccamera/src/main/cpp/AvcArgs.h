//
// Created by sjh on 2021/12/11.
//

#ifndef USBCAMERA_AVCARGS_H
#define USBCAMERA_AVCARGS_H


struct AvcArgs {
    int width = 640;
    int height = 480;
    int frame_rate = 20;
    int bit_rate = 19200;
    int color_format;
    const char* path_name;
};


#endif //USBCAMERA_AVCARGS_H
