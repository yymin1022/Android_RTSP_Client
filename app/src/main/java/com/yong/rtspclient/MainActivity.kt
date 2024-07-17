package com.yong.rtspclient

import android.graphics.Bitmap
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ir.am3n.rtsp.client.Rtsp
import ir.am3n.rtsp.client.data.Frame
import ir.am3n.rtsp.client.data.SdpInfo
import ir.am3n.rtsp.client.interfaces.RtspFrameListener
import ir.am3n.rtsp.client.interfaces.RtspStatusListener

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "RTSP Client"

    private var btnStart: Button? = null
    private var btnStop: Button? = null
    private var rtspInput: EditText? = null
    private var rtspView: SurfaceView? = null

    private var isRtspPlaying = false
    private var rtspObject: Rtsp? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart = findViewById(R.id.main_btn_start)
        btnStop = findViewById(R.id.main_btn_stop)
        rtspInput = findViewById(R.id.main_input_rtsp)
        rtspView = findViewById(R.id.main_surface_rtsp)

        btnStart!!.setOnClickListener(btnListener)
        btnStop!!.setOnClickListener(btnListener)
    }

    private fun startRTSP() {
        if(!isRtspPlaying) {
            Log.i(LOG_TAG, "RTSP Starting")
            isRtspPlaying = true

            val rtspUrl = getRtspUrl()
            rtspObject = Rtsp()
            rtspObject!!.init(rtspUrl, null, null)
            rtspObject!!.setFrameListener(RtspListenerFrame())
            rtspObject!!.setStatusListener(RtspListenerStatus())
            rtspObject!!.setSurfaceView(rtspView)
            rtspObject!!.start()
            Log.i(LOG_TAG, "RTSP Started from ${rtspUrl}")
        }

    }

    private fun stopRTSP() {
        if(isRtspPlaying) {
            Log.i(LOG_TAG, "RTSP Stopping")
            isRtspPlaying = false

            rtspObject!!.stop()
            rtspObject = null
            Log.i(LOG_TAG, "RTSP Stopped")
        }
    }

    private fun getRtspUrl(): String {
        val inputUrl = rtspInput!!.text.toString()
        return inputUrl
    }

    private val btnListener = View.OnClickListener {
        when(it.id) {
            R.id.main_btn_start -> startRTSP()
            R.id.main_btn_stop -> stopRTSP()
        }
    }

    inner class RtspListenerFrame: RtspFrameListener {
        override fun onAudioSampleReceived(frame: Frame?) {
            Log.i(LOG_TAG, "RTSP Audio Received")
        }

        override fun onVideoFrameReceived(
            width: Int,
            height: Int,
            mediaImage: Image?,
            yuv420Bytes: ByteArray?,
            bitmap: Bitmap?
        ) {
            Log.i(LOG_TAG, "RTSP Video Received")
        }

        override fun onVideoNalUnitReceived(frame: Frame?) {
            Log.i(LOG_TAG, "RTSP Video Nal Received")
        }
    }

    inner class RtspListenerStatus: RtspStatusListener {
        override fun onConnecting() {
            Log.i(LOG_TAG, "RTSP Connecting")
        }
        override fun onConnected(sdpInfo: SdpInfo) {
            Log.i(LOG_TAG, "RTSP Connected")
        }
        override fun onDisconnected() {
            Log.i(LOG_TAG, "RTSP Disconnected")
        }
        override fun onUnauthorized() {
            Log.i(LOG_TAG, "RTSP Unauthorized")
        }
        override fun onFailed(message: String?) {
            Log.e(LOG_TAG, "RTSP Failed: $message")
        }
    }
}