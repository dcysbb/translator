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
    private val getOverlayBounds: () -> android.graphics.Rect?,
    private val onTextChanged: (String) -> Unit
) {
    private var virtualDisplay: VirtualDisplay? = null
    private var imageReader: ImageReader? = null
    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val ocrEngine = OcrEngine()
    private val scope = CoroutineScope(Dispatchers.IO)

    // For simplicity, defining a static crop rect here.
    // In full MVP, this should be dynamically provided by OverlayManager's selection box.
    private var cropRect: android.graphics.Rect? = null

    @Volatile private var paused = false
    @Volatile private var pendingCapture = false

    /**
     * Request one capture+OCR pass. Driven entirely by the user tapping
     * refresh — there is no automatic interval/timer. We immediately try to
     * pull a frame on the capture handler, and if none is available yet we
     * poll a few times: a VirtualDisplay in AUTO_MIRROR only pushes new frames
     * when the screen content changes, so when the screen is still the
     * [ImageReader] listener may never fire — a manual pull is required.
     */
    fun triggerCapture() {
        android.util.Log.d("RefreshTrace", "triggerCapture: pendingCapture=true, handler=${handler != null}")
        pendingCapture = true
        handler?.post { pullFrameWithRetry(0) }
    }

    fun updateCropRect(rect: android.graphics.Rect) {
        cropRect = rect
    }

    fun pause() {
        paused = true
    }

    fun resume() {
        paused = false
    }

    /**
     * Actively try to acquire a frame for the pending capture. Retries a few
     * times with a short delay because the freshest frame may not be buffered
     * the instant the user taps.
     */
    private fun pullFrameWithRetry(attempt: Int) {
        if (!pendingCapture || paused) return
        val reader = imageReader ?: run {
            android.util.Log.d("RefreshTrace", "pullFrameWithRetry#$attempt: imageReader null")
            return
        }
        val image = try {
            reader.acquireLatestImage()
        } catch (_: Exception) {
            null
        }
        when {
            image != null -> {
                android.util.Log.d("RefreshTrace", "pullFrameWithRetry#$attempt: got frame, processing")
                pendingCapture = false
                processImage(image)
            }
            attempt < 8 -> {
                // No fresh frame yet (screen still) — retry shortly. The
                // VirtualDisplay keeps mirroring, so a frame will land soon.
                if (attempt == 0) android.util.Log.d("RefreshTrace", "pullFrameWithRetry: no frame yet, retrying")
                handler?.postDelayed({ pullFrameWithRetry(attempt + 1) }, 60L)
            }
            else -> {
                android.util.Log.d("RefreshTrace", "pullFrameWithRetry: gave up after retries (screen still / no frames)")
            }
        }
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
            // Passive path: only honour a pending manual capture request. This
            // also covers the case where the screen changes after the user
            // tapped but before [pullFrameWithRetry] got a frame.
            if (!pendingCapture || paused || cropRect == null) {
                reader.acquireLatestImage()?.close()
                return@setOnImageAvailableListener
            }
            val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
            pendingCapture = false
            processImage(image)
        }, handler)
    }

    private fun processImage(image: Image) {
        val rect = cropRect ?: run {
            android.util.Log.d("RefreshTrace", "processImage: no cropRect, dropping")
            image.close()
            return
        }
        android.util.Log.d("RefreshTrace", "processImage: image ${image.width}x${image.height}, cropRect=$rect")
        
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

        // Mask out the overlay area to prevent the OCR engine from reading its own output
        getOverlayBounds()?.let { overlayRect ->
            android.util.Log.d("ScreenCaptureProcessor", "Masking overlayRect: $overlayRect, bitmap size: ${bitmap.width}x${bitmap.height}")
            val canvas = android.graphics.Canvas(bitmap)
            val paint = android.graphics.Paint().apply {
                color = android.graphics.Color.BLACK
                style = android.graphics.Paint.Style.FILL
            }
            canvas.drawRect(overlayRect, paint)
        }

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
            val recognizedText = textResult?.text ?: ""
            android.util.Log.d("RefreshTrace", "OCR result: length=${recognizedText.length}, blank=${recognizedText.isBlank()}")
            if (recognizedText.isNotBlank()) {
                onTextChanged(recognizedText)
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
