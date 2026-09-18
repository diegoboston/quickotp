package com.quickotp.app.barcode

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.common.BitMatrix

object QrBitmaps {
    fun encode(content: String, size: Int = 512): Bitmap = QrCodec.encode(content, size).toBitmap()
}

fun BitMatrix.toBitmap(): Bitmap {
    val pixels = IntArray(width * height)
    var i = 0
    for (y in 0 until height) {
        for (x in 0 until width) {
            pixels[i++] = if (get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.RGB_565)
}
