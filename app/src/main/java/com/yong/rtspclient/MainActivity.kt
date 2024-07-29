package com.yong.rtspclient

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.media.MediaFormat
import android.media.MediaMuxer
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
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
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

    private var rtspServerAddress = "192.168.36.112"
    private var rtspServerPort = 8554
    private val rtspUploadChunkSize = 55500 - 12
    private var rtspUploadSequenceNum = 0
    private var rtspUploadTimestamp = 0L

    private var mediaCodec: MediaCodec? = null
    private var mediaMuxer: MediaMuxer? = null
    private val videoBufferInfo = MediaCodec.BufferInfo()
    private var videoOutputPath: String = ""
    private var videoTrackIndex = -1

    private var isMuxerStarted = false
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

            initMediaCodec()
            initMediaMuxer()

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
            releaseMedia()
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

    private fun initMediaCodec() {
        Log.i(LOG_TAG, "Initializing Media Codec")
        val mediaFormat = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, 480, 480)
        mediaFormat.setInteger(MediaFormat.KEY_BIT_RATE, 125000)
        mediaFormat.setInteger(MediaFormat.KEY_FRAME_RATE, 15)
        mediaFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible)
        mediaFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 5)

        val codecList = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codecInfos = codecList.codecInfos
        var encoderName: String? = null
        for(codecInfo in codecInfos) {
            if(codecInfo.isEncoder) {
                val supportedTypes = codecInfo.supportedTypes
                for(type in supportedTypes) {
                    if(type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, ignoreCase = true)) {
                        encoderName = codecInfo.name
                        break
                    }
                }
                if(encoderName != null) break
            }
        }

        if(encoderName == null) {
            throw RuntimeException("No suitable encoder found")
        }

        mediaCodec = MediaCodec.createByCodecName(encoderName)
        mediaCodec!!.configure(mediaFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        mediaCodec!!.start()
        Log.i(LOG_TAG, "Initialized Media Codec")
    }

    private fun initMediaMuxer() {
        Log.i(LOG_TAG, "Initializing Media Muxer")
        try {
            videoOutputPath = "${applicationContext.filesDir.absolutePath}/output.mp4"
            mediaMuxer = MediaMuxer(videoOutputPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        } catch(e: IOException) {
            Log.e(LOG_TAG, "Error Initializing Muxer: $e")
        }
        Log.i(LOG_TAG, "Initialized Media Muxer")
    }

    private fun releaseMedia() {
        Log.i(LOG_TAG, "Closing Media Codec/Muxer")

        try {
            if(mediaCodec != null) {
                mediaCodec?.signalEndOfInputStream()
            }
        } catch(e: IllegalStateException) {
            Log.e(LOG_TAG, "Error signaling end of input stream: $e")
        }

        mediaCodec!!.stop()
        mediaCodec!!.release()
        mediaCodec = null

        if(isMuxerStarted) {
            mediaMuxer!!.stop()
            mediaMuxer!!.release()
        }
        mediaMuxer = null
        isMuxerStarted = false
        videoTrackIndex = -1
        Log.i(LOG_TAG, "Closed Media Codec/Muxer")
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

    private fun saveBufferAsFile(data: ByteArray, offset: Int, length: Int, timestamp: Long) {
        val inputBufferIndex = mediaCodec!!.dequeueInputBuffer(10000)
        if(inputBufferIndex >= 0) {
            val inputBuffer = mediaCodec!!.getInputBuffer(inputBufferIndex)
            inputBuffer?.clear()
            inputBuffer?.put(data, offset, length)
            mediaCodec!!.queueInputBuffer(inputBufferIndex, 0, length, timestamp, 0)
        }

        var outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(videoBufferInfo, 10000)
        while(outputBufferIndex >= 0) {
            val outputBuffer = mediaCodec!!.getOutputBuffer(outputBufferIndex)

            if(videoBufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                videoBufferInfo.size = 0
            }

            if(videoBufferInfo.size != 0) {
                if(!isMuxerStarted) {
                    val mediaFormat = mediaCodec!!.outputFormat
                    Log.i(LOG_TAG, "Media Codec Format: $mediaFormat")

                    try {
                        videoTrackIndex = mediaMuxer!!.addTrack(mediaFormat)
                        mediaMuxer!!.start()
                        isMuxerStarted = true
                        Log.i(LOG_TAG, "Media Muxer Started")
                    } catch(e: IllegalArgumentException) {
                        Log.e(LOG_TAG, "Invalid Media Format [$mediaFormat]: $e")
                    } catch(e: IllegalStateException) {
                        Log.e(LOG_TAG, "Media Muxer is invalid: $e")
                    }
                }

                outputBuffer?.position(videoBufferInfo.offset)
                outputBuffer?.limit(videoBufferInfo.offset + videoBufferInfo.size)

                if(outputBuffer != null) {
                    mediaMuxer!!.writeSampleData(videoTrackIndex, outputBuffer, videoBufferInfo)
                }
            }

            mediaCodec!!.releaseOutputBuffer(outputBufferIndex, false)
            outputBufferIndex = mediaCodec!!.dequeueOutputBuffer(videoBufferInfo, 0)
        }
    }

    private fun uploadBufferAsRTSP(data: ByteArray) {
        val rtspChunkCount = data.size / rtspUploadChunkSize + if (data.size % rtspUploadChunkSize > 0) 1 else 0

        DatagramSocket().use { socket ->
            val serverAddress = InetAddress.getByName(rtspServerAddress)

            for(i in 0 until rtspChunkCount) {
                val idxFrom = i * rtspUploadChunkSize
                val idxTo = minOf((i + 1) * rtspUploadChunkSize, data.size)
                val rtpChunk = data.sliceArray(idxFrom until idxTo)
                val rtpHeader = RTPHeader(96, sequenceNumber = rtspUploadSequenceNum, timestamp = rtspUploadTimestamp, 12345678).getBytes()

                val rtpPacket = rtpHeader + rtpChunk
                val sendPacket = DatagramPacket(rtpPacket, rtpPacket.size, serverAddress, rtspServerPort)
                socket.send(sendPacket)

                rtspUploadSequenceNum++
                rtspUploadTimestamp += 3000L
            }
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
            Log.i(LOG_TAG, "RTSP Video Nal Received: ${data.contentToString()}")

            saveBufferAsFile(data, offset, length, timestamp)
            uploadBufferAsRTSP(data)
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