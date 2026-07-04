package com.example.translator

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.util.DisplayMetrics
import android.view.WindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.nio.ByteBuffer

class ScreenCaptureProcessor(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val onTextChanged: (String) -> Unit
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val ocrEngine = OcrEngine()
    private val scope = CoroutineScope(Dispatchers.IO)
    private var lastProcessTime = 0L
    private var processIntervalMs = 1000L

    // For simplicity, defining a static crop rect here.
    // In full MVP, this should be dynamically provided by OverlayManager's selection box.
    private var cropRect: android.graphics.Rect? = null

    @Volatile private var paused = false

    fun updateCropRect(rect: android.graphics.Rect) {
        cropRect = rect
    }

    /** Update the throttle interval (ms) between OCR passes. */
    fun updateInterval(ms: Long) {
        processIntervalMs = ms.coerceIn(300L, 5000L)
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
        lastProcessTime = 0L
    }

    @SuppressLint("WrongConstant")
    fun start() {
        handlerThread = HandlerThread("ScreenCaptureThread").apply { start() }
        handler = Handler(handlerThread!!.looper)

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getRealMetrics(metrics)

        val width = metrics.widthPixels
        val height = metrics.heightPixels
        val density = metrics.densityDpi

        // ImageFormat.PIXEL_FORMAT_RGBA_8888 is deprecated but widely used for MediaProjection.
        // The value is 1.
        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        
        virtualDisplay = mediaProjection.createVirtualDisplay(
            "ScreenCapture",
            width, height, density,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader?.surface, null, handler
        )

        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            
            val currentTime = System.currentTimeMillis()
            if (paused || currentTime - lastProcessTime < processIntervalMs || cropRect == null) {
                image.close()
                return@setOnImageAvailableListener
            }
            
            lastProcessTime = currentTime
            processImage(image)
        }, handler)
    }

    private fun processImage(image: Image) {
        val rect = cropRect ?: return image.close()
        
        val width = image.width
        val height = image.height
        val planes = image.planes
        val buffer: ByteBuffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * width

        // Calculate bitmap width dynamically accounting for row padding
        val bmpWidth = width + rowPadding / pixelStride
        val bitmap = Bitmap.createBitmap(bmpWidth, height, Bitmap.Config.ARGB_8888)
        bitmap.copyPixelsFromBuffer(buffer)
        image.close()

        // Safely crop the bitmap
        val safeLeft = rect.left.coerceIn(0, bmpWidth - 1)
        val safeTop = rect.top.coerceIn(0, height - 1)
        val safeRight = rect.right.coerceIn(safeLeft + 1, bmpWidth)
        val safeBottom = rect.bottom.coerceIn(safeTop + 1, height)

        val croppedBitmap = Bitmap.createBitmap(
            bitmap,
            safeLeft,
            safeTop,
            safeRight - safeLeft,
            safeBottom - safeTop
        )

        scope.launch {
            val textResult = ocrEngine.recognizeText(croppedBitmap)
            textResult?.let {
                val recognizedText = it.text
                if (recognizedText.isNotBlank()) {
                    onTextChanged(recognizedText)
                }
            }
            bitmap.recycle()
            croppedBitmap.recycle()
        }
    }

    fun stop() {
        virtualDisplay?.release()
        virtualDisplay = null
        imageReader?.close()
        imageReader = null
        handlerThread?.quitSafely()
        handlerThread = null
        handler = null
    }
}
