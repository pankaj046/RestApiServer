package app.pankaj

import apiServer
import response
import route

fun main() {

    apiServer(8080) {
        route("/api") {
            route(path = "1" , request = LoginRequest::class.java, methods = "POST") { exchange, req->
                exchange.response(200, req)
            }

            route(path = "2", request = HashMap(), methods = "POST") { exchange, req->
                val response = "${req?.get("Email")}"
                exchange.response(200, response)
            }
        }
    }.start()
}


data class LoginRequest(
    val email: String ="",
    val password: String =""
)