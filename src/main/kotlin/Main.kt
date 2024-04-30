package app.pankaj

import apiServer
import route
import java.io.OutputStream


fun main() {

    apiServer(8080) {
        route("/api") {
            route(path = "1" , methods = "POST") { exchange ->
                val response = "Hello, World!"
                exchange.sendResponseHeaders(200, response.length.toLong())
                val outputStream: OutputStream = exchange.responseBody
                outputStream.write(response.toByteArray())
                outputStream.close()
            }
        }
    }.start()
}