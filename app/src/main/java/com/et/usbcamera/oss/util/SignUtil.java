package com.et.usbcamera.oss.util;

import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public final class SignUtil {

    public static String getSign(Map<String, Object> map, boolean base64) {
        try {
            List<String> keys = new ArrayList<>(map.keySet());
            Collections.sort(keys);
            StringBuilder sb = new StringBuilder(512);
            Object obj;
            for (String k : keys) {
                if (TextUtils.isEmpty(k)) continue;
                obj = map.get(k);
                if (obj == null) continue;
                sb.append(k).append('=').append(obj.toString()).append('&');
            }
            map.remove("key");
            sb.delete(sb.length() - 1, sb.length());
            String content = sb.toString();
//            Log.e("et_log", "content:" + content);
            if (base64) {
                content = new String(Base64.encode(content.getBytes("UTF-8"), Base64.NO_WRAP), "UTF-8");
//                Log.e("et_log", "base64:" + content);
            }
            String signMd5 = Md5Util.md5(content);
//            Log.e("et_log", "md5:" + signMd5);
            signMd5 = signMd5.substring(1, 2) + signMd5.substring(3, 4)
                    + signMd5.substring(5, 6) + signMd5.substring(7)
                    + signMd5.substring(0, 1) + signMd5.substring(2, 3)
                    + signMd5.substring(4, 5) + signMd5.substring(6, 7);
//            Log.e("et_log", "换位:" + signMd5);
            return Md5Util.md5(signMd5).toLowerCase();
        } catch (Exception e) {
            Log.d("et_log", "E-秘钥获取失败：", e);
            return "";
        }
    }
}
