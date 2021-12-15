package com.et.usbcamera

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.android.uvccamera.USBMonitor
import com.android.uvccamera.UVCCamera
import com.et.usbcamera.databinding.ActivityMainBinding
import com.et.usbcamera.oss.OSSFileInfo
import com.et.usbcamera.oss.service.IOSSBinderImpl2
import com.et.usbcamera.oss.util.SignUtil
import com.tencent.bugly.crashreport.CrashReport
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {


    private lateinit var binding: ActivityMainBinding
    private val camera: UVCCamera by lazy { UVCCamera() }
    private var selectCamera = false

    private val onDeviceConnectListener: USBMonitor.OnDeviceConnectListener =
        object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                device?.let {
                    Log.d(
                        TAG,
                        "onAttach pid = ${
                            String.format(
                                "%x",
                                it.productId
                            )
                        } vid = ${String.format("%x", it.vendorId)}"
                    )
                    if ((it.productId == 0x1001 && it.vendorId == 0x1d6c) || (it.productId == 0x636b && it.vendorId == 0xc45)) {
                        Log.d(TAG, "请求相机权限")
                        usbMonitor.requestPermission(it)
                    } else {
                        Log.d(TAG, "不符合相机标号")
                    }
                }
            }

            override fun onDetach(device: UsbDevice?) {
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                if (selectCamera) return
                selectCamera = true
                camera.open(ctrlBlock)
                camera.setPreviewDisplay(binding.surface.holder)
                camera.setPreviewSize(640, 480, UVCCamera.FRAME_FORMAT_YUYV)
                camera.startPreview()
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                selectCamera = false
            }

            override fun onCancel(device: UsbDevice?) {
            }
        }

    private val usbMonitor: USBMonitor by lazy {
        USBMonitor(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(LayoutInflater.from(this))
        setContentView(binding.root)
        CrashReport.initCrashReport(applicationContext, "0a142437fe", true);


        usbMonitor.setOnDeviceConnectListener(onDeviceConnectListener)
        usbMonitor.register()

        IOSSBinderImpl2.getInstance().initOOS("10000000", this.applicationContext)

        binding.btnUpload.setOnClickListener {

            Thread {
                getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { mov ->
                    val f = File(mov, "test-1639547377288.mp4")
                    IOSSBinderImpl2.getInstance().upload(OSSFileInfo().apply {
                        val dir = String.format("vision/10000000/20211215")
                        this.ossPath = String.format("%s/%s", dir, f.name)
                        this.localPath = f
                    }) { url ->
                        try {
                            val map: MutableMap<String?, Any?> = HashMap()
                            map["avm"] = "10000000"
                            map["myappid"] =
                                "202004---1e38dsdfdfwes28093411d36375ffgujyudfh0euyiy73e544"
                            map["key"] =
                                "202004---++sfasaf+*gfh*ddsfwewe345F,,,&&yjfdg4567&&23Itrhyrty4545ht@@!!!$$..ss356h**33"
                            val sdf = SimpleDateFormat("yyyyMMddHHmmss", Locale.CHINA)
                            map["datetime"] = sdf.format(System.currentTimeMillis())
                            map["url"] = url
                            map["out_code"] = "0"
                            map["sign"] = SignUtil.getSign(map, true)
                            map.remove("key")
                            val mClient = OkHttpClient.Builder()
                                .connectTimeout(20, TimeUnit.SECONDS)
                                .writeTimeout(20, TimeUnit.SECONDS)
                                .readTimeout(20, TimeUnit.SECONDS)
                                .build()
                            val j = JSONObject(map)
                            Log.e("et_log", "上传纯净度信息：$j")
                            val builder = FormBody.Builder()
                            for ((key, value) in map) {
                                if (null != key && null != value) {
                                    builder.add(key, value.toString().trim { it <= ' ' })
                                }
                            }
                            val formBody = builder.build()
                            val request: Request = Request.Builder()
                                .url(URL_PURITY)
                                .post(formBody)
                                .build()
                            val response = mClient.newCall(request).execute()
                            if (response.isSuccessful) {
                                val body = response.body()
                                if (body != null) {
                                    val json = body.string()
                                    Log.e("et_log", "纯净度上传完成：$json")
                                }
                            }
                        } catch (ignored: Exception) {
                        }
                        null
                    }
                }
            }.start()

        }

        binding.btnStartRecord.setOnClickListener {
            getExternalFilesDir(Environment.DIRECTORY_MOVIES).apply {
                val f = File(this, "video-${System.currentTimeMillis()}.mp4")
                camera.startRecordingAvc(f.absolutePath)
            }
        }

        binding.btnStopRecord.setOnClickListener {
            camera.stopRecordingAvc()
        }
    }

    override fun onResume() {
        super.onResume()
        A().apply {
            getMediaCodecList()
        }
    }


    companion object {
        const val TAG = "MainActivity"
        const val URL_PURITY =
            "http://test.easytouch-manager.com:7008/Interface/cs/up_box_medias.do"
    }
}