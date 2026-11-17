package com.example.meterphoto

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.google.android.gms.tasks.Task
import java.io.File

/**
 * Распознаёт печатный серийный номер с фото, когда штрихкода нет.
 * Из найденного текста выделяет последовательности длиной 4+ символа,
 * содержащие минимум 3 цифры, — это типичный вид заводского номера.
 */
object TextNumberRecognizer {

    fun recognize(context: Context, photoFile: File): Task<List<String>> {
        val image = InputImage.fromFilePath(context, Uri.fromFile(photoFile))
        return TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .process(image)
            .continueWith { task -> extractCandidates(task.result?.text ?: "") }
    }

    fun extractCandidates(text: String): List<String> {
        val counts = linkedMapOf<String, Int>()
        Regex("[A-Za-zА-Яа-я0-9]{4,}").findAll(text).forEach { match ->
            val s = match.value.trim()
            if (s.count { it.isDigit() } >= 3) {
                counts[s] = (counts[s] ?: 0) + 1
            }
        }
        return counts.entries
            .sortedByDescending { it.value }
            .map { it.key }
            .take(5)
    }
}
