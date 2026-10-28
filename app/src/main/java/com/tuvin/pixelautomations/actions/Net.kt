package com.tuvin.pixelautomations.actions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.HttpURLConnection
import java.net.URL

data class HttpResult(val code: Int, val body: String) {
    val ok get() = code in 200..299
}

object Http {
    /** One HTTP request with a 15 s timeout. Throws on network errors; non-2xx comes back as a result. */
    suspend fun request(url: String, method: String, headers: Map<String, String> = emptyMap(), body: String? = null): HttpResult =
        withContext(Dispatchers.IO) {
            val conn = URL(url).openConnection() as HttpURLConnection
            try {
                conn.requestMethod = method.uppercase()
                conn.connectTimeout = 15_000
                conn.readTimeout = 15_000
                headers.forEach { (k, v) -> conn.setRequestProperty(k, v) }
                if (body != null && method.uppercase() !in setOf("GET", "HEAD")) {
                    conn.doOutput = true
                    if (headers.keys.none { it.equals("Content-Type", ignoreCase = true) }) {
                        conn.setRequestProperty("Content-Type", "application/json")
                    }
                    conn.outputStream.use { it.write(body.toByteArray()) }
                }
                val code = conn.responseCode
                val stream = if (code >= 400) conn.errorStream else conn.inputStream
                HttpResult(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty().take(4_000))
            } finally {
                conn.disconnect()
            }
        }

    /** "Name: value" lines → map; blank and malformed lines are skipped. */
    fun parseHeaders(text: String): Map<String, String> = text.lines()
        .mapNotNull { line -> line.indexOf(':').takeIf { it > 0 }?.let { line.substring(0, it).trim() to line.substring(it + 1).trim() } }
        .filter { it.first.isNotEmpty() }
        .toMap()
}

object HomeAssistantApi {
    private val json = Json { ignoreUnknownKeys = true }

    /** "light.turn_on" → "/api/services/light/turn_on". */
    fun path(service: String): String {
        val (domain, name) = service.trim().split('.', limit = 2).let { it.getOrNull(0).orEmpty() to it.getOrNull(1).orEmpty() }
        require(domain.isNotEmpty() && name.isNotEmpty()) { "service must look like domain.service, e.g. light.turn_on" }
        return "/api/services/$domain/$name"
    }

    /** entity_id plus any extra JSON fields, e.g. {"brightness_pct": 40}. */
    fun body(entityId: String, extraJson: String): String {
        val extra: Map<String, JsonElement> =
            if (extraJson.isBlank()) emptyMap() else json.parseToJsonElement(extraJson).jsonObject
        val fields = buildMap {
            if (entityId.isNotBlank()) put("entity_id", JsonPrimitive(entityId.trim()))
            putAll(extra)
        }
        return JsonObject(fields).toString()
    }
}
