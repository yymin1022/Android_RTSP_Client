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
import com.alexvas.rtsp.RtspClient
import com.alexvas.rtsp.widget.RtspSurfaceView
import com.alexvas.utils.NetUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.net.Socket
import java.util.concurrent.atomic.AtomicBoolean

class MainActivity : AppCompatActivity() {
    private val LOG_TAG = "RTSP Client"

    private var btnStartListener: Button? = null
    private var btnStopListener: Button? = null
    private var btnStartView: Button? = null
    private var btnStopView: Button? = null
    private var rtspInput: EditText? = null
    private var rtspClient: RtspClient? = null
    private var rtspSocket: Socket? = null
    private var rtspView: RtspSurfaceView? = null

    private var isRtspListenerPlaying = AtomicBoolean(false)
    private var isRtspViewPlaying = AtomicBoolean(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        btnStartListener = findViewById(R.id.main_btn_start_listener)
        btnStopListener = findViewById(R.id.main_btn_stop_listener)
        btnStartView = findViewById(R.id.main_btn_start_view)
        btnStopView = findViewById(R.id.main_btn_stop_view)
        rtspInput = findViewById(R.id.main_input_rtsp)
        rtspView = findViewById(R.id.main_surface_rtsp)

        btnStartListener!!.setOnClickListener(btnListener)
        btnStopListener!!.setOnClickListener(btnListener)
        btnStartView!!.setOnClickListener(btnListener)
        btnStopView!!.setOnClickListener(btnListener)
    }

    private fun startRtspListener() {
        if(!isRtspListenerPlaying.get() && !isRtspViewPlaying.get()) {
            Log.i(LOG_TAG, "RTSP Listener Starting")
            isRtspListenerPlaying.set(true)

            val rtspUrl = getRtspUrl()
            CoroutineScope(Dispatchers.IO).launch {
                val isRtspRunning = AtomicBoolean(false)
                val rtspUri = Uri.parse(rtspUrl)
                rtspSocket = NetUtils.createSocketAndConnect(rtspUri.host!!, rtspUri.port, 10000)
                rtspClient = RtspClient.Builder(rtspSocket!!, rtspUri.toString(), isRtspRunning, RtspClientListener())
                    .requestVideo(true)
                    .build()
                rtspClient!!.execute()
            }

            Log.i(LOG_TAG, "RTSP Listener Started from $rtspUrl")
        }
    }

    private fun stopRtspListener() {
        if(isRtspListenerPlaying.get()) {
            Log.i(LOG_TAG, "RTSP Listener Stopping")
            isRtspListenerPlaying.set(false)

            NetUtils.closeSocket(rtspSocket)
            Log.i(LOG_TAG, "RTSP Listener Stopped")
        }
    }

    private fun startRtspView() {
        if(!isRtspListenerPlaying.get() && !isRtspViewPlaying.get()) {
            Log.i(LOG_TAG, "RTSP View Starting")
            isRtspViewPlaying.set(true)

            val rtspUrl = getRtspUrl()
            CoroutineScope(Dispatchers.IO).launch {
                rtspView!!.init(Uri.parse(rtspUrl), null, null)
                rtspView!!.setStatusListener(RtspStatusListener())
                rtspView!!.start(requestVideo = true, requestAudio = false)
            }

            Log.i(LOG_TAG, "RTSP View Started from $rtspUrl")
        }
    }

    private fun stopRtspView() {
        if(isRtspViewPlaying.get()) {
            Log.i(LOG_TAG, "RTSP View Stopping")
            isRtspViewPlaying.set(false)

            rtspView!!.stop()
            Log.i(LOG_TAG, "RTSP View Stopped")
        }
    }

    private fun getRtspUrl(): String {
        val inputUrl = rtspInput!!.text.toString()
        return inputUrl
    }

    private val btnListener = View.OnClickListener {
        when(it.id) {
            R.id.main_btn_start_listener -> startRtspListener()
            R.id.main_btn_stop_listener -> stopRtspListener()
            R.id.main_btn_start_view -> startRtspView()
            R.id.main_btn_stop_view -> stopRtspView()
        }
    }

    inner class RtspClientListener: RtspClient.RtspClientListener {
        override fun onRtspConnecting() {
            Log.i(LOG_TAG, "RTSP Connecting")
        }

        override fun onRtspConnected(sdpInfo: RtspClient.SdpInfo) {
            Log.i(LOG_TAG, "RTSP Connected")
        }

        override fun onRtspVideoNalUnitReceived(data: ByteArray, offset: Int, length: Int, timestamp: Long) {
            Log.i(LOG_TAG, "RTSP Video Nal Received")
        }

        override fun onRtspAudioSampleReceived(data: ByteArray, offset: Int, length: Int, timestamp: Long) {
            Log.i(LOG_TAG, "RTSP Audio Sample Received")
        }

        override fun onRtspDisconnected() {
            Log.i(LOG_TAG, "RTSP Disconnected")
        }

        override fun onRtspFailedUnauthorized() {
            Log.e(LOG_TAG, "RTSP Unauthorized")
        }

        override fun onRtspFailed(message: String?) {
            Log.e(LOG_TAG, "RTSP Failed: $message")
        }

    }

    inner class RtspStatusListener: RtspSurfaceView.RtspStatusListener {
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