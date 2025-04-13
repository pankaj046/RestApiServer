
fun apiServer(port: Int = 8080, block: ApiServer.() -> Unit): ApiServer {
    val server = ApiServer(port)
    server.configure(block)
    return server
}
