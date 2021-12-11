package com.android.uvccamera

class NativeLib {

    /**
     * A native method that is implemented by the 'uvccamera' native library,
     * which is packaged with this application.
     */
    external fun stringFromJNI(): String

    companion object {
        // Used to load the 'uvccamera' library on application startup.
        init {
            System.loadLibrary("uvccamera")
        }
    }
}