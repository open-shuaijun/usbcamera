package com.et.usbcamera.oss.auth;

import android.util.Log;

import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider;
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken;
import com.et.usbcamera.oss.util.SignUtil;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class OSSAuthCredentialsProvider2 extends OSSFederationCredentialProvider {

    private static final String TAG = "OSSAuthProvider";
    private final String mAuthServerUrl;
    private final String mUuid;

    public OSSAuthCredentialsProvider2(String uuid, String authServerUrl) {
        this.mAuthServerUrl = authServerUrl;
        mUuid = uuid;
    }

    @Override
    public OSSFederationToken getFederationToken() {
        OSSFederationToken authToken = null;
        try {
            Map<String, Object> map = new HashMap<>();
            map.put("avm", mUuid);
            map.put("myappid", "202004---1e38dsdfdfwes28093411d36375ffgujyudfh0euyiy73e544");
            map.put("key", "202004---++sfasaf+*gfh*ddsfwewe345F,,,&&yjfdg4567&&23Itrhyrty4545ht@@!!!$$..ss356h**33");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA);
            map.put("datetime", sdf.format(System.currentTimeMillis()));
            map.put("sign", SignUtil.getSign(map, true));
            map.remove("key");
            OkHttpClient mClient = new OkHttpClient.Builder()
                    .connectTimeout(20, TimeUnit.SECONDS)
                    .writeTimeout(20, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();

            JSONObject j = new JSONObject(map);
            Log.e("et_log", "获取OSS密钥：" + j.toString());
            FormBody.Builder builder = new FormBody.Builder();
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                if (null != entry.getKey() && null != entry.getValue()) {
                    builder.add(entry.getKey(), entry.getValue().toString().trim());
                }
            }
            FormBody formBody = builder.build();
            Request request = new Request.Builder()
                    .url(this.mAuthServerUrl)
                    .post(formBody)
                    .build();
            Response response = mClient.newCall(request).execute();
            if (response.isSuccessful()) {
                ResponseBody body = response.body();
                if (body != null) {
                    String json = body.string();
                    Log.e("et_log", "获取到OSS密钥：" + json);
                    // {"endpoint":"oss-accelerate.aliyuncs.com","flag":0,"expire":"2021-03-09T09:19:31Z","id":"STS.NUBZAfGjZ6vg3Knn9qn3hoyU2","secret":"ErpbwLCC2NFATuLJxd8fQNEZYp7e5PpMYxd4udGGQ1tP","token":"CAISjQJ1q6Ft5B2yfSjIr5b3EfvSqrV7gbSMMW3fijkkYrxEgLz+0Dz2IHFPf3dgCeoXt/gzmGhU7/gSlq9tSoREQkqc4C27NH0Qo22beIPkl5Gf6d5t0e+qewW6Dxr8w7WdAYHQR8/cffGAck3NkjQJr5LxaTSlWS7CU/iOkoU1VskLeQO6YDFafoo0QDFvs8gHL3DcGO+wOxrx+ArqAVFvpxB3hBEOi9u2ydbO7QHF3h+oiL0Yu8HvI4OlaM8rfrUHCo/kh7UrK/WRj34ItkcSrp0b1vIUpW312fiGGERU7hm8NO7Zz8ZiNgcRZNJhSvUf8qisz6wh5rCPz9iulUZXU/9USCXYQpBOAAl/8kw5XxqAAR5NESz0xxUmeoYqBTKsPthI/PRYIEaJP0w4z5BjztYnwDa2Sq0hORAnN1ZJWOwKgruJDVd4nR7CwgY/bX1R137me1G05nb3LzNEpiKmpRvGVPGMlMVI20lpZAovf3dRDH3jjf4Aw6PCsUPPhrxPNnJHD6HWpHSvAR0PWRbXyPVx"}
                    JSONObject jsonObj = new JSONObject(json);
                    int flag = jsonObj.getInt("flag");
                    if (flag == 0) {
                        String ak = jsonObj.getString("id");
                        String sk = jsonObj.getString("secret");
                        String token = jsonObj.getString("token");
                        String expiration = jsonObj.getString("expire");
                        authToken = new OSSFederationToken(ak, sk, token, expiration);
                    }
                }
            } else {
                Log.e(TAG,"服务端未启动:"+response.code());
            }
            return authToken;
        } catch (Exception ignored) {

        }
        return null;
    }
}
