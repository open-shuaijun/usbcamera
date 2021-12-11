package com.et.usbcamera

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.android.uvccamera.USBMonitor
import com.android.uvccamera.UVCCamera
import java.util.*

class MainActivity : AppCompatActivity() {
    private val onDeviceConnectListener: USBMonitor.OnDeviceConnectListener =
        object : USBMonitor.OnDeviceConnectListener {
            override fun onAttach(device: UsbDevice?) {
                TODO("Not yet implemented")
            }

            override fun onDetach(device: UsbDevice?) {
                TODO("Not yet implemented")
            }

            override fun onConnect(
                device: UsbDevice?,
                ctrlBlock: USBMonitor.UsbControlBlock?,
                createNew: Boolean
            ) {
                TODO("Not yet implemented")
            }

            override fun onDisconnect(device: UsbDevice?, ctrlBlock: USBMonitor.UsbControlBlock?) {
                TODO("Not yet implemented")
            }

            override fun onCancel(device: UsbDevice?) {
                TODO("Not yet implemented")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val usbMonitor = USBMonitor(this, onDeviceConnectListener)
        usbMonitor.register()

        Timer().schedule(object : TimerTask() {
            override fun run() {
                Log.d(TAG, "UVCCamera Count:${usbMonitor.deviceCount}")
                usbMonitor.devices.forEach {
                    Log.e(TAG,"SSSSSSSSSSSSSS")
                }
            }
        }, 3_000)
    }

    companion object {
        const val TAG = "MainActivity"
    }
}