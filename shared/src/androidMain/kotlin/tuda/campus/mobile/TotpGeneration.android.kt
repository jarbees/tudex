package tuda.campus.mobile

import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec


actual fun hmacSha1(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA1")
    mac.init(SecretKeySpec(key, "HmacSHA1"))
    return mac.doFinal(data)
}

actual fun hmacSha256(key: ByteArray, data: ByteArray): ByteArray {
    val mac = Mac.getInstance("HmacSHA256")
    mac.init(SecretKeySpec(key, "HmacSHA256"))
    return mac.doFinal(data)
}

actual fun currentTimeMillis(): Long = System.currentTimeMillis()