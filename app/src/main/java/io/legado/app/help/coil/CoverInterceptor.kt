package io.legado.app.help.coil

import coil3.BitmapImage
import coil3.decode.DataSource
import coil3.intercept.Interceptor
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.ImageResult
import coil3.request.SuccessResult
import coil3.request.allowHardware
import coil3.request.crossfade
import coil3.request.maxBitmapSize
import coil3.request.transformations
import coil3.size.Size
import io.legado.app.data.appDb
import io.legado.app.help.book.BookHelp
import io.legado.app.utils.isAbsUrl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File

class CoverInterceptor(
    private val acquireMangaFile: suspend (ImageRequest, String) -> Pair<File, Closeable> = { request, data ->
        val bookUrl = requireNotNull(request.extras[CoverExtras.MangaBookUrl])
        val book = requireNotNull(appDb.bookDao.getBook(bookUrl)) { "Manga book missing" }
        val source = appDb.bookSourceDao.getBookSource(
            request.extras[CoverExtras.SourceOrigin] ?: book.origin,
        )
        BookHelp.acquireReadingImage(
            source, book, data, request.extras[CoverExtras.LoadOnlyWifi] == true,
            request.extras[CoverExtras.MangaFileTransferStarted] ?: {},
            request.extras[CoverExtras.MangaChapterSubject],
        )
    },
) : Interceptor {

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        // Telephoto 取消 Coil 的尺寸上限后，按宽度加载长图会生成无法绘制的整图。
        // 整图仅作预览；分页和条漫的高清细节都由原图区域解码补足。
        val incoming = chain.request
        val boundedChain = if (incoming.extras[CoverExtras.Manga] == true ||
            incoming.extras[CoverExtras.MangaFileOwner] != null
        ) {
            chain.withRequest(incoming.newBuilder().maxBitmapSize(Size(4096, 4096)).build())
        } else chain
        val request = boundedChain.request
        val data = request.data

        val fileOwner = request.extras[CoverExtras.MangaFileOwner]
        // 分页与条漫在线页复用同一原图；本地漫画与 content/file URI 保持平台加载器。
        if (fileOwner != null && data is String &&
            data.startsWith("data:", true) &&
            !data.startsWith("file:", true) && !data.startsWith("content:", true)
        ) {
            val (file, version, ratio) = withContext(Dispatchers.IO) {
                val acquired = fileOwner.acquire(data) { acquireMangaFile(request, data) }
                Triple(
                    acquired,
                    "${acquired.length()}:${acquired.lastModified()}",
                    MangaImageDimensions.read(acquired)
                )
            }
            fileOwner.setAspectRatio(ratio)
            ratio?.let { request.extras[CoverExtras.MangaAspectRatio]?.invoke(it) }
            val memoryKey = "${request.memoryCacheKey ?: data}:original:$version"
            val webtoon =
                request.extras[CoverExtras.MangaWebtoon] == true && request.extras[CoverExtras.MangaDataPrefetch] != true
            val previewKey = if (webtoon) MangaPreviewCache.key(memoryKey, request) else ""
            val snapshot =
                if (webtoon) withContext(Dispatchers.IO) { MangaPreviewCache.open(previewKey) } else null

            fun localRequest(dataFile: File, cachedPreview: Boolean) = request.newBuilder()
                .data(dataFile).memoryCacheKey(memoryKey)
                .placeholderMemoryCacheKey(memoryKey).diskCachePolicy(CachePolicy.DISABLED)
                .apply {
                    if (webtoon) allowHardware(false)
                    if (cachedPreview) transformations(emptyList())
                }.build()

            var usedPreview = snapshot != null
            var result = try {
                boundedChain.withRequest(
                    localRequest(
                        snapshot?.data?.toFile() ?: file,
                        usedPreview
                    )
                )
                    .proceed()
            } finally {
                if (snapshot != null) withContext(kotlinx.coroutines.NonCancellable + Dispatchers.IO) { snapshot.close() }
            }
            if (usedPreview && result is ErrorResult) {
                withContext(Dispatchers.IO) { MangaPreviewCache.remove(previewKey) }
                usedPreview = false
                result = boundedChain.withRequest(localRequest(file, false)).proceed()
            }
            if (webtoon && !usedPreview && result is SuccessResult && result.image is BitmapImage) {
                MangaPreviewCache.save(previewKey, (result.image as BitmapImage).bitmap)
            }
            // Telephoto 会对内存命中也重新淡入；已准备的预览直接显示，避免滑动时露出底色。
            return if (result is SuccessResult && result.dataSource == DataSource.MEMORY_CACHE) {
                result.copy(request = result.request.newBuilder().crossfade(false).build())
            } else result
        }

        if (data is String && data.isNotBlank()) {
            // 保留旧版已下载的封面缓存；未命中时交给本地文件或内嵌图片加载器。
            val isManga = request.extras[CoverExtras.Manga] == true
            val bookUrl = request.extras[CoverExtras.BookUrl]
            val preferCache = request.extras[CoverExtras.PreferCache] == true
            if (!isManga && !data.startsWith("data:", true)) {
                val exactFile = CoverFileCache.read(data)
                val cachedFile = exactFile
                    ?: bookUrl?.takeIf { preferCache }?.let { CoverFileCache.readByBookUrl(it) }
                cachedFile?.let { file ->
                    // 为已有封面缓存补齐书籍别名，保证离线重启后仍可读取。
                    if (exactFile != null && bookUrl != null) {
                        withContext(Dispatchers.IO) {
                            CoverFileCache.ensureAlias(bookUrl, file)
                        }
                    }
                    val localRequest = request.newBuilder()
                        .data(file)
                        .build()
                    return boundedChain.withRequest(localRequest).proceed()
                }
            }
        }
        return boundedChain.proceed()
    }
}
