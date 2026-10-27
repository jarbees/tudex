package tuda.campus.mobile

interface Platform {
    val name: String
}


//expect class SystemImage    // iOS -> UIImage, Android -> ???

expect fun getPlatform(): Platform

expect fun randomUUID(): String

expect fun sha256(data: ByteArray): ByteArray