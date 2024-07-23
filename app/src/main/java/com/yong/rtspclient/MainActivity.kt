package com.yong.rtspclient

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.alexvas.rtsp.widget.RtspSurfaceView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "RTSP Client"

    private var btnStart: Button? = null
    private var btnStop: Button? = null
    private var rtspInput: EditText? = null
    private var rtspView: RtspSurfaceView? = null

    private var isRtspPlaying = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStart = findViewById(R.id.main_btn_start_view)
        btnStop = findViewById(R.id.main_btn_stop_view)
        rtspInput = findViewById(R.id.main_input_rtsp)
        rtspView = findViewById(R.id.main_surface_rtsp)

        btnStart!!.setOnClickListener(btnListener)
        btnStop!!.setOnClickListener(btnListener)
    }

    private fun startRTSP() {
        if(!isRtspPlaying.get()) {
            Log.i(LOG_TAG, "RTSP Starting")
            isRtspPlaying.set(true)

            val rtspUrl = getRtspUrl()
            CoroutineScope(Dispatchers.IO).launch {
                rtspView!!.init(Uri.parse(rtspUrl), null, null)
                rtspView!!.setStatusListener(RtspClientListener())
                rtspView!!.start(requestVideo = true, requestAudio = false)
            }

            Log.i(LOG_TAG, "RTSP Started from $rtspUrl")
        }

    }

    private fun stopRTSP() {
        if(isRtspPlaying.get()) {
            Log.i(LOG_TAG, "RTSP Stopping")
            isRtspPlaying.set(false)

            rtspView!!.stop()
            Log.i(LOG_TAG, "RTSP Stopped")
        }
    }

    private fun getRtspUrl(): String {
        val inputUrl = rtspInput!!.text.toString()
        return inputUrl
    }

    private val btnListener = View.OnClickListener {
        when(it.id) {
            R.id.main_btn_start_view -> startRTSP()
            R.id.main_btn_stop_view -> stopRTSP()
        }
    }

    inner class RtspClientListener: RtspSurfaceView.RtspStatusListener {
        override fun onRtspFirstFrameRendered() {
            Log.i(LOG_TAG, "RTSP Render Started")
        }

        override fun onRtspStatusConnected() {
            Log.i(LOG_TAG, "RTSP Connected")
        }

        override fun onRtspStatusConnecting() {
            Log.i(LOG_TAG, "RTSP Connecting")
        }

        override fun onRtspStatusDisconnected() {
            Log.i(LOG_TAG, "RTSP Disconnected")
        }

        override fun onRtspStatusFailed(message: String?) {
            Log.e(LOG_TAG, "RTSP Failed: $message")
        }

        override fun onRtspStatusFailedUnauthorized() {
            Log.e(LOG_TAG, "RTSP Unauthorized")
        }
    }
}