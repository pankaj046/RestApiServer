package app.pankaj

import model.*

fun main() {
    val server = apiServer(8080) {
        // Define routes
        route("/api") {
            // Login route
            post { ctx ->
                try {
                    val loginRequest = ctx.receive<LoginRequest>()

                    if (loginRequest.email.isBlank() || loginRequest.password.isBlank()) {
                        ctx.respond(400, ErrorResponse("Email and password are required"))
                        return@post
                    }

                    val response = LoginResponse(
                        success = true,
                        message = "Login successful",
                        user = User(loginRequest.email)
                    )

                    ctx.respond(200, response)
                } catch (e: Exception) {
                    ctx.respond(400, ErrorResponse("Invalid request format: ${e.message}"))
                }
            }

            // Health check
            get { ctx ->
                ctx.respond(200, mapOf(
                    "status" to "healthy",
                    "timestamp" to System.currentTimeMillis()
                ))
            }

            // Dynamic route that accepts any JSON
            route("/dynamic") {
                post { ctx ->
                    try {
                        val data = ctx.receive<Map<String, Any>>()

                        ctx.respond(200, mapOf(
                            "received" to data,
                            "message" to "Data received successfully"
                        ))
                    } catch (e: Exception) {
                        ctx.respond(400, ErrorResponse("Invalid request format: ${e.message}"))
                    }
                }
            }

            // Route with path parameter
            route("/users/{id}") {
                get { ctx ->
                    val userId = ctx.path.substringAfterLast("/")
                    ctx.respond(200, mapOf(
                        "id" to userId,
                        "name" to "model.User $userId",
                        "email" to "user$userId@example.com"
                    ))
                }

                put { ctx ->
                    try {
                        val updateRequest = ctx.receive<UserUpdateRequest>()
                        val userId = ctx.path.substringAfterLast("/")
                        ctx.respond(200, mapOf(
                            "id" to userId,
                            "name" to updateRequest.name,
                            "email" to updateRequest.email,
                            "message" to "model.User updated successfully"
                        ))
                    } catch (e: Exception) {
                        ctx.respond(400, ErrorResponse("Invalid request format: ${e.message}"))
                    }
                }

                delete { ctx ->
                    val userId = ctx.path.substringAfterLast("/")
                    ctx.respond(200, mapOf(
                        "id" to userId,
                        "message" to "model.User deleted successfully"
                    ))
                }
            }
        }
    }

    server.start()
    println("Server is running. Press Ctrl+C to stop.")
}