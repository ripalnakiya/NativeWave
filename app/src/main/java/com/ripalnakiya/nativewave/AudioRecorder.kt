package com.ripalnakiya.nativewave

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.ripalnakiya.nativewave.ui.WaveformSurfaceView

private const val TAG = "AudioRecorder"

class AudioRecorder(private val surfaceView: WaveformSurfaceView) {
    private var audioRecord: AudioRecord? = null
    private val fftSize = 1024

    companion object {
        @Volatile
        var isRecording = false

        init {
            System.loadLibrary("native-lib")
        }
    }

    external fun performFFT(input: FloatArray): FloatArray

    fun startRecording() {
        val sampleRate = 44100 // Sample rate in Hz
        val channelConfig = AudioFormat.CHANNEL_IN_MONO
        val audioFormat = AudioFormat.ENCODING_PCM_16BIT

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            channelConfig,
            audioFormat,
            fftSize,
        )

        audioRecord?.startRecording()
        isRecording = true

        Thread {
            val buffer = ShortArray(fftSize)
            while (isRecording) {
                val read = audioRecord?.read(buffer, 0, fftSize) ?: 0
                processAudio(buffer, read)
            }
        }.start()
    }

    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

    private fun processAudio(data: ShortArray, size: Int) {
        if (size <= 0) return

        val floatData = FloatArray(size)
        for (i in 0 until size) {
            floatData[i] = data[i] / 32768f
        }

        // Apply Hanning window
        for (i in floatData.indices) {
            val window = 0.5f * (1 - kotlin.math.cos(2 * Math.PI * i / (floatData.size - 1))).toFloat()
            floatData[i] *= window
        }

        val fftResult = performFFT(floatData)

        val half = fftResult.size / 2
        val spectrum = fftResult.copyOfRange(0, half)

        val logSpectrum = spectrum.map {
            (20 * kotlin.math.log10(it + 1e-6)).toFloat()
        }

        surfaceView.updateSpectrum(logSpectrum)
    }
}