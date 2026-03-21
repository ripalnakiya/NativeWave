package com.ripalnakiya.nativewave

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView

class WaveformSurfaceView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {

    private var drawingThread: Thread? = null
    @Volatile
    private var isDrawing = false
    @Volatile
    private var isRecording = false

    @Volatile
    private var smoothedData = FloatArray(0)

    private lateinit var waveformBitmap: Bitmap
    private lateinit var waveformCanvas: Canvas

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isDrawing = true
//        drawingThread = Thread { drawLoop() }
        drawingThread = Thread { drawLoopWithMovingAnimation() }
        drawingThread?.start()
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isDrawing = false
        drawingThread?.join()
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
        waveformBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        waveformCanvas = Canvas(waveformBitmap)
    }

    fun startVisualization() {
        isRecording = true
    }

    fun stopVisualization() {
        isRecording = false
    }

    // receive FFT data
    fun updateSpectrum(spectrum: List<Float>) {
        if (!isRecording) return

        val newData = spectrum.map {
            it.coerceIn(-80f, 0f) // clamp dB range properly
        }.toFloatArray()

        if (smoothedData.isEmpty()) {
            smoothedData = newData
            return
        }

        val alpha = 0.2f // smaller = smoother

        for (i in newData.indices) {
            smoothedData[i] = smoothedData[i] * (1 - alpha) + newData[i] * alpha
        }
    }

    private val paint = Paint().apply {
        color = Color.rgb(3, 169, 244)
        isAntiAlias = true
    }

    /**
     * Displays the FFT data on the screen directly
     */
    private fun drawLoop() {
        while (isDrawing) {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: continue

                canvas.drawColor(Color.WHITE)

                val data = smoothedData
                if (data.isEmpty()) continue

                val bars = minOf(64, data.size)
                val step = data.size / bars
                val barWidth = width.toFloat() / bars
                val maxHeight = height.toFloat()

                for (i in 0 until bars) {

                    var sum = 0f
                    for (j in 0 until step) {
                        sum += data[i * step + j]
                    }

                    val avg = sum / step
//                    val normalized = ((avg + 60) / 60).coerceIn(0f, 1f)
                    val minDb = -60f
                    val maxDb = 0f
                    val normalized = ((avg - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)

                    val barHeight = normalized * maxHeight

                    val x = i * barWidth

                    canvas.drawRect(
                        x,
                        height - barHeight,
                        x + barWidth * 0.8f,
                        height.toFloat(),
                        paint
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            Thread.sleep(16) // ~60 FPS
        }
    }

    /**
     * Displays FFT data as a continuous stream (Moving from right to left)
     */
    private fun drawLoopWithMovingAnimation() {
        val shift = 6 // pixels per frame (speed of movement)

        while (isDrawing) {
            var canvas: Canvas? = null

            try {
                canvas = holder.lockCanvas() ?: continue

                // If not recording, decay everything to zero
                if (!isRecording) {
                    // Gradually decay everything to zero
                    val minDb = -60f
                    for (i in smoothedData.indices) {
                        smoothedData[i] += (minDb - smoothedData[i]) * 0.1f
                    }
                }

                val data = smoothedData
                if (data.isEmpty()) continue

                val width = waveformBitmap.width
                val height = waveformBitmap.height

                // Shift existing bitmap LEFT
                val src = Rect(shift, 0, width, height)
                val dst = Rect(0, 0, width - shift, height)
                waveformCanvas.drawBitmap(waveformBitmap, src, dst, null)

                // Clear right edge
                val clearPaint = Paint().apply {
                    color = Color.WHITE
                    style = Paint.Style.FILL
                }

                waveformCanvas.drawRect(
                    (width - shift).toFloat(),
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    clearPaint
                )

                // Draw new FFT slice on RIGHT
                val bars = minOf(64, data.size)
                val step = data.size / bars
                val maxHeight = height.toFloat()

                val barWidth = shift.toFloat() / bars

                for (i in 0 until bars) {

                    var sum = 0f
                    for (j in 0 until step) {
                        sum += data[i * step + j]
                    }

                    val avg = sum / step
//                    val normalized = ((avg + 60) / 60).coerceIn(0f, 1f)
                    val minDb = -60f
                    val maxDb = 0f
                    val normalized = ((avg - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
                    val barHeight = normalized * maxHeight

                    val x = width - shift + i * barWidth

                    waveformCanvas.drawLine(x, height.toFloat(), x, height - barHeight, paint)
                }

                // Draw final bitmap to screen
                canvas.drawBitmap(waveformBitmap, 0f, 0f, null)

            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            Thread.sleep(16) // ~60 FPS
        }
    }
}
