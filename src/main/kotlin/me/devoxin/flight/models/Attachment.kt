package me.devoxin.flight.models

import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStream

public class Attachment(val stream: InputStream, val filename: String) {
    companion object {
        public fun from(inputStream: InputStream, filename: String): Attachment {
            return Attachment(inputStream, filename)
        }

        public fun from(byteArray: ByteArray, filename: String): Attachment {
            return Attachment(ByteArrayInputStream(byteArray), filename)
        }

        public fun from(file: File, filename: String? = null): Attachment {
            val name = filename ?: file.name
            return Attachment(FileInputStream(file), name)
        }
    }
}