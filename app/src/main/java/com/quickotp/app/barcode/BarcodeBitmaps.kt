package com.quickotp.app.barcode

import android.graphics.Bitmap

data class RenderedBarcode(
    val bitmap: Bitmap,
    val wide: Boolean,
)

object BarcodeBitmaps {
    fun render(content: String, formatName: String, size: Int = 512): RenderedBarcode {
        val encoded = BarcodeCodec.encode(content, formatName, size)
        return RenderedBarcode(
            bitmap = encoded.matrix.toBitmap(),
            wide = encoded.matrix.width > encoded.matrix.height,
        )
    }
}
