package com.example.translator

import android.graphics.Bitmap
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class OcrEngine {
    // The Japanese recognizer also recognises Latin characters, so it covers
    // both the Japanese and English use cases from the MVP plan.
    private val recognizer =
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

    suspend fun recognizeText(bitmap: Bitmap): Text? {
        val image = InputImage.fromBitmap(bitmap, 0)
        return try {
            recognizer.process(image).await()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}

/**
 * Bridges a GMS [Task] into a coroutine without depending on
 * `kotlinx-coroutines-play-services`.
 */
suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { e ->
        if (cont.isActive) cont.cancel(e)
    }
}
