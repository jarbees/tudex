package tuda.campus.mobile

import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.statement.HttpResponse
import io.ktor.http.Parameters
import kotlinx.coroutines.CancellationException

suspend inline fun <T> HttpClient.safeRequest(
    crossinline block: suspend HttpClient.() -> T
): T? {
    repeat (3) {
        try {
            return block()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (!isSuspensionError(e)) {
                return null
            }
        }
    }

    return null
}

suspend fun HttpClient.safeGet(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse? =
    safeRequest {
        get(url, block)
    }

suspend fun HttpClient.safePost(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {}
): HttpResponse? =
    safeRequest {
        post(url, block)
    }

suspend fun HttpClient.safeSubmitForm(
    url: String,
    formParameters: Parameters
): HttpResponse? =
    safeRequest {
        submitForm(url, formParameters)
    }


fun isSuspensionError(exception: Throwable): Boolean {
    val message = exception.message ?: return false

    // These should be unique to errors produced by app suspension
    val hasNSURLError = "NSURLErrorDomain Code=-1005" in message
    val hasPOSIXCode53 = "_kCFStreamErrorCodeKey=53" in message
    val hasPOSIXDomain = "_kCFStreamErrorDomainKey=1" in message

    // These shouldn't be in errors produced by app suspension
    val notTimeout = !message.contains("NSURLErrorTimedOut")
    val notConnectivity = !message.contains("NSURLErrorNotConnectedToInternet")
    val notResourceNotFound = !message.contains("NSURLErrorResourceNotFound")

    return hasNSURLError && hasPOSIXCode53 && hasPOSIXDomain &&
            notTimeout && notConnectivity && notResourceNotFound
}