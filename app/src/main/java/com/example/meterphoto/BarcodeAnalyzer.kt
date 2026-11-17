package com.example.meterphoto

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

/**
 * Анализатор видеопотока CameraX.
 * Распознаёт QR, EAN-13, Code-128, DataMatrix и др. — офлайн.
 */
class BarcodeAnalyzer(
    private val onCode: (String) -> Unit
) : ImageAnalysis.Analyzer {

    private val scanner = BarcodeScanning.getClient()
    @Volatile private var busy = false

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: return imageProxy.close()
        if (busy) { imageProxy.close(); return }
        busy = true
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                barcodes.firstOrNull()?.rawValue?.let(onCode)
            }
            .addOnCompleteListener {
                busy = false
                imageProxy.close()
            }
    }
}
