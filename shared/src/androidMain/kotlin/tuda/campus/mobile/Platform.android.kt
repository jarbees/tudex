package tuda.campus.mobile

import android.os.Build
import java.security.MessageDigest
import java.util.UUID

class AndroidPlatform : Platform {
    override val name: String = "Android ${Build.VERSION.SDK_INT}"
}

// Temporary
//actual typealias SystemImage = String

actual fun getPlatform(): Platform = AndroidPlatform()

actual fun randomUUID(): String = UUID.randomUUID().toString()

actual fun sha256(data: ByteArray): ByteArray {
    return MessageDigest.getInstance("SHA-256").digest(data)
}