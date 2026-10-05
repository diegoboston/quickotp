package com.quickotp.app.barcode

import android.content.Context
import android.net.Uri
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.io.IOException

/** ML Kit setup shared by the camera scanner and the gallery image scan. */
object MlKitBarcodes {
    val options: BarcodeScannerOptions = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_PDF417,
            Barcode.FORMAT_CODE_128,
            Barcode.FORMAT_CODE_39,
            Barcode.FORMAT_CODE_93,
            Barcode.FORMAT_EAN_13,
            Barcode.FORMAT_EAN_8,
        )
        .build()

    fun formatName(format: Int): String = when (format) {
        Barcode.FORMAT_QR_CODE -> "QR_CODE"
        Barcode.FORMAT_AZTEC -> "AZTEC"
        Barcode.FORMAT_DATA_MATRIX -> "DATA_MATRIX"
        Barcode.FORMAT_PDF417 -> "PDF_417"
        Barcode.FORMAT_CODE_128 -> "CODE_128"
        Barcode.FORMAT_CODE_39 -> "CODE_39"
        Barcode.FORMAT_CODE_93 -> "CODE_93"
        Barcode.FORMAT_EAN_13 -> "EAN_13"
        Barcode.FORMAT_EAN_8 -> "EAN_8"
        Barcode.FORMAT_UPC_A -> "UPC_A"
        Barcode.FORMAT_UPC_E -> "UPC_E"
        Barcode.FORMAT_CODABAR -> "CODABAR"
        Barcode.FORMAT_ITF -> "ITF"
        else -> "QR_CODE"
    }

    /**
     * Reads the first barcode in the image at [uri]. Callbacks run on the main thread;
     * [onNotFound] covers unreadable images as well as images without a barcode.
     */
    fun scanImage(
        context: Context,
        uri: Uri,
        onFound: (raw: String, format: String) -> Unit,
        onNotFound: () -> Unit,
    ) {
        val image = try {
            InputImage.fromFilePath(context, uri)
        } catch (_: IOException) {
            onNotFound()
            return
        }
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val scanner = BarcodeScanning.getClient(options)
        scanner.process(image)
            .addOnSuccessListener(mainExecutor) { barcodes ->
                val hit = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }
                val raw = hit?.rawValue
                if (hit != null && !raw.isNullOrBlank()) {
                    onFound(raw, formatName(hit.format))
                } else {
                    onNotFound()
                }
            }
            .addOnFailureListener(mainExecutor) { onNotFound() }
            .addOnCompleteListener(mainExecutor) { scanner.close() }
    }
}
