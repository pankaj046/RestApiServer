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

fun ServerContext.route(path: String = "", methods: String, handler: (HttpExchange) -> Unit) {
    val fullPath = if (path.isNotEmpty()) {
        "${this.path}/$path"
    } else {
        this.path
    }

    this.server.createContext(fullPath) { exchange ->
        handleRequestError(exchange, methods, fullPath)

        if (methods.isNotEmpty() && exchange.requestMethod == methods
            && exchange.requestURI.path == fullPath) {
            handler.invoke(exchange)
            return@createContext
        }
        handleError(exchange, getStatusByCode(500))
    }
}

private fun handleRequestError(exchange: HttpExchange, methods: String, path: String) {
    val requestMethod = exchange.requestMethod
    val reqContentType = exchange.requestHeaders.getFirst("Content-Type")
    val reqPath = exchange.requestURI.path

    if (!isMethodAllowed(methods, requestMethod)) {
        handleError(exchange, getStatusByCode(405))
    }

    if (!contentTypes.contains(reqContentType)){
        handleError(exchange, getStatusByCode(400))
    }

    if (path == "/notfound" || path != reqPath) {
        handleError(exchange, getStatusByCode(404))
    }
}


private fun handleError(exchange: HttpExchange, status:StatusCode) {
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
    for (statusCode in statusCodes) {
        if (statusCode.code == code) {
            return statusCode
        }
    }
    return StatusCode(500, "Internal Server Error")
}

private fun isMethodAllowed(method: String, requestMethodType: String): Boolean {
    return requestMethodType == method
}