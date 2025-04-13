import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import java.io.*
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

class RequestContext(
    private val exchange: HttpExchange,
    private val mapper: ObjectMapper,
    private val pathParams: Map<String, String> = emptyMap()
) {
    val path: String
        get() = exchange.requestURI.path

    val method: String
        get() = exchange.requestMethod

    val headers: Map<String, List<String>>
        get() = exchange.requestHeaders

    private val formData = mutableMapOf<String, FormField>()
    private var parsedBody: Any? = null

    init {
        if (isMultipartFormData()) {
            parseMultipartFormData()
        }
    }

    data class FormField(
        val value: String,
        val fileName: String? = null,
        val contentType: String? = null,
        val data: ByteArray? = null
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as FormField

            if (value != other.value) return false
            if (fileName != other.fileName) return false
            if (contentType != other.contentType) return false
            if (data != null) {
                if (other.data == null) return false
                if (!data.contentEquals(other.data)) return false
            } else if (other.data != null) return false

            return true
        }

        override fun hashCode(): Int {
            var result = value.hashCode()
            result = 31 * result + (fileName?.hashCode() ?: 0)
            result = 31 * result + (contentType?.hashCode() ?: 0)
            result = 31 * result + (data?.contentHashCode() ?: 0)
            return result
        }
    }

    private fun isMultipartFormData(): Boolean {
        val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: return false
        return contentType.startsWith("multipart/form-data")
    }

    private fun parseMultipartFormData() {
        val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: return
        val boundary = contentType.substringAfter("boundary=").trim()
        val inputStream = exchange.requestBody
        val reader = BufferedReader(InputStreamReader(inputStream))
        
        var currentField: String? = null
        var currentFileName: String? = null
        var currentContentType: String? = null
        val fieldBuffer = StringBuilder()
        var isReadingField = false
        
        while (true) {
            val line = reader.readLine() ?: break
            
            when {
                line.startsWith("--$boundary") -> {
                    // Save previous field if exists
                    if (currentField != null) {
                        formData[currentField] = FormField(
                            value = fieldBuffer.toString().trim(),
                            fileName = currentFileName,
                            contentType = currentContentType
                        )
                        fieldBuffer.clear()
                    }
                    currentField = null
                    currentFileName = null
                    currentContentType = null
                    isReadingField = false
                }
                line.startsWith("Content-Disposition:") -> {
                    // Parse field name and filename
                    val disposition = line.substringAfter("Content-Disposition: form-data; ")
                    currentField = disposition.substringAfter("name=\"").substringBefore("\"")
                    currentFileName = if (disposition.contains("filename=")) {
                        disposition.substringAfter("filename=\"").substringBefore("\"")
                    } else null
                }
                line.startsWith("Content-Type:") -> {
                    currentContentType = line.substringAfter("Content-Type: ")
                }
                line.isBlank() -> {
                    isReadingField = true
                }
                isReadingField -> {
                    fieldBuffer.append(line).append("\n")
                }
            }
        }
        
        // Save last field if exists
        if (currentField != null) {
            formData[currentField] = FormField(
                value = fieldBuffer.toString().trim(),
                fileName = currentFileName,
                contentType = currentContentType
            )
        }
    }

    val parameters: Map<String, String>
        get() = exchange.requestURI.query
            ?.split("&")
            ?.mapNotNull {
                val parts = it.split("=")
                if (parts.size == 2) {
                    URLDecoder.decode(parts[0], StandardCharsets.UTF_8) to
                            URLDecoder.decode(parts[1], StandardCharsets.UTF_8)
                } else null
            }?.toMap() ?: emptyMap()

    val pathVariables: Map<String, String>
        get() = pathParams

    fun pathParam(name: String): String? = pathParams[name]

    fun formField(name: String): FormField? = formData[name]

    fun formFields(): Map<String, FormField> = formData.toMap()

    private val body: String
        get() = BufferedReader(InputStreamReader(exchange.requestBody)).use { it.readText() }

    fun <T : Any> receive(type: KClass<T>): T {
        if (parsedBody != null) {
            @Suppress("UNCHECKED_CAST")
            return parsedBody as T
        }

        val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: ""
        val body = when {
            contentType.startsWith("application/json") -> {
                exchange.requestBody.bufferedReader().use { it.readText() }
            }
            contentType.startsWith("multipart/form-data") -> {
                mapper.writeValueAsString(formData.mapValues { it.value.value })
            }
            else -> {
                exchange.requestBody.bufferedReader().use { it.readText() }
            }
        }

        parsedBody = mapper.readValue(body, type.java)
        return parsedBody as T
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