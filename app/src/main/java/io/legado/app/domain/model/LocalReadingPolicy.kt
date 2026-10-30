package io.legado.app.domain.model

import io.legado.app.constant.BookType

/** 本地标记和文件地址必须同时满足，避免旧备份中的网络书籍重新进入阅读流程。 */
object LocalReadingPolicy {
    private val schemePattern = Regex("^([A-Za-z][A-Za-z0-9+.-]*):")

    fun acceptsBook(type: Int, address: String): Boolean =
        type and BookType.local != 0 && acceptsAddress(address)

    fun acceptsAddress(address: String): Boolean {
        if (address.isBlank() || address.first().isWhitespace() || address.startsWith("//")) return false
        val scheme = schemePattern.find(address)?.groupValues?.get(1)?.lowercase()
        return scheme == null || scheme == "file" || scheme == "content"
    }
}
