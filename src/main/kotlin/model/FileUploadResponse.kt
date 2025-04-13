package model

data class FileUploadResponse(
    val message: String,
    val fileName: String?,
    val contentType: String?,
    val name: String?
) 