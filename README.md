Project Name: HTTP Server Framework (Fun Project)
===================================

Description
-----------

This project is a lightweight HTTP server framework implemented in Kotlin, providing developers with an easy-to-use platform to create RESTful APIs. It simplifies the process of setting up HTTP servers and defining routes for handling various HTTP methods and request types.

Features
--------

*   **Simple Routing:** Define routes effortlessly using a fluent API. || DONE
*   **Status Code Handling:** Automatically handle common HTTP status codes.  || PENDING
*   **Content Type Validation:** Validate incoming request content types. || PENDING
*   **Error Handling:** Handle method not allowed, content type not supported, and route not found errors gracefully.  || WORKING
*   **Customizable:** Easily customize port number and server configurations.  || DONE

Installation
------------

1.  Clone this repository.
    
        git clone https://github.com/pankaj046/RestApiServer.git
    
2.  Import the project into your preferred Kotlin IDE.

Usage
-----

1.  Create a new HTTP server instance using the `apiServer` function, specifying the port number.
    
        val server = apiServer(port = 8080) {
            // Define server routes here
        }
    
2.  Define routes using the `route` function within the server instance.
    
        server.route("/example") {
            // Define route handling logic here
        }
    
3.  Implement route handling logic within the provided handler function, specifying the HTTP method(s) and desired behavior.
    
        server.route("/example", methods = "GET") { exchange ->
            // Handle GET requests for the "/example" route
        }
    
4.  Start the server by calling the `start` method.
    
        server.start()
    

Example
-------

Here's a simple example demonstrating how to create a basic HTTP server using this framework:

    import com.sun.net.httpserver.HttpExchange;
    import com.sun.net.httpserver.HttpServer;
    import java.net.InetSocketAddress;
    
    fun main() {
        val server = apiServer(port = 8080) {
            route("/hello", methods = "GET") { exchange ->
                val response = "Hello, World!";
                exchange.sendResponseHeaders(200, response.length.toLong());
                val outputStream = exchange.responseBody;
                outputStream.write(response.toByteArray());
                outputStream.close();
            }
        }.start();
        println("Server started on port 8080");
    }

Dependencies
------------

*   Kotlin
*   JDK (Java Development Kit)

License
-------

# Released under MIT License

Permission is hereby granted, free of charge, to any person obtaining a copy 
of this software and associated documentation files (the "Software"), to deal 
in the Software without restriction, including without limitation the rights 
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all 
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

Authors
-------

*   [Pankaj]([https://github.com/pankaj046](https://github.com/pankaj046))

Contact
-------

For any inquiries or support, please contact [pankaj\dev.pankaj046@gmail.com](mailto:dev.pankaj046@gmail.com).
