package com.et.usbcamera.oss.util;

import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.math.BigInteger;
import java.security.MessageDigest;

public class Md5Util {
    public static final String[] CHAR = {"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "a", "b", "c", "d", "e", "f"};

    public static String md5(String string) {
        try {
            MessageDigest digest = MessageDigest.getInstance("md5");
            byte[] result = digest.digest(string.getBytes());
            StringBuilder buffer = new StringBuilder();
            for (byte b : result) {
                buffer.append(CHAR[b >> 4 & 0xf]);
                buffer.append(CHAR[b & 0xf]);
            }
            return buffer.toString();
        } catch (Exception ignored) {
            //
        }
        return "00000000000000000000000000000000";
    }

    public static String md5(File file) {
        try {
            if (!file.exists() || !file.isFile()) return null;
            byte[] bytes = new byte[1024];
            int len;
            MessageDigest digest = MessageDigest.getInstance("MD5");
            FileInputStream in = new FileInputStream(file);
            while ((len = in.read(bytes, 0, 1024)) != -1) {
                digest.update(bytes, 0, len);
            }
            in.close();
            BigInteger bigInt = new BigInteger(1, digest.digest());
            String md5 = bigInt.toString(16);
            if (md5.length() < 32) {
                StringBuilder string = new StringBuilder(32);
                for (int i = 0; i < 32 - md5.length(); i++) {
                    string.append("0");
                }
                md5 = string.append(md5).toString();
            }
            return md5;
        } catch (Exception ignored) {
            Log.d("et_log", "Get MD5 Fault");
        }
        return null;
    }


//    public static String md5(File f) {
//        if (f == null) return null;
//        String md5 = null;
//        try (InputStream fis = new FileInputStream(f);) {
//            byte[] buffer = new byte[4096];
//            MessageDigest digest;
//            try {
//                digest = MessageDigest.getInstance("MD5");
//                int numRead;
//                while ((numRead = fis.read(buffer)) > 0) {
//                    digest.update(buffer, 0, numRead);
//                }
//                byte[] result = digest.digest();
//                StringBuilder builder = new StringBuilder(result.length << 1);
//                for (byte b : result) {
//                    builder.append(CHAR[b >> 4 & 0xf]);
//                    builder.append(CHAR[b & 0xf]);
//                }
//                md5 = convertHashToString(result);
//                Log.w("et_log", "MD5=" + md5);
//                md5 = builder.toString();
//                Log.w("et_log", "MD5=" + md5);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        return md5;
//    }

//    private static String convertHashToString(byte[] hashBytes) {
//        StringBuilder returnVal = new StringBuilder();
//        for (byte hashByte : hashBytes) {
//            returnVal.append(Integer.toString((hashByte & 0xff) + 0x100, 16).substring(1));
//        }
//        return returnVal.toString().toLowerCase();
//    }
}
