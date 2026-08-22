package com.example.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class FetchResult(
    val isSuccess: Boolean,
    val html: String? = null,
    val title: String? = null,
    val statusCode: Int = 0,
    val errorMessage: String? = null,
    val responseTimeMs: Long = 0
)

object WebpageFetcher {

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/128.0.0.0 Mobile Safari/537.36 WebWatch/1.0"

    fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return if (!trimmed.startsWith("http://", ignoreCase = true) &&
            !trimmed.startsWith("https://", ignoreCase = true)
        ) {
            "https://$trimmed"
        } else {
            trimmed
        }
    }

    suspend fun fetchUrl(url: String): FetchResult = withContext(Dispatchers.IO) {
        val normalizedUrl = normalizeUrl(url)
        val startTime = System.currentTimeMillis()

        try {
            val request = Request.Builder()
                .url(normalizedUrl)
                .header("User-Agent", USER_AGENT)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Language", "en-US,en;q=0.9")
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build()

            client.newCall(request).execute().use { response ->
                val elapsed = System.currentTimeMillis() - startTime
                val code = response.code

                if (!response.isSuccessful) {
                    return@withContext FetchResult(
                        isSuccess = false,
                        statusCode = code,
                        errorMessage = "HTTP Error $code: ${response.message}",
                        responseTimeMs = elapsed
                    )
                }

                val bodyString = response.body?.string() ?: ""
                val title = extractTitle(bodyString)

                FetchResult(
                    isSuccess = true,
                    html = bodyString,
                    title = title,
                    statusCode = code,
                    responseTimeMs = elapsed
                )
            }
        } catch (e: IOException) {
            FetchResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Network connection failed",
                responseTimeMs = System.currentTimeMillis() - startTime
            )
        } catch (e: Exception) {
            FetchResult(
                isSuccess = false,
                errorMessage = e.localizedMessage ?: "Unexpected error fetching URL",
                responseTimeMs = System.currentTimeMillis() - startTime
            )
        }
    }

    private fun extractTitle(html: String): String? {
        return try {
            val matcher = Pattern.compile("(?is)<title>(.*?)</title>").matcher(html)
            if (matcher.find()) {
                matcher.group(1)?.trim()?.replace("\n", " ")?.replace("\r", "")
            } else null
        } catch (e: Exception) {
            null
        }
    }
}
