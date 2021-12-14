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
import java.io.File
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
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
                val camera = UVCCamera()
                camera.open(ctrlBlock)
                camera.setPreviewDisplay(binding.surface.holder)
                camera.startPreview()

                getExternalFilesDir(Environment.DIRECTORY_MOVIES)?.let { mov ->
                    val f = File(mov, "test.avc")
                    camera.startRecordingAvc(f.absolutePath)
                }
                Timer().schedule(object:TimerTask(){
                    override fun run() {
                        camera.stopRecordingAvc()
                        camera.stopPreview()
                        camera.destroy()
                    }
                },3_000)
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
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

        usbMonitor.setOnDeviceConnectListener(onDeviceConnectListener)
        usbMonitor.register()
    }

    companion object {
        const val TAG = "MainActivity"
    }
}