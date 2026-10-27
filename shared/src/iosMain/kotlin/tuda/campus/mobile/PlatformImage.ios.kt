package tuda.campus.mobile

import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import platform.UIKit.UIImage
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.allocArrayOf
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.Foundation.create
import platform.posix.memcpy


actual class PlatformImage actual constructor(bytes: ByteArray) {
    actual val bytes: ByteArray = bytes

    val uiImage = UIImage(data = bytes.toData())
}


// For these two methods, see
// https://gist.github.com/noahsark769/61cfb7a8b7231e2069a9dab94cf74a62 or
// https://stackoverflow.com/a/58521109 (same person and answer)

@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun ByteArray.toData(): NSData = memScoped {
    NSData.create(bytes = allocArrayOf(this@toData),
        length = this@toData.size.toULong())
}

@OptIn(ExperimentalForeignApi::class)
fun NSData.toByteArray(): ByteArray = ByteArray(this@toByteArray.length.toInt()).apply {
    usePinned {
        memcpy(it.addressOf(0), this@toByteArray.bytes, this@toByteArray.length)
    }
}