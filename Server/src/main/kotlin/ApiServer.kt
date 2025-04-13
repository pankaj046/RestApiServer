import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import model.FormField
import java.net.InetSocketAddress
import java.net.URLDecoder
import java.util.concurrent.Executors
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*
import kotlin.reflect.KClass

class ApiServer(private val port: Int = 8080) {
    private val server = HttpServer.create(InetSocketAddress(port), 0)
    private val mapper = jacksonObjectMapper()
    private val routes = mutableListOf<Route>()

    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("ApiServerScope"))

    fun configure(block: ApiServer.() -> Unit) {
        block()
    }

    fun start() {
        server.executor = Executors.newFixedThreadPool(10)

        server.createContext("/") { exchange ->
            val requestPath = exchange.requestURI.path
            val method = exchange.requestMethod

            val matchedRoute = routes.firstOrNull { it.matches(requestPath) }

            if (matchedRoute != null) {
                handleRequest(exchange, matchedRoute, method, requestPath)
            } else {
                sendError(exchange, 404, "Not Found")
            }
        }

        server.start()
        println("Server started on port $port")
    }

    fun stop() {
        server.stop(0)
        coroutineScope.cancel()
        println("Server stopped")
    }

    fun route(pathPattern: String, block: Route.() -> Unit) {
        val route = Route(pathPattern)
        route.block()
        routes.add(route)
    }

    inner class Route(private val pathPattern: String) {
        private val handlers = mutableMapOf<String, RequestHandler>()
        private val regex = Regex("^" + pathPattern
            .replace(Regex("\\{(\\w+)}"), "([^/]+)") + "$")

        fun matches(path: String): Boolean {
            return regex.matches(path)
        }

        fun extractParams(path: String): Map<String, String> {
            val match = regex.matchEntire(path) ?: return emptyMap()
            val keys = "\\{(\\w+)}".toRegex().findAll(pathPattern).map { it.groupValues[1] }.toList()
            return keys.zip(match.groupValues.drop(1)).toMap()
        }

        fun get(subPath: String = "", handler: suspend (RequestContext) -> Unit) {
            handlers["GET"] = RequestHandler("GET", handler)
        }

        fun post(subPath: String = "", handler: suspend (RequestContext) -> Unit) {
            handlers["POST"] = RequestHandler("POST", handler)
        }

        fun put(subPath: String = "", handler: suspend (RequestContext) -> Unit) {
            handlers["PUT"] = RequestHandler("PUT", handler)
        }

        fun delete(subPath: String = "", handler: suspend (RequestContext) -> Unit) {
            handlers["DELETE"] = RequestHandler("DELETE", handler)
        }

        fun getHandler(method: String): RequestHandler? {
            return handlers[method]
        }

        fun getParams(path: String): Map<String, String> = extractParams(path)
    }

    data class RequestHandler(
        val method: String,
        val handler: suspend (RequestContext) -> Unit
    )

    inner class RequestContext(
        private val exchange: HttpExchange,
        private val mapper: ObjectMapper,
        private val pathParams: Map<String, String>
    ) {
        private val queryParams = parseQueryParams(exchange.requestURI.query)
        private val formData = mutableMapOf<String, FormField>()
        private var parsedBody: Any? = null

        init {
            if (isMultipartFormData()) {
                parseMultipartFormData()
            }
        }

        private fun isMultipartFormData(): Boolean {
            val contentType = exchange.requestHeaders.getFirst("Content-Type") ?: return false
            return contentType.startsWith("multipart/form-data")
        }

        private fun parseQueryParams(query: String?): Map<String, String> {
            if (query == null) return emptyMap()
            return query.split("&").associate { param ->
                val (key, value) = param.split("=", limit = 2)
                URLDecoder.decode(key, "UTF-8") to URLDecoder.decode(value, "UTF-8")
            }
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

        inline fun <reified T : Any> receive(): T = receive(T::class)

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

        val parameters: Map<String, String>
            get() = pathParams + queryParams + formData.mapValues { it.value.value }

        fun formField(name: String): FormField? = formData[name]

        fun formFields(): Map<String, FormField> = formData.toMap()
    }

    private fun handleRequest(exchange: HttpExchange, route: Route, method: String, requestPath: String) {
        val handler = route.getHandler(method)
        if (handler == null) {
            sendError(exchange, 405, "Method Not Allowed")
            return
        }

        coroutineScope.launch {
            try {
                val context = RequestContext(exchange, mapper, route.getParams(requestPath))
                handler.handler(context)
            } catch (e: Exception) {
                sendError(exchange, 500, "Internal Server Error: ${e.message}")
            }
        }
    }

    private fun sendError(exchange: HttpExchange, status: Int, message: String) {
        val response = mapOf("error" to message)
        val responseBody = mapper.writeValueAsString(response)
        val responseBytes = responseBody.toByteArray()

        exchange.responseHeaders.add("Content-Type", "application/json")
        exchange.sendResponseHeaders(status, responseBytes.size.toLong())

        exchange.responseBody.use { os ->
            os.write(responseBytes)
            os.flush()
        }
    }
}