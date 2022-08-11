package me.devoxin.flight.internal.utils

object TextUtils {
    fun split(content: String, limit: Int = 2000): List<String> {
        val pages = mutableListOf<String>()

        val lines = content.trim().split("\n").dropLastWhile { it.isEmpty() }
        var chunk = StringBuilder()

        for (line in lines) {
            if (chunk.isNotEmpty() && chunk.length + line.length > limit) {
                pages.add(chunk.toString())
                chunk = StringBuilder()
            }

            if (line.length > limit) {
                val lineChunks = line.length / limit

                for (i in 0 until lineChunks) {
                    val start = limit * i
                    val end = start + limit
                    pages.add(line.substring(start, end))
                }
            } else {
                chunk.append(line).append("\n")
            }
        }

        if (chunk.isNotEmpty()) {
            pages.add(chunk.toString())
        }

        return pages.toList()
    }

    fun capitalise(s: String): String = s.lowercase().replaceFirstChar { it.uppercase() }
}
