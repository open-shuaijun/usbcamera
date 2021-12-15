package com.et.usbcamera.oss.service;//package com.easytouch.base.oss.service;
//
//import android.app.Service;
//import android.content.Intent;
//import android.os.IBinder;
//import android.util.Log;
//
//public class OSSService extends Service {
//
//    public OSSService() {
//    }
//
//    @Override
//    public void onCreate() {
//        super.onCreate();
//        IOSSBinderImpl.getInstance().initOOS(this);
//    }
//
//    @Override
//    public IBinder onBind(Intent intent) {
//        Log.w("et_log","onBind");
//        return IOSSBinderImpl.getInstance();
//    }
//
//    @Override
//    public boolean onUnbind(Intent intent) {
//        Log.w("et_log","onUnBind");
//        return true;
//    }
//}