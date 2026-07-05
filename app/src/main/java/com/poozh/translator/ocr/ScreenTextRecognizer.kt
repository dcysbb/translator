package com.poozh.translator.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable
import kotlin.math.max

class ScreenTextRecognizer : Closeable {
    private val japaneseRecognizer: TextRecognizer =
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    private val latinRecognizer: TextRecognizer =
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognize(bitmap: Bitmap, callback: Callback) {
        val workingBitmap = bitmap.scaleForRecognition()
        if (workingBitmap !== bitmap) bitmap.recycle()
        val image = InputImage.fromBitmap(workingBitmap, 0)
        japaneseRecognizer.process(image)
            .addOnSuccessListener { japaneseText ->
                val text = japaneseText.text.trim()
                if (text.isNotBlank()) {
                    workingBitmap.safeRecycle()
                    callback.onSuccess(text)
                } else {
                    recognizeLatin(image, workingBitmap, callback)
                }
            }
            .addOnFailureListener {
                recognizeLatin(image, workingBitmap, callback)
            }
    }

    private fun recognizeLatin(image: InputImage, bitmap: Bitmap, callback: Callback) {
        latinRecognizer.process(image)
            .addOnSuccessListener { text: Text ->
                bitmap.safeRecycle()
                callback.onSuccess(text.text.trim())
            }
            .addOnFailureListener { error ->
                bitmap.safeRecycle()
                callback.onFailure(error.message ?: "OCR 识别失败")
            }
    }

    private fun Bitmap.scaleForRecognition(): Bitmap {
        val largestSide = max(width, height)
        if (largestSide <= MAX_RECOGNITION_SIDE_PX) return this

        val ratio = MAX_RECOGNITION_SIDE_PX.toFloat() / largestSide.toFloat()
        val scaledWidth = (width * ratio).toInt().coerceAtLeast(1)
        val scaledHeight = (height * ratio).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(this, scaledWidth, scaledHeight, true)
    }

    private fun Bitmap.safeRecycle() {
        if (!isRecycled) recycle()
    }

    override fun close() {
        japaneseRecognizer.close()
        latinRecognizer.close()
    }

    interface Callback {
        fun onSuccess(text: String)
        fun onFailure(message: String)
    }

    companion object {
        private const val MAX_RECOGNITION_SIDE_PX = 1600
    }
}
