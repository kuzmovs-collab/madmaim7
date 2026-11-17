package com.example.meterphoto

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.meterphoto.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.io.File

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsStore
    private var imageCapture: ImageCapture? = null
    private var lastRecognizedCode: String? = null
    private var captureInProgress = false   // защита от двойной автосъёмки

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        settings = SettingsStore(this)

        binding.btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        binding.btnCapture.setOnClickListener { takePhoto() }

        if (allPermissionsGranted()) startCamera()
        else ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), RC_CAMERA)
    }

    // ---------- Камера ----------

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()

            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = binding.viewFinder.surfaceProvider }

            imageCapture = ImageCapture.Builder().build()

            val analyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(ContextCompat.getMainExecutor(this), BarcodeAnalyzer { code ->
                        lastRecognizedCode = code
                        runOnUiThread {
                            binding.tvCode.text = "Распознан: $code"
                            // Автосъёмка: как только штрихкод пойман — снимок делается сам
                            if (settings.autoCapture && !captureInProgress) takePhoto()
                        }
                    })
                }

            provider.unbindAll()
            provider.bindToLifecycle(
                this, CameraSelector.DEFAULT_BACK_CAMERA,
                preview, imageCapture, analyzer
            )
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        val capture = imageCapture ?: return
        if (captureInProgress) return
        captureInProgress = true
        val photoFile = File(cacheDir, "temp_meter.jpg")
        val output = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        capture.takePicture(output, ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    captureInProgress = false
                    showConfirmDialog(photoFile)
                }
                override fun onError(exception: ImageCaptureException) {
                    captureInProgress = false
                    Toast.makeText(this@MainActivity, "Ошибка съёмки", Toast.LENGTH_SHORT).show()
                }
            })
    }

    // ---------- Подтверждение номера ----------

    private fun showConfirmDialog(photoFile: File) {
        val edit = android.widget.EditText(this).apply {
            setText(lastRecognizedCode ?: "")
            hint = "Номер счётчика"
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Проверьте номер счётчика")
            .setView(edit)
            // Если штрихкода не было: распознаём печатный номер прямо с фото
            .setNeutralButton("Распознать с фото") { _, _ ->
                recognizeFromPhoto(photoFile) { candidates ->
                    showCandidatePicker(candidates) { chosen ->
                        edit.setText(chosen)
                    }
                }
            }
            .setPositiveButton("Сохранить и отправить") { _, _ ->
                val number = edit.text.toString().trim()
                if (number.isNotEmpty()) processPhoto(photoFile, number)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    /** Показывает варианты номера, найденные OCR, пользователь выбирает нужный. */
    private fun showCandidatePicker(candidates: List<String>, onPick: (String) -> Unit) {
        if (candidates.isEmpty()) {
            Toast.makeText(this, "Номер на фото не найден, введите вручную", Toast.LENGTH_LONG).show()
            return
        }
        MaterialAlertDialogBuilder(this)
            .setTitle("Выберите номер")
            .setItems(candidates.toTypedArray()) { _, which -> onPick(candidates[which]) }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun recognizeFromPhoto(photoFile: File, onDone: (List<String>) -> Unit) {
        Toast.makeText(this, "Распознавание номера...", Toast.LENGTH_SHORT).show()
        TextNumberRecognizer.recognize(this, photoFile)
            .addOnSuccessListener { runOnUiThread { onDone(it) } }
            .addOnFailureListener {
                runOnUiThread {
                    Toast.makeText(this, "Не удалось распознать: ${it.message}", Toast.LENGTH_LONG).show()
                }
            }
    }

    // ---------- Сохранение и постановка в очередь ----------

    private fun processPhoto(photoFile: File, number: String) {
        // 1. Переименование файла
        val savedFile = PhotoStorage.saveAs(this, photoFile, number)

        // 2. Постановка в очередь: отправится сама, когда появится интернет
        OutboxStore(this).enqueue(savedFile.absolutePath, number)
        SendWorker.schedule(this)

        val pending = OutboxStore(this).count
        Toast.makeText(
            this,
            "Сохранено. В очереди отправки: $pending фото",
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun allPermissionsGranted() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RC_CAMERA && allPermissionsGranted()) startCamera()
    }

    companion object { private const val RC_CAMERA = 10 }
}
