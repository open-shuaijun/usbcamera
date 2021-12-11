package com.et.usbcamera

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity
import com.android.uvccamera.USBMonitor
import com.android.uvccamera.UVCCamera
import com.et.usbcamera.databinding.ActivityMainBinding
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val onDeviceConnectListener: USBMonitor.OnDeviceConnectListener =
        object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                usbMonitor.requestPermission(device)
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

        Timer().schedule(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "UVCCamera Count:${usbMonitor.deviceCount}")
                usbMonitor.devices.forEach {
                    Log.e(TAG, "SSSSSSSSSSSSSS")
                }
            }
        }, 3_000)
    }

    companion object {
        const val TAG = "MainActivity"
    }
}