package tuda.campus.mobile

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.util.concurrent.ConcurrentHashMap

actual class PlatformImage actual constructor(bytes: ByteArray) {
    actual val bytes: ByteArray = bytes

    //val bitmap: Bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    companion object {
        private val bitmapCache = ConcurrentHashMap<Int, Bitmap>()
    }

    val bitmap: Bitmap

    init {
        val key = bytes.contentHashCode()
        bitmap = bitmapCache.getOrPut(key) {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }
    }
}


// Not actually used anymore, since Compose Multiplatform provides
// ByteArray.decodeToImageBitmap()
// for commonMain, which seems to perform fine