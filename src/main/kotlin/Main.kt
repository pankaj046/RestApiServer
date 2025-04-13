package app.pankaj

import apiServer
import model.User
import model.Product
import model.Order
import model.FileUploadResponse

fun main() {
    apiServer(8080) {
        route("/users") {
            post { context ->
                val user = context.receive<User>()
                val createdUser = User(user.name, user.email)
                context.respond(201, createdUser)
            }
            get { context ->
                val users = listOf(
                    User("John Doe", "john.doe@example.com"),
                    User("Jane Smith", "jane.smith@example.com")
                )
                context.respond(200, users)
            }
        }

        route("/users/{id}") {
            get { context ->
                val userId = context.parameters["id"]
                val user = User("John Doe", "john.doe@example.com")
                context.respond(200, user)
            }

            put { context ->
                val userId = context.parameters["id"]
                val updatedUser = context.receive<User>()
                val user = User(updatedUser.name, updatedUser.email)
                context.respond(200, user)
            }
            
            delete { context ->
                val userId = context.parameters["id"]
                context.respond(200, mapOf("message" to "User with ID $userId deleted successfully"))
            }
        }

        route("/users/{id}/lock/{lockId}") {
            post { context ->
                val userId = context.parameters["id"] ?: return@post context.respond(400, mapOf("error" to "userId is required"))
                val lockId = context.parameters["lockId"] ?: return@post context.respond(400, mapOf("error" to "lockId is required"))
                context.respond(200, mapOf(
                    "message" to "User $userId locked with ID $lockId",
                    "userId" to userId,
                    "lockId" to lockId
                ))
            }
        }

        route("/users/search") {
            get { context ->
                val id = context.parameters["id"]
                val name = context.parameters["name"]
                val email = context.parameters["email"]
                
                val searchResults = listOf(
                    User(
                        name = name ?: "John Doe",
                        email = email ?: "john.doe@example.com"
                    )
                )
                
                context.respond(200, mapOf(
                    "results" to searchResults,
                    "query" to mapOf(
                        "id" to id,
                        "name" to name,
                        "email" to email
                    )
                ))
            }
        }

        route("/users/form") {
            post { context ->
                // Handle form data
                val formData = context.parameters
                context.respond(200, mapOf(
                    "message" to "Form data received successfully",
                    "data" to formData
                ))
            }
        }

        route("/upload") {
            post { context ->
                val name = context.formField("name")?.value
                val file = context.formField("file")
                
                context.respond(200, FileUploadResponse(
                    message = "File uploaded successfully",
                    fileName = file?.fileName,
                    contentType = file?.contentType,
                    name = name
                ))
            }
        }

        route("/upload/multiple") {
            post { context ->
                val name = context.formField("name")?.value
                val files = context.formFields().filter { it.key.startsWith("file") }
                
                val fileDetails = files.map { (key, field) ->
                    mapOf(
                        "fieldName" to key,
                        "fileName" to field.fileName,
                        "contentType" to field.contentType
                    )
                }
                
                context.respond(200, mapOf(
                    "message" to "Files uploaded successfully",
                    "name" to name,
                    "files" to fileDetails
                ))
            }
        }

        route("/mixed") {
            post { context ->
                // Check if it's multipart form data by looking at the form fields
                val isFormData = context.formFields().isNotEmpty()
                
                when {
                    !isFormData -> {
                        // Assume it's JSON if not form data
                        val data = context.receive<Map<String, Any>>()
                        context.respond(200, mapOf(
                            "type" to "json",
                            "data" to data
                        ))
                    }
                    else -> {
                        val formFields = context.formFields()
                        context.respond(200, mapOf(
                            "type" to "form",
                            "data" to formFields.mapValues { it.value.value }
                        ))
                    }
                }
            }
        }

        route("/text") {
            get { context ->
                context.respondText(200, "This is a plain text response")
            }
        }

        route("/old-path") {
            get { context ->
                context.redirect("/new-path")
            }
        }

        route("/new-path") {
            get { context ->
                context.respond(200, mapOf(
                    "message" to "You have been redirected to the new path"
                ))
            }
        }

        route("/users/{userId}/orders") {
            get { context ->
                val userId = context.parameters["userId"] ?: return@get context.respond(400, mapOf("error" to "userId is required"))
                val orders = listOf(
                    Order("order1", userId, "Pending"),
                    Order("order2", userId, "Completed")
                )
                context.respond(200, orders)
            }
            
            post { context ->
                val userId = context.parameters["userId"] ?: return@post context.respond(400, mapOf("error" to "userId is required"))
                val order = context.receive<Order>()
                val createdOrder = Order(order.id, userId, order.status)
                context.respond(201, createdOrder)
            }
        }

        route("/users/{userId}/orders/{orderId}") {
            get { context ->
                val userId = context.parameters["userId"] ?: return@get context.respond(400, mapOf("error" to "userId is required"))
                val orderId = context.parameters["orderId"] ?: return@get context.respond(400, mapOf("error" to "orderId is required"))
                val order = Order(orderId, userId, "Pending")
                context.respond(200, order)
            }
            
            put { context ->
                val userId = context.parameters["userId"] ?: return@put context.respond(400, mapOf("error" to "userId is required"))
                val orderId = context.parameters["orderId"] ?: return@put context.respond(400, mapOf("error" to "orderId is required"))
                val order = context.receive<Order>()
                val updatedOrder = Order(orderId, userId, order.status)
                context.respond(200, updatedOrder)
            }
            
            delete { context ->
                val userId = context.parameters["userId"] ?: return@delete context.respond(400, mapOf("error" to "userId is required"))
                val orderId = context.parameters["orderId"] ?: return@delete context.respond(400, mapOf("error" to "orderId is required"))
                context.respond(200, mapOf(
                    "message" to "Order $orderId for user $userId deleted successfully"
                ))
            }
        }

        route("/products") {
            get { context ->
                val category = context.parameters["category"]
                val minPrice = context.parameters["minPrice"]?.toDoubleOrNull()
                val maxPrice = context.parameters["maxPrice"]?.toDoubleOrNull()
                
                val products = listOf(
                    Product("1", "Laptop", 999.99, "Electronics"),
                    Product("2", "Desk", 199.99, "Furniture"),
                    Product("3", "Chair", 49.99, "Furniture")
                ).filter { product ->
                    (category == null || product.category == category) &&
                    (minPrice == null || product.price >= minPrice) &&
                    (maxPrice == null || product.price <= maxPrice)
                }
                
                context.respond(200, products)
            }
            
            post { context ->
                val product = context.receive<Product>()
                context.respond(201, product)
            }
        }
    }.start()
} 