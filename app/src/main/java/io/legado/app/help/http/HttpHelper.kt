package io.legado.app.help.http

import io.legado.app.data.entities.BaseSource
import io.legado.app.help.CacheManager
import io.legado.app.help.ConcurrentRateLimiter
import io.legado.app.help.glide.progress.ProgressManager.LISTENER
import io.legado.app.help.glide.progress.ProgressResponseBody
import io.legado.app.help.glide.progress.ProgressUrlTag
import io.legado.app.utils.NetworkUtils
import okhttp3.Cache
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Credentials
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import splitties.init.appCtx
import java.io.File
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap

private val proxyClientCache: ConcurrentHashMap<String, OkHttpClient> by lazy {
    ConcurrentHashMap()
}

private val cacheSettingsGateway get() = org.koin.core.context.GlobalContext.get().get<io.legado.app.domain.gateway.DownloadCacheSettingsGateway>()

val cookieJar by lazy {
    object : CookieJar {

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            return emptyList()
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            if (cookies.isEmpty()) return
            //临时保存 书源启用cookie选项再添加到数据库
            val cookieBuilder = StringBuilder()
            cookies.forEachIndexed { index, cookie ->
                if (index > 0) cookieBuilder.append(";")
                cookieBuilder.append(cookie.name).append('=').append(cookie.value)
            }
            val domain = NetworkUtils.getSubDomain(url.toString())
            CacheManager.putMemory("${domain}_cookieJar", cookieBuilder.toString())
        }

    }
}

// 本地版拒绝所有 HTTP 请求，旧备份中的远程地址也不能重新启用联网。
val okHttpClient: OkHttpClient by lazy {
    OkHttpClient.Builder()
        .addInterceptor { throw java.io.IOException("当前版本仅支持本地文件") }
        .build()
}

val okHttpClientManga by lazy {
    okHttpClient.newBuilder().run {
        cache(Cache(File(appCtx.cacheDir, "manga_cache"), 100L * 1024L * 1024L))
        val interceptors = interceptors()
        interceptors.add(1) { chain ->
            val request = chain.request()
            val response = chain.proceed(request)
            // 漫画正文地址会被书源规则改写，进度必须回报给“原始地址”才能与阅读页的
            // page.imageUrl 对上；普通封面请求没有 tag，退回最终 URL。
            val url = request.tag(ProgressUrlTag::class.java)?.url ?: request.url.toString()
            response.newBuilder()
                .body(ProgressResponseBody(url, LISTENER, response.body))
                .build()
        }
        interceptors.add(1) { chain ->
            ConcurrentRateLimiter(chain.request().tag(BaseSource::class.java)).withLimitBlocking {
                chain.proceed(chain.request())
            }
        }
        build()
    }
}

enum class HttpCacheType(val dirName: String, val maxSize: Long) {
    COVER("http_cache", 100L * 1024 * 1024),
    MANGA("manga_cache", 100L * 1024 * 1024),
}

fun getHttpCacheSize(type: HttpCacheType): Long {
    val dir = File(appCtx.cacheDir, type.dirName)
    if (!dir.exists()) return 0
    return dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
}

fun clearHttpCache(type: HttpCacheType) {
    when (type) {
        // 设置页“封面缓存”条目同时清掉持久化封面文件缓存（CoverFileCache，
        // 位于 filesDir/cover_cache，不在“清除缓存”目录扫描范围内），
        // 保证用户能彻底删除封面数据腾空间。
        HttpCacheType.COVER -> {
            okHttpClient.cache?.delete()
            io.legado.app.help.coil.CoverFileCache.clear()
        }
        HttpCacheType.MANGA -> okHttpClientManga.cache?.delete()
    }
}

/**
 * 缓存代理okHttp
 */
fun getProxyClient(proxy: String? = null): OkHttpClient {
    if (proxy.isNullOrBlank()) {
        return okHttpClient
    }
    proxyClientCache[proxy]?.let {
        return it
    }
    val r = Regex("(http|socks4|socks5)://(.*):(\\d{2,5})(@.*@.*)?")
    val ms = r.findAll(proxy)
    val group = ms.first()
    var username = ""       //代理服务器验证用户名
    var password = ""       //代理服务器验证密码
    val type = if (group.groupValues[1] == "http") "http" else "socks"
    val host = group.groupValues[2]
    val port = group.groupValues[3].toInt()
    if (group.groupValues[4] != "") {
        username = group.groupValues[4].split("@")[1]
        password = group.groupValues[4].split("@")[2]
    }
    if (host != "") {
        val builder = okHttpClient.newBuilder()
        if (type == "http") {
            builder.proxy(Proxy(Proxy.Type.HTTP, InetSocketAddress(host, port)))
        } else {
            builder.proxy(Proxy(Proxy.Type.SOCKS, InetSocketAddress(host, port)))
        }
        if (username != "" && password != "") {
            builder.proxyAuthenticator { _, response -> //设置代理服务器账号密码
                val credential: String = Credentials.basic(username, password)
                response.request.newBuilder()
                    .header("Proxy-Authorization", credential)
                    .build()
            }
        }
        val proxyClient = builder.build()
        proxyClientCache[proxy] = proxyClient
        return proxyClient
    }
    return okHttpClient
}
