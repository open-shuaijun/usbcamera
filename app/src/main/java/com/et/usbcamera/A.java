package com.et.usbcamera;

import java.util.Arrays;
import android.annotation.SuppressLint;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.util.Log;

public class A {
    private static final String TAG = "MyMediaCodec";


    @SuppressLint("NewApi")
    public void getMediaCodecList(){
        //获取解码器列表
        int numCodecs = MediaCodecList.getCodecCount();
        MediaCodecInfo codecInfo = null;
        for(int i = 0; i < numCodecs && codecInfo == null ; i++){
            MediaCodecInfo info = MediaCodecList.getCodecInfoAt(i);
            if(!info.isEncoder()){
                continue;
            }
            String[] types = info.getSupportedTypes();
            boolean found = false;
            //轮训所要的解码器
            for(int j=0; j<types.length && !found; j++){
                if(types[j].equals("video/avc")){
                    System.out.println("found");
                    found = true;
                }
            }
            if(!found){
                continue;
            }
            codecInfo = info;
        }
        Log.d(TAG, "found"+codecInfo.getName() + "supporting" +" video/avc");



        //检查所支持的colorspace
        int colorFormat = 0;
        MediaCodecInfo.CodecCapabilities capabilities = codecInfo.getCapabilitiesForType("video/avc");
        System.out.println("length-"+capabilities.colorFormats.length + "==" + Arrays.toString(capabilities.colorFormats));
        for(int i = 0; i < capabilities.colorFormats.length && colorFormat == 0 ; i++){
            int format = capabilities.colorFormats[i];
            switch (format) {
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar:
                    System.out.println("-");
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar:
                    System.out.println("-");
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar:
                    System.out.println("-");
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar:
                    System.out.println("-");
                    break;
                case MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar:
                    colorFormat = format;
                    System.out.println("-");
                    break;
                default:
                    Log.d(TAG, "Skipping unsupported color format "+format);
                    break;
            }
        }
        Log.d(TAG, "color format "+colorFormat);
    }
}
