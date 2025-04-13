package app.pankaj

import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.reflect.KClass

class RequestContext(
    private val exchange: HttpExchange,
    private val mapper: ObjectMapper
) {
    val path: String
        get() = exchange.requestURI.path

    val method: String
        get() = exchange.requestMethod

    val headers: Map<String, List<String>>
        get() = exchange.requestHeaders

    private val parameters: Map<String, String>
        get() = exchange.requestURI.query?.split("&")?.associate {
            val parts = it.split("=")
            if (parts.size == 2) parts[0] to parts[1] else parts[0] to ""
        } ?: emptyMap()

    private val body: String
        get() = BufferedReader(InputStreamReader(exchange.requestBody)).use { it.readText() }

    fun <T : Any> receive(type: KClass<T>): T {
        return mapper.readValue(body, type.java)
    }

    inline fun <reified T : Any> receive(): T {
        return receive(T::class)
    }

    fun respond(status: Int, body: Any) {
        val responseBody = mapper.writeValueAsString(body)
        val responseBytes = responseBody.toByteArray()

        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, responseBytes.size.toLong())

        exchange.responseBody.use { os ->
            os.write(responseBytes)
            os.flush()
        }
    }

    fun respondText(status: Int, text: String) {
        val responseBytes = text.toByteArray()

        exchange.responseHeaders.add("Content-Type", "text/plain")
        exchange.sendResponseHeaders(status, responseBytes.size.toLong())

        exchange.responseBody.use { os ->
            os.write(responseBytes)
            os.flush()
        }
    }

    fun redirect(url: String) {
        exchange.responseHeaders.add("Location", url)
        exchange.sendResponseHeaders(302, 0)
        exchange.responseBody.close()
    }
}