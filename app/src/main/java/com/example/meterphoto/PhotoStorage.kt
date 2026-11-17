package com.example.meterphoto

import android.content.Context
import android.os.Environment
import java.io.File

/** Сохраняет фото с именем, равным распознанному номеру счётчика. */
object PhotoStorage {

    fun saveAs(context: Context, source: File, number: String): File {
        val safeName = number.replace(Regex("[^A-Za-z0-9а-яА-Я_-]"), "_")
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)!!
        val dest = File(dir, "$safeName.jpg")
        source.copyTo(dest, overwrite = true)
        return dest
    }
}
