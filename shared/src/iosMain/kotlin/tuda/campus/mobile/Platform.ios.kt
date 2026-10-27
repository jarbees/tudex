package tuda.campus.mobile

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CC_SHA256
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.UIKit.UIDevice
import platform.Foundation.NSUUID

class IOSPlatform: Platform {
    override val name: String = UIDevice.currentDevice.systemName() + " " + UIDevice.currentDevice.systemVersion
}


actual fun getPlatform(): Platform = IOSPlatform()

actual fun randomUUID(): String = NSUUID().UUIDString()

@OptIn(ExperimentalForeignApi::class)
actual fun sha256(data: ByteArray): ByteArray {
    val digest = UByteArray(CC_SHA256_DIGEST_LENGTH)

    data.usePinned { pinned ->
        digest.usePinned { out ->
            CC_SHA256(
                pinned.addressOf(0),
                data.size.convert(),
                out.addressOf(0)
            )
        }
    }

    return digest.toByteArray()
}