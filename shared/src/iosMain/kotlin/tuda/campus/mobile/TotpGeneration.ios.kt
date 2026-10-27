package tuda.campus.mobile

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CoreCrypto.CCHmac
import platform.CoreCrypto.CC_SHA1_DIGEST_LENGTH
import platform.CoreCrypto.CC_SHA256_DIGEST_LENGTH
import platform.CoreCrypto.kCCHmacAlgSHA1
import platform.CoreCrypto.kCCHmacAlgSHA256
import platform.Foundation.timeIntervalSince1970


@OptIn(ExperimentalForeignApi::class)
actual fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray {
    val result = ByteArray(CC_SHA1_DIGEST_LENGTH)

    key.usePinned { keyPinned ->
        data.usePinned { dataPinned ->
            result.usePinned { resultPinned ->
                CCHmac(
                    kCCHmacAlgSHA1,
                    keyPinned.addressOf(0),
                    key.size.toULong(),
                    dataPinned.addressOf(0),
                    data.size.toULong(),
                    resultPinned.addressOf(0)
                )
            }
        }
    }
    return result
}

actual fun currentTimeMillis(): Long =
    (platform.Foundation.NSDate().timeIntervalSince1970 * 1000).toLong()

@OptIn(ExperimentalForeignApi::class)
actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val result = ByteArray(CC_SHA256_DIGEST_LENGTH)

    key.usePinned { keyPinned ->
        data.usePinned { dataPinned ->
            result.usePinned { resultPinned ->
                CCHmac(
                    kCCHmacAlgSHA256,
                    keyPinned.addressOf(0),
                    key.size.toULong(),
                    dataPinned.addressOf(0),
                    data.size.toULong(),
                    resultPinned.addressOf(0)
                )
            }
        }
    }
    return result
}