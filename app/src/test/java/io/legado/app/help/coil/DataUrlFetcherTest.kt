package io.legado.app.help.coil

import android.app.Application
import android.graphics.Bitmap
import android.util.Base64
import coil3.ImageLoader
import coil3.decode.BitmapFactoryDecoder
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DataUrlFetcherTest {
    @Test
    fun localFileAndEmbeddedImageStillDecodeWithoutNetworkExtension() = runBlocking {
        val context = RuntimeEnvironment.getApplication()
        val bytes = ByteArrayOutputStream().use { output ->
            val bitmap = Bitmap.createBitmap(2, 3, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.BLUE)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            bitmap.recycle()
            output.toByteArray()
        }
        val file = File(context.cacheDir, "local-cover-test.png").apply { writeBytes(bytes) }
        val loader = ImageLoader.Builder(context)
            .components {
                add(DataUrlFetcher.Factory())
                // Windows 上的 Robolectric 不支持 ImageDecoder，使用兼容的本地位图解码器。
                add(BitmapFactoryDecoder.Factory())
            }
            .build()
        try {
            val sources = listOf(file, "data:image/png;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP))
            for (source in sources) {
                val result = loader.execute(ImageRequest.Builder(context).data(source).size(2, 3).build())
                assertTrue("本地图片解码失败：$result", result is SuccessResult)
                val image = (result as SuccessResult).image
                assertEquals(2, image.width)
                assertEquals(3, image.height)
            }
        } finally {
            loader.shutdown()
            file.delete()
        }
    }
}
