package me.devoxin.flight.api.entities

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

class Attachment(val stream: InputStream, val filename: String) {
    companion object {
        fun from(inputStream: InputStream, filename: String): Attachment {
            return Attachment(inputStream, filename)
        }

        fun from(byteArray: ByteArray, filename: String): Attachment {
            return Attachment(ByteArrayInputStream(byteArray), filename)
        }

        fun from(file: File, filename: String? = null): Attachment {
            val name = filename ?: file.name
            return Attachment(FileInputStream(file), name)
        }
    }
}
