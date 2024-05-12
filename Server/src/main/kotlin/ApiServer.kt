import com.fasterxml.jackson.databind.ObjectMapper
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.InetSocketAddress


data class ServerContext(val path: String, val server: HttpServer)


data class StatusCode(val code: Int, val message: String)

val statusCodes = listOf(
    StatusCode(200, "OK"),
    StatusCode(201, "Created"),
    StatusCode(204, "No Content"),
    StatusCode(400, "Bad Request"),
    StatusCode(401, "Unauthorized"),
    StatusCode(403, "Forbidden"),
    StatusCode(404, "Not Found"),
    StatusCode(405, "Method Not Allowed"),
    StatusCode(409, "Conflict"),
    StatusCode(500, "Internal Server Error")
)

val contentTypes = arrayOf(
    "application/json",
    "application/xml",
    "application/x-www-form-urlencoded",
    "multipart/form-data",
    "text/plain",
    "text/html",
    "image/jpeg",
    "image/png",
    "application/pdf",
    "audio/mpeg",
    "video/mp4",
    "application/octet-stream"
)

fun HttpServer.route(path: String = "/", block: ServerContext.() -> Unit): ServerContext {
    val context = ServerContext(path, this)
    context.block()
    return context
}


fun ServerContext.route(
    path: String = "",
    methods: String,
    request: HashMap<String, Any>? = null,
    handler: (HttpExchange, HashMap<String, Any>?) -> Unit
) {
    val fullPath = if (path.isNotEmpty()) {
        "${this.path}/$path"
    } else {
        this.path
    }

    this.server.createContext(fullPath) { exchange ->
        handleRequestError(exchange, methods, fullPath)

        if (methods.isNotEmpty() && exchange.requestMethod == methods
            && exchange.requestURI.path == fullPath) {
            val requestBody = exchange.requestBody
            if (request == null){
                handler.invoke(exchange, null)
            }else{
                val requestData = RequestHandler.handleRequest(requestBody)
                if (requestData == null){
                    handleError(exchange, getStatusByCode(400))
                }
                handler.invoke(exchange, requestData!!)
            }
            return@createContext
        }
        handleError(exchange, getStatusByCode(500))
    }
}

fun <T> ServerContext.route(
    path: String = "",
    methods: String,
    request: Class<T>?,
    handler: (HttpExchange, T?) -> Unit
) {
    val fullPath = if (path.isNotEmpty()) {
        "${this.path}/$path"
    } else {
        this.path
    }

    this.server.createContext(fullPath) { exchange ->
        handleRequestError(exchange, methods, fullPath)

        if (methods.isNotEmpty() && exchange.requestMethod == methods
            && exchange.requestURI.path == fullPath) {
            val requestBody = exchange.requestBody
            val requestData = if (request != null) {
                RequestHandler.readJsonFromInputStream(requestBody, request)
            } else {
                null
            }
            handler.invoke(exchange, requestData)
        } else {
            handleError(exchange, getStatusByCode(405))
        }
    }
}

fun <T> HttpExchange.response(status: Int, response: T) {
    val objectMapper = ObjectMapper()
    val responseBody = objectMapper.writeValueAsString(response)
    val bytes = responseBody.toByteArray(Charsets.UTF_8)
    responseHeaders.add("Content-Type", "application/json")
    sendResponseHeaders(status, bytes.size.toLong())
    val outputStream: OutputStream = getResponseBody()
    outputStream.write(bytes)
    outputStream.close()
}

private fun handleRequestError(exchange: HttpExchange, methods: String, path: String) {
    val requestMethod = exchange.requestMethod
    val reqContentType = exchange.requestHeaders.getFirst("Content-Type")
    val reqPath = exchange.requestURI.path

    if (methods.isNotEmpty() && requestMethod != methods) {
        handleError(exchange, getStatusByCode(405))
        return
    }

    if (!contentTypes.contains(reqContentType)) {
        handleError(exchange, getStatusByCode(400))
        return
    }

    if (path != reqPath) {
        handleError(exchange, getStatusByCode(404))
        return
    }
}

private fun handleError(exchange: HttpExchange, status: StatusCode) {
    val response = """{"message": "${status.message}", "code": ${status.code}}"""
    exchange.sendResponseHeaders(status.code, response.length.toLong())
    exchange.responseHeaders.add("Content-Type", "application/json")
    val outputStream: OutputStream = exchange.responseBody
    outputStream.write(response.toByteArray())
    outputStream.close()
}

fun apiServer(port: Int = 8080, block: HttpServer.() -> Unit): HttpServer {
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.block()
    return server
}

private fun getStatusByCode(code: Int): StatusCode {
    return statusCodes.firstOrNull { it.code == code } ?: StatusCode(500, "Internal Server Error")
}