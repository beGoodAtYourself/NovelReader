package io.legado.app.help.coil

import android.util.Base64
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.decode.ImageSource
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import okio.Buffer
import java.io.IOException

/** 只解码书籍内嵌图片；文件与 content URI 由 Coil 的本地加载器处理。 */
class DataUrlFetcher(private val data: String, private val options: Options) : Fetcher {
    override suspend fun fetch(): FetchResult {
        val encoded = data.substringAfter("base64,", "")
        if (encoded.isEmpty()) throw IOException("内嵌图片数据无效")
        val bytes = Base64.decode(encoded, Base64.DEFAULT)
        return SourceFetchResult(
            source = ImageSource(Buffer().write(bytes), options.fileSystem),
            mimeType = null,
            dataSource = DataSource.MEMORY,
        )
    }

    class Factory : Fetcher.Factory<coil3.Uri> {
        override fun create(data: coil3.Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (data.scheme == "data") DataUrlFetcher(data.toString(), options) else null
    }
}
