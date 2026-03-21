package com.ripalnakiya.nativewave

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.core.graphics.createBitmap

private const val TAG = "WaveformSurfaceView"

class WaveformSurfaceView(context: Context, attrs: AttributeSet?) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {

    @Volatile
    var renderMode = RenderMode.STATIC

    private var drawingThread: Thread? = null
    @Volatile
    private var isDrawing = false

    @Volatile
    private var smoothedData = FloatArray(0)

    private lateinit var waveformBitmap: Bitmap
    private lateinit var waveformCanvas: Canvas

    init {
        holder.addCallback(this)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isDrawing = true
        drawingThread = Thread { drawLoop() }
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
        waveformBitmap = createBitmap(width, height)
        waveformCanvas = Canvas(waveformBitmap)
    }

    private val minDb = -60f
    private val maxDb = 0f

    // receive FFT data
    fun updateSpectrum(spectrum: List<Float>) {
        if (!AudioRecorder.isRecording) return

        val newData = spectrum.map {
            it.coerceIn(minDb, maxDb) // clamp dB range properly
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

    private fun drawLoop() {
        while (isDrawing) {
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas() ?: continue
                canvas.drawColor(Color.WHITE)

                Logger.log(TAG, "drawLoop", renderMode.toString())

                // If not recording, Gradually decay everything to zero
                if (!AudioRecorder.isRecording) {
                    val minDb = -60f
                    for (i in smoothedData.indices) {
                        smoothedData[i] += (minDb - smoothedData[i]) * 0.1f
                    }
                }

                when (renderMode) {
                    RenderMode.STATIC -> drawStatic(canvas, smoothedData)
                    RenderMode.MOVING -> drawMoving(canvas, smoothedData)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            Thread.sleep(16) // ~60 FPS
        }
    }

    private val paint = Paint().apply {
        color = Color.rgb(3, 169, 244)
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        color = Color.WHITE
        style = Paint.Style.FILL
    }

    /**
     * Displays the FFT data on the screen directly
     */
    private fun drawStatic(canvas: Canvas, data: FloatArray) {
        if (data.isEmpty()) return

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
            val normalized = ((avg - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
            val barHeight = normalized * maxHeight
            val x = i * barWidth

//            canvas.drawRect(x, height - barHeight, x + barWidth * 0.8f, height.toFloat(), paint)

            // Have a curved top on rectangular bars
            val rect = RectF(x, height - barHeight, x + barWidth * 0.8f, height.toFloat())
            val path = Path()
            val radius = barWidth * 0.4f
            path.addRoundRect(rect, floatArrayOf(
                    radius, radius,   // top-left
                    radius, radius,   // top-right
                    0f, 0f,           // bottom-right
                    0f, 0f            // bottom-left
                ), Path.Direction.CW)

            canvas.drawPath(path, paint)
        }
    }

    private val shift = 6 // pixels per frame (speed of movement)
    /**
     * Displays FFT data as a continuous stream (Moving from right to left)
     */
    private fun drawMoving(canvas: Canvas, data: FloatArray) {
        if (data.isEmpty()) return

        val width = waveformBitmap.width
        val height = waveformBitmap.height

        // Shift existing bitmap LEFT
        val src = Rect(shift, 0, width, height)
        val dst = Rect(0, 0, width - shift, height)
        waveformCanvas.drawBitmap(waveformBitmap, src, dst, null)

        // Clear right edge
        waveformCanvas.drawRect((width - shift).toFloat(), 0f, width.toFloat(), height.toFloat(), clearPaint)

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
            val normalized = ((avg - minDb) / (maxDb - minDb)).coerceIn(0f, 1f)
            val barHeight = normalized * maxHeight
            val x = width - shift + i * barWidth
            waveformCanvas.drawLine(x, height.toFloat(), x, height - barHeight, paint)
        }

        // Draw final bitmap to screen
        canvas.drawBitmap(waveformBitmap, 0f, 0f, null)
    }
}

enum class RenderMode {
    STATIC,
    MOVING
}
