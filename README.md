Kotlin Coroutine-Based HTTP Server (Fun Project)
==================================

This project is a lightweight HTTP server library built using **Kotlin Coroutines**. It allows you to define RESTful routes easily and handle JSON-based HTTP requests/responses in a clean, coroutine-first style.

🚀 Features
-----------

*   Lightweight and fast server
*   Clean routing DSL
*   JSON request/response handling
*   Coroutine-based request processing
*   Dynamic routes and path parameters
*   Easy to extend and customize

🧰 Tech Stack
-------------

*   Kotlin (JVM)
*   Coroutines
*   kotlinx.serialization (for JSON handling)
*   Java’s built-in `HttpServer`

🚀 Getting Started
------------------

Follow these steps to get the server up and running:

### 1\. Clone the Repository

    git clone https://github.com/yourusername/coroutine-http-server.git
    cd coroutine-http-server

### 2\. Build the Project

    ./gradlew build

### 3\. Run the Server

    ./gradlew run

📌 Example API Endpoints
------------------------

### ✅ Health Check

**URL:** `/api`

**Method:** `GET`

**Response:**

    
    {
      "status": "healthy",
      "timestamp": 1715018245000
    }
        

### 🔐 Login

**URL:** `/api`

**Method:** `POST`

**Request Body:**

    
    {
      "email": "user@example.com",
      "password": "password123"
    }
        

**Response:**

    
    {
      "success": true,
      "message": "Login successful",
      "user": {
        "email": "user@example.com"
      }
    }
        

### 🔄 Dynamic JSON

**URL:** `/api/dynamic`

**Method:** `POST`

**Request Body:** Any JSON object

**Response:**

    
    {
      "received": { "key": "value" },
      "message": "Data received successfully"
    }
        

### 👤 User Routes

#### GET User by ID

**URL:** `/api/users/{id}`

**Method:** `GET`

#### PUT Update User

**URL:** `/api/users/{id}`

**Method:** `PUT`

**Request Body:**

    
    {
      "name": "John Doe",
      "email": "john@example.com"
    }
        

#### DELETE User

**URL:** `/api/users/{id}`

**Method:** `DELETE`

📁 Project Structure
--------------------

    
    src/
    ├── Main.kt                   # Entry point, defines API routes
    ├── Server.kt                 # Core server engine
    ├── Context.kt                # Request context handler
    ├── Models.kt                 # Request/response models
    └── Json.kt                   # JSON serializer using kotlinx.serialization
        

🧩 Extending the Server
-----------------------

You can define your own routes using the `route`, `get`, `post`, `put`, and `delete` functions:

    route("/myroute") {
        get { ctx ->
            ctx.respond(200, mapOf("message" to "Hello from /myroute"))
        }
    }
        

🧪 Test with curl
-----------------

    curl -X POST http://localhost:8080/api -H "Content-Type: application/json" \
      -d '{"email": "test@example.com", "password": "123456"}'
        

👨‍💻 Author
------------

Developed with ❤️ by [Pankaj](https://github.com/pankaj046)

📄 License
----------

This project is licensed under the MIT License.
