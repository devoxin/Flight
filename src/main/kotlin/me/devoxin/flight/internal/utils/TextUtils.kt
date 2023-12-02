package me.devoxin.flight.internal.utils

object TextUtils {
    fun split(content: String, limit: Int = 2000): List<String> {
        if (content.isEmpty()) {
            return emptyList()
        } else if (content.length < limit) {
            return listOf(content)
        }

        val pages = mutableListOf<String>()

        val lines = content.trim().split("\n").dropLastWhile { it.isEmpty() }
        val chunk = StringBuilder(limit)

        for (line in lines) {
            if (chunk.isNotEmpty() && chunk.length + line.length > limit) {
                pages.add(chunk.toString())
                chunk.setLength(0)
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

    fun plural(num: Number): String = if (num == 1) "" else "s"

    fun truncate(s: String, maxLength: Int) = s.takeIf { it.length <= maxLength } ?: (s.take(maxLength - 3) + "...")

    fun toTitleCase(s: String) = s.split(" +".toRegex()).joinToString(" ", transform = ::capitalise)
}
