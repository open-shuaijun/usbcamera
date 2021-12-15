package com.et.usbcamera.oss.service;

import android.content.Context;
import android.util.Log;

import com.alibaba.sdk.android.oss.ClientConfiguration;
import com.alibaba.sdk.android.oss.ClientException;
import com.alibaba.sdk.android.oss.OSS;
import com.alibaba.sdk.android.oss.OSSClient;
import com.alibaba.sdk.android.oss.ServiceException;
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback;
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider;
import com.alibaba.sdk.android.oss.model.PutObjectRequest;
import com.alibaba.sdk.android.oss.model.PutObjectResult;
import com.et.usbcamera.oss.OSSFileInfo;
import com.et.usbcamera.oss.auth.OSSAuthCredentialsProvider2;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import kotlin.Unit;
import kotlin.jvm.functions.Function1;

public class IOSSBinderImpl2 {

    String OSS_BUCKET_NAME = "et-vision";
    String OSS_ENDPOINT = "http://oss-accelerate.aliyuncs.com/";
    String STS_INFO_URL = "http://test.easytouch-manager.com:7008/Interface/utils/beforeUp.do";


    private static final Executor THREAD = Executors.newSingleThreadExecutor();
    private OSS ossClient;

    private IOSSBinderImpl2() {
    }


    private static final class INSTANCE {
        private static final IOSSBinderImpl2 ins = new IOSSBinderImpl2();
    }

    public static IOSSBinderImpl2 getInstance() {
        return INSTANCE.ins;
    }

    public void initOOS(String uuid, Context context) {
        OSSCredentialProvider credentialProvider = new OSSAuthCredentialsProvider2(uuid, STS_INFO_URL);
        ClientConfiguration conf = new ClientConfiguration();
        conf.setConnectionTimeout(15 * 1000); // 连接超时时间，默认15秒。
        conf.setSocketTimeout(15 * 1000); // Socket超时时间，默认15秒。
        conf.setMaxConcurrentRequest(6); // 最大并发请求数，默认5个。
        conf.setMaxErrorRetry(3); // 失败后最大重试次数，默认2次。
        ossClient = new OSSClient(context.getApplicationContext(), OSS_ENDPOINT, credentialProvider, conf);
    }

    public synchronized void upload(OSSFileInfo ossFileInfo, Function1<Object,String> callback) {
        PutObjectRequest put = new PutObjectRequest(OSS_BUCKET_NAME, ossFileInfo.ossPath, ossFileInfo.localPath.getAbsolutePath());
        put.setProgressCallback((request, currentSize, totalSize) -> {
        });
        ossClient.asyncPutObject(put, new OSSCompletedCallback<PutObjectRequest, PutObjectResult>() {
            @Override
            public void onSuccess(PutObjectRequest request, PutObjectResult result) {
                Log.w("et_log", "上传成功: " + "https://et-vision.oss-accelerate.aliyuncs.com/" + ossFileInfo.ossPath);
                callback.invoke("https://et-vision.oss-accelerate.aliyuncs.com/" + ossFileInfo.ossPath);
            }

            @Override
            public void onFailure(PutObjectRequest request, ClientException clientExcepion, ServiceException serviceException) {
                if (clientExcepion != null) {
                    clientExcepion.printStackTrace();
                }
                if (serviceException != null) {
                    // Service exception
                    Log.e("ErrorCode", serviceException.getErrorCode());
                    Log.e("RequestId", serviceException.getRequestId());
                    Log.e("HostId", serviceException.getHostId());
                    Log.e("RawMessage", serviceException.getRawMessage());
                }
                Log.w("et_log", "上传失败：" + ossFileInfo.ossPath);
            }
        });
    }
}
