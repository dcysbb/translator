package com.example.translator

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class CaptureService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_REFRESH = "ACTION_REFRESH"
        const val EXTRA_RESULT_CODE = "EXTRA_RESULT_CODE"
        const val EXTRA_RESULT_DATA = "EXTRA_RESULT_DATA"
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "CaptureServiceChannel"
    }

    private var mediaProjection: MediaProjection? = null
    private var overlayManager: OverlayManager? = null
    private var captureProcessor: ScreenCaptureProcessor? = null
    private var deepSeekClient: DeepSeekClient? = null

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val ocrCache = OcrTextCache()
    private val translationLock = Mutex()
    private var translationJob: Job? = null

    @Volatile private var paused = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        overlayManager = OverlayManager(this).also { om ->
            om.onClose = { stopSelf() }
            om.onRefresh = { forceRefresh() }
            om.onSelectRegion = { om.showSelectionOverlay() }
            om.onRequestRebuildClient = { rebuildClient() }
            om.onSelectionResult = { rect -> handleSelectionResult(rect) }
        }
        rebuildClient()
    }

    /** User-chosen screen region to OCR. Null until the user draws one. */
    private var selectionRect: android.graphics.Rect? = null

    private fun handleSelectionResult(rect: android.graphics.Rect?) {
        if (rect == null) {
            overlayManager?.updateStatus("已取消选区")
            return
        }
        selectionRect = rect
        captureProcessor?.updateCropRect(rect)
        overlayManager?.updateStatus("选区已更新")
        // Immediately capture the freshly chosen region.
        captureProcessor?.triggerCapture()
    }

    private fun rebuildClient() {
        val prefs = PreferencesManager(this)
        val provider = prefs.currentProvider
        deepSeekClient = DeepSeekClient(
            apiKey = prefs.apiKey,
            baseUrl = provider.baseUrl,
            modelName = prefs.modelName,
            supportsJsonMode = provider.supportsJsonMode
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                val resultCode = intent.getIntExtra(EXTRA_RESULT_CODE, 0)
                val resultData: Intent? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(EXTRA_RESULT_DATA, Intent::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(EXTRA_RESULT_DATA)
                }
                if (resultCode != 0 && resultData != null) {
                    rebuildClient()
                    startForegroundWithNotification()
                    startProjection(resultCode, resultData)
                    overlayManager?.showControlOverlay()
                }
            }
            ACTION_STOP -> stopSelf()
            ACTION_REFRESH -> forceRefresh()
        }
        return START_NOT_STICKY
    }

    private fun startForegroundWithNotification() {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Screen Translator Active")
            .setContentText("Translating selected area on screen")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startProjection(resultCode: Int, resultData: Intent) {
        // Tear down any previous projection first: Android 14+ forbids
        // re-using a MediaProjection token / creating multiple VirtualDisplays
        // on the same instance (throws SecurityException).
        stopProjection()

        val mpManager =
            getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        val projection = try {
            mpManager.getMediaProjection(resultCode, resultData)
        } catch (e: SecurityException) {
            // Token reused or expired — surface a clear error instead of crashing.
            overlayManager?.showError("屏幕捕获授权已失效，请重新开始翻译服务")
            stopSelf()
            return
        }
        if (projection == null) {
            overlayManager?.showError("无法获取屏幕捕获授权，请重试")
            stopSelf()
            return
        }
        mediaProjection = projection
        mediaProjection?.registerCallback(object : MediaProjection.Callback() {
            override fun onStop() {
                stopSelf()
            }
        }, null)

        ocrCache.reset()
        captureProcessor = ScreenCaptureProcessor(
            context = this,
            mediaProjection = mediaProjection!!,
            getOverlayBounds = { overlayManager?.getOverlayBounds() }
        ) { text ->
            scope.launch {
                handleOcrText(text)
            }
        }

        // Apply any user-chosen selection; if none yet, forceRefresh will prompt
        // the user to draw one before capturing.
        selectionRect?.let { captureProcessor?.updateCropRect(it) }
        try {
            captureProcessor?.start()
        } catch (e: SecurityException) {
            overlayManager?.showError("屏幕捕获启动失败，请重新开始翻译服务")
            stopSelf()
        }
    }

    private fun handleOcrText(text: String) {
        requestTranslation(text)
    }

    /**
     * Manually re-trigger a translation. Bypasses the de-dup cache so the user
     * can force a refresh of the current selection. Used by the overlay's
     * refresh button.
     */
    private fun forceRefresh() {
        android.util.Log.d("RefreshTrace", "forceRefresh: captureProcessor=${captureProcessor != null}, hasSelection=${selectionRect != null}")
        // No selection yet → ask the user to draw one first.
        if (selectionRect == null) {
            overlayManager?.showSelectionOverlay()
            return
        }
        captureProcessor?.triggerCapture()
    }

    private var lastOcrText: String = ""

    private fun requestTranslation(text: String) {
        lastOcrText = text
        // Collapse the card and show the spinner while the request is in flight.
        overlayManager?.showLoading()

        translationJob?.cancel()
        translationJob = scope.launch {
            translationLock.withLock {
                val client = deepSeekClient ?: run {
                    overlayManager?.showError("未配置 DeepSeek，请在设置中填写 API Key")
                    return@withLock
                }
                when (val outcome = client.translate(text)) {
                    is TranslationOutcome.Success -> {
                        val formatted = formatResult(outcome.result)
                        overlayManager?.updateText(formatted)
                        overlayManager?.addToHistory(text, formatted)
                    }
                    is TranslationOutcome.Error ->
                        overlayManager?.showError(outcome.message)
                }
            }
        }
    }

    private fun formatResult(r: TranslationResult): String = buildString {
        r.translation?.takeIf { it.isNotBlank() }?.let {
            append("译文：").append(it)
        }
        r.vocabulary?.takeIf { it.isNotEmpty() }?.let { items ->
            if (isNotEmpty()) append("\n\n")
            append("词汇：")
            items.joinTo(this, "；") { v ->
                listOfNotNull(v.word, v.meaning).joinToString(" ")
            }
        }
        r.particles?.takeIf { it.isNotEmpty() }?.let {
            if (isNotEmpty()) append("\n\n")
            append("助词：").append(it.joinToString("；"))
        }
        r.conjugation?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append("\n\n")
            append("活用：").append(it)
        }
        r.fixedExpressions?.takeIf { it.isNotEmpty() }?.let {
            if (isNotEmpty()) append("\n\n")
            append("固定表达：").append(it.joinToString("；"))
        }
        r.pragmaticNotes?.takeIf { it.isNotBlank() }?.let {
            if (isNotEmpty()) append("\n\n")
            append("语用：").append(it)
        }
        r.sentenceAnalysis?.takeIf { it.isNotEmpty() }?.let { sents ->
            if (isNotEmpty()) append("\n\n")
            append("逐句解析：")
            sents.forEachIndexed { i, s ->
                append("\n").append(i + 1).append(". ")
                s.sentence?.let { append(it) }
                s.translation?.takeIf { it.isNotBlank() }?.let { append(" → ").append(it) }
                s.note?.takeIf { it.isNotBlank() }?.let { append("（").append(it).append("）") }
            }
        }
        if (isEmpty()) append("(无可用翻译结果)")
    }

    private fun stopProjection() {
        captureProcessor?.stop()
        captureProcessor = null
        mediaProjection?.stop()
        mediaProjection = null
    }

    override fun onDestroy() {
        super.onDestroy()
        translationJob?.cancel()
        scope.cancel()
        stopProjection()
        overlayManager?.hideAll()
        overlayManager = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Screen Translator Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java)
                ?.createNotificationChannel(channel)
        }
    }
}
