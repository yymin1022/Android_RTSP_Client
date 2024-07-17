package com.yong.rtspclient

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

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "RTSP Client"

    private var btnStart: Button? = null
    private var btnStop: Button? = null
    private var rtspInput: EditText? = null
    private var rtspView: SurfaceView? = null

    private var isRtspPlaying = false

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
            Log.i(LOG_TAG, "RTSP Started from ${rtspUrl}")
        }

    }

    private fun stopRTSP() {
        if(isRtspPlaying) {
            Log.i(LOG_TAG, "RTSP Stopping")
            isRtspPlaying = false
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
}