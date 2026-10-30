package io.legado.app.help.http

import okhttp3.Request
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.IOException

class LocalHttpClientTest {
    @Test
    fun legacyHttpRequestsFailBeforeDnsOrNetworkAccess() {
        val client = okHttpClient.newBuilder()
            .dns { throw AssertionError("本地版不应解析网络地址") }
            .build()
        val request = Request.Builder().url("https://offline-reader.invalid/book").build()
        val error = assertThrows(IOException::class.java) {
            client.newCall(request).execute().close()
        }
        assertEquals("当前版本仅支持本地文件", error.message)
    }
}
