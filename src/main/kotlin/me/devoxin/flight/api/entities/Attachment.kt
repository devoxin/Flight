package me.devoxin.flight.api.entities

import net.dv8tion.jda.api.utils.FileUpload
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

@Deprecated("Please use JDA's FileUpload class")
class Attachment(stream: InputStream, filename: String) : FileUpload(stream, filename) {
    companion object {
        fun from(inputStream: InputStream, filename: String): FileUpload {
            return fromData(inputStream, filename)
        }

        fun from(byteArray: ByteArray, filename: String): FileUpload {
            return fromData(ByteArrayInputStream(byteArray), filename)
        }

        fun from(file: File, filename: String? = null): FileUpload {
            val name = filename ?: file.name
            return fromData(FileInputStream(file), name)
        }
    }
}
