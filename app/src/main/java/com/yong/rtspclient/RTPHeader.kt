package com.yong.rtspclient

class RTPHeader(
    val payloadType: Int,
    var sequenceNumber: Int,
    var timestamp: Long,
    val ssrc: Int
) {
    fun getBytes(): ByteArray {
        val header = ByteArray(12)
        header[0] = 0x80.toByte() // Version 2, No Byte Padding, No CSRC Identifier
        header[1] = (payloadType and 0x7F).toByte() // Payload Type
        header[2] = (sequenceNumber shr 8).toByte() // Sequence Num
        header[3] = (sequenceNumber and 0xFF).toByte()
        header[4] = (timestamp shr 24).toByte() // Timestamp
        header[5] = (timestamp shr 16).toByte()
        header[6] = (timestamp shr 8).toByte()
        header[7] = (timestamp and 0xFF).toByte()
        header[8] = (ssrc shr 24).toByte() // SSRC Identifier
        header[9] = (ssrc shr 16).toByte()
        header[10] = (ssrc shr 8).toByte()
        header[11] = (ssrc and 0xFF).toByte()

        return header
    }
}