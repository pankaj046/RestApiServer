
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


fun <T> readJsonFromInputStream(inputStream: InputStream, clazz: Class<T>): T {
    val jsonBuilder = StringBuilder()
    BufferedReader(InputStreamReader(inputStream)).use { reader ->
        var line: String?
        while ((reader.readLine().also { line = it }) != null) {
            jsonBuilder.append(line)
        }
    }
    val json = jsonBuilder.toString()
    try {
        return fromJson(json, clazz)
    } catch (e: Exception) {
        throw IOException("Failed to deserialize JSON", e)
    }
}


val mapper = ObjectMapper().registerModule(JavaTimeModule())

fun <T> fromJson(json: String?, clazz: Class<T>?): T {
    return mapper.readValue(json, clazz)
}

fun <T> fromJson(json: String?, typeReference: TypeReference<T>?): T {
    return mapper.readValue(json, typeReference)
}


fun handleRequest(requestBody: InputStream) : HashMap<String, Any>?{
    val req = HashMap<String, Any>()
   return try {
        val formData: Map<String, String> = parseFormData(requestBody)
        for ((key, value) in formData) {
            req[key] = value
        }
       req
    } catch (e: IOException) {
        return null
    }
}


fun parseFormData(inputStream: InputStream): Map<String, String> {
    val formData: MutableMap<String, String> = HashMap()
    BufferedReader(InputStreamReader(inputStream)).use { reader ->
        var line: String
        while ((reader.readLine().also { line = it }) != null) {
            val parts =
                line.split("=".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            if (parts.size == 2) {
                val key = parts[0]
                val value = parts[1]
                formData[key] = value
            }
        }
    }
    return formData
}