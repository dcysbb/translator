package com.poozh.translator.capture

import android.content.Context
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.graphics.Rect
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.WindowManager
import kotlin.math.max
import kotlin.math.min

class ScreenCaptureController(
    private val context: Context,
    private val mediaProjection: MediaProjection,
    private val selectionProvider: () -> Rect?,
    private val frameCallback: (Bitmap) -> Unit,
    private val errorCallback: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private var captureThread: HandlerThread? = null
    private var captureHandler: Handler? = null
    private var imageReader: ImageReader? = null
    private var virtualDisplay: VirtualDisplay? = null
    private var pendingCapture = false

    private val projectionCallback = object : MediaProjection.Callback() {
        override fun onStop() {
            mainHandler.post {
                pendingCapture = false
                releaseDisplay()
                errorCallback("屏幕捕获已停止")
            }
        }
    }

    fun prepare() {
        ensureDisplay()
    }

    fun captureOnce() {
        if (pendingCapture) {
            mainHandler.post { errorCallback("正在截取屏幕，请稍候") }
            return
        }
        ensureDisplay()
        drainPendingImages()
        pendingCapture = true
        captureHandler?.postDelayed({
            val delivered = captureLatestFrame()
            pendingCapture = false
            if (!delivered) {
                mainHandler.post { errorCallback("暂时没有可用画面，请再点一次刷新") }
            }
        }, INITIAL_FRAME_DELAY_MS)
    }

    fun release() {
        pendingCapture = false
        releaseDisplay()
        runCatching { mediaProjection.unregisterCallback(projectionCallback) }
        runCatching { mediaProjection.stop() }
    }

    private fun ensureDisplay() {
        if (virtualDisplay != null) return

        val metrics = context.resources.displayMetrics
        val width = max(metrics.widthPixels, 1)
        val height = max(metrics.heightPixels, 1)
        val density = metrics.densityDpi

        val thread = HandlerThread("screen-translator-capture").also { it.start() }
        captureThread = thread
        captureHandler = Handler(thread.looper)
        val reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2)
        imageReader = reader

        mediaProjection.registerCallback(projectionCallback, mainHandler)
        // createVirtualDisplay throws SecurityException / IllegalStateException on
        // Android 14+ if the projection isn't active or the FGS type isn't allowed.
        // Wrap it so a failure cleans up half-built resources and reports a clear
        // error instead of crashing.
        virtualDisplay = try {
            mediaProjection.createVirtualDisplay(
                "ScreenTranslator",
                width,
                height,
                density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                reader.surface,
                null,
                captureHandler
            )
        } catch (e: Throwable) {
            runCatching { mediaProjection.unregisterCallback(projectionCallback) }
            releaseDisplay()
            mainHandler.post { errorCallback("屏幕捕获启动失败，请重新授权：${e.localizedMessage ?: e::class.simpleName}") }
            null
        }
    }

    private fun captureLatestFrame(): Boolean {
        val reader = imageReader ?: run {
            android.util.Log.w(TAG, "captureLatestFrame: imageReader is null")
            return false
        }
        val image = runCatching { reader.acquireLatestImage() }.getOrNull() ?: run {
            android.util.Log.w(TAG, "captureLatestFrame: no frame available yet")
            return false
        }
        image.use { current ->
            val bitmap = imageToBitmap(current)
            val rawSelection = selectionProvider()
            val cropRect = rawSelection?.let { sanitizeRect(it, bitmap.width, bitmap.height) }
                ?: Rect(0, 0, bitmap.width, bitmap.height)
            android.util.Log.d(TAG, "captureLatestFrame: bitmap=${bitmap.width}x${bitmap.height}, selection=$rawSelection, cropRect=$cropRect")
            if (cropRect.width() < 8 || cropRect.height() < 8) {
                android.util.Log.w(TAG, "captureLatestFrame: cropRect too small, dropping")
                bitmap.recycle()
                return false
            }
            val cropped = Bitmap.createBitmap(bitmap, cropRect.left, cropRect.top, cropRect.width(), cropRect.height())
            bitmap.recycle()
            android.util.Log.d(TAG, "captureLatestFrame: cropped=${cropped.width}x${cropped.height}, delivering")
            mainHandler.post { frameCallback(cropped) }
            return true
        }
    }

    private fun drainPendingImages() {
        val reader = imageReader ?: return
        while (true) {
            val staleImage = runCatching { reader.acquireLatestImage() }.getOrNull() ?: return
            staleImage.close()
        }
    }

    private fun imageToBitmap(image: Image): Bitmap {
        val plane = image.planes[0]
        val buffer = plane.buffer
        buffer.rewind()
        val pixelStride = plane.pixelStride
        val rowStride = plane.rowStride
        val rowPadding = rowStride - pixelStride * image.width
        val paddedWidth = image.width + rowPadding / pixelStride
        val paddedBitmap = Bitmap.createBitmap(paddedWidth, image.height, Bitmap.Config.ARGB_8888)
        paddedBitmap.copyPixelsFromBuffer(buffer)
        val bitmap = Bitmap.createBitmap(paddedBitmap, 0, 0, image.width, image.height)
        paddedBitmap.recycle()
        return bitmap
    }

    private fun sanitizeRect(rect: Rect, maxWidth: Int, maxHeight: Int): Rect {
        val left = rect.left.coerceIn(0, maxWidth - 1)
        val top = rect.top.coerceIn(0, maxHeight - 1)
        val right = rect.right.coerceIn(left + 1, maxWidth)
        val bottom = rect.bottom.coerceIn(top + 1, maxHeight)
        return Rect(left, top, right, bottom)
    }

    private fun releaseDisplay() {
        runCatching { virtualDisplay?.release() }
        virtualDisplay = null
        runCatching { imageReader?.close() }
        imageReader = null
        captureHandler?.removeCallbacksAndMessages(null)
        captureThread?.quitSafely()
        captureHandler = null
        captureThread = null
    }

    companion object {
        private const val TAG = "ScreenCaptureCtrl"
        private const val INITIAL_FRAME_DELAY_MS = 240L
        /** How long to wait after flushing the buffer before grabbing the frame
         *  we actually OCR — lets the compositor settle after a transition. */
        private const val SETTLE_DELAY_MS = 150L
    }
}
