package app.pankaj

import apiServer
import route
import java.io.OutputStream


fun main() {

    apiServer(port = 8001) {
        route("/api") {
            route("/{id}", "GET") { exchange->
                exchange.sendResponseHeaders(200, "Hello World".length.toLong())
                exchange.responseHeaders.add("Content-Type", "text/plain")
                val outputStream: OutputStream = exchange.responseBody
                outputStream.write("Hello World".toByteArray())
                outputStream.close()
            }
        }

    }.start()
}