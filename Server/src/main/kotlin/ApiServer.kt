package app.pankaj

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import kotlinx.coroutines.*
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.concurrent.Executors

class ApiServer(private val port: Int = 8080) {
    private val server = HttpServer.create(InetSocketAddress(port), 0)
    private val mapper = jacksonObjectMapper()
    private val routes = mutableListOf<Route>()

    // Coroutine scope for handling requests
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob() + CoroutineName("ApiServerScope"))

    fun configure(block: ApiServer.() -> Unit) {
        block()
    }

    fun start() {
        server.executor = Executors.newFixedThreadPool(10)

        routes.forEach { route ->
            server.createContext(route.path) { exchange ->
                handleRequest(exchange, route)
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

    fun route(path: String, block: Route.() -> Unit) {
        val route = Route(path)
        route.block()
        routes.add(route)
    }

    inner class Route(val path: String) {
        private val handlers = mutableMapOf<String, RequestHandler>()

        fun get(handler: suspend (RequestContext) -> Unit) {
            handlers["GET"] = RequestHandler("GET", handler)
        }

        fun post(handler: suspend (RequestContext) -> Unit) {
            handlers["POST"] = RequestHandler("POST", handler)
        }

        fun put(handler: suspend (RequestContext) -> Unit) {
            handlers["PUT"] = RequestHandler("PUT", handler)
        }

        fun delete(handler: suspend (RequestContext) -> Unit) {
            handlers["DELETE"] = RequestHandler("DELETE", handler)
        }

        fun getHandler(method: String): RequestHandler? {
            return handlers[method]
        }
    }

    data class RequestHandler(
        val method: String,
        val handler: suspend (RequestContext) -> Unit
    )

    private fun handleRequest(exchange: HttpExchange, route: Route) {
        val method = exchange.requestMethod
        val handler = route.getHandler(method)

        if (handler == null) {
            sendError(exchange, 405, "Method Not Allowed")
            return
        }

        coroutineScope.launch {
            try {
                val context = RequestContext(exchange, mapper)
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