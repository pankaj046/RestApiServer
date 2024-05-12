import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.core.type.TypeReference
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.io.IOException

object RequestHandler {

    private val objectMapper = ObjectMapper().registerModule(JavaTimeModule())

    fun <T> readJsonFromInputStream(inputStream: InputStream, clazz: Class<T>): T? {
        val jsonBuilder = StringBuilder()
        BufferedReader(InputStreamReader(inputStream)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                jsonBuilder.append(line)
            }
        }
        val json = jsonBuilder.toString()
        return fromJson(json, clazz)
    }

    private fun <T> fromJson(json: String?, clazz: Class<T>?): T {
        return objectMapper.readValue(json, clazz)
    }

    private fun <T> fromJson(json: String?, typeReference: TypeReference<T>?): T {
        return objectMapper.readValue(json, typeReference)
    }

    fun handleRequest(requestBody: InputStream): HashMap<String, Any>? {
        val formData: HashMap<String, Any> = HashMap()
        BufferedReader(InputStreamReader(requestBody)).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                val parts = line?.split("=".toRegex())?.dropLastWhile { it.isEmpty() }?.toTypedArray()?: arrayOf()
                if (parts.isEmpty()){
                    return null
                }
                if (parts.size == 2) {
                    val key = parts[0]
                    val value = parts[1]
                    formData[key] = value
                }
            }
        }
        return formData
    }
}
