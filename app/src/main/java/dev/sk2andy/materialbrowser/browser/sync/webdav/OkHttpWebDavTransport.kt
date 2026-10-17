package dev.sk2andy.materialbrowser.browser.sync.webdav

import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavRequest
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavResponse
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavSyncFormat
import dev.sk2andy.materialbrowser.shared.sync.webdav.WebDavTransport
import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * [WebDavTransport] on OkHttp: HTTPS only with the system's certificate checks, no redirect
 * (a server could otherwise send the app password to another host or to `http`), no cookies or
 * cache, bounded waits and a bounded body.
 */
internal class OkHttpWebDavTransport(
    clientOnFirstUse: Lazy<OkHttpClient> = lazy(::defaultClient),
    private val maxBodyBytes: Long = WebDavSyncFormat.MAX_DEVICE_FILE_CHARS.toLong(),
) : WebDavTransport {
    /** Built on the first request, on the sync's thread: the app start never pays for OkHttp's setup. */
    private val client by clientOnFirstUse

    override fun send(request: WebDavRequest): WebDavResponse {
        require(request.url.startsWith("https://")) { "HTTPS only" }
        val contentType = request.headers["Content-Type"]
        val body = request.body?.toRequestBody(contentType?.toMediaTypeOrNull())
        val call = Request.Builder()
            .url(request.url)
            .method(request.method, body)
            .apply { request.headers.forEach { (name, value) -> if (name != "Content-Type") header(name, value) } }
            .build()
        return client.newCall(call).execute().use { response ->
            val source = response.body.source()
            source.request(maxBodyBytes + 1)
            if (source.buffer.size > maxBodyBytes) throw IOException("WebDAV answer above the limit")
            WebDavResponse(response.code, response.header("ETag"), source.buffer.readByteArray())
        }
    }

    private companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .callTimeout(2, TimeUnit.MINUTES)
            .build()
    }
}
