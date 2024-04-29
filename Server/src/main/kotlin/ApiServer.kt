import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.OutputStream
import java.net.InetSocketAddress

data class ServerContext(val path: String, val server: HttpServer)

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
        if (methods.isNotEmpty() && exchange.requestMethod == methods) {
            handler.invoke(exchange)
        } else {
            val response = """{"error": "Method Not Allowed"}"""
            exchange.sendResponseHeaders(405, response.length.toLong())
            exchange.responseHeaders.add("Content-Type", "application/json")
            val outputStream: OutputStream = exchange.responseBody
            outputStream.write(response.toByteArray())
            outputStream.close()
        }
    }
}


fun apiServer(port: Int = 8080, block: HttpServer.() -> Unit): HttpServer {
    val server = HttpServer.create(InetSocketAddress(port), 0)
    server.block()
    return server
}
