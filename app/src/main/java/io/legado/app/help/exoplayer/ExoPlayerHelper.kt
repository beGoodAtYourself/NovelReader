package io.legado.app.help.exoplayer

import android.annotation.SuppressLint
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.FileDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import io.legado.app.domain.model.LocalReadingPolicy

@SuppressLint("UnsafeOptInUsageError")
object ExoPlayerHelper {
    fun createMediaItem(url: String): MediaItem {
        require(LocalReadingPolicy.acceptsAddress(url)) { "本地版仅支持设备中的音频文件" }
        return MediaItem.fromUri(url)
    }

    /** 本地文件与系统内容提供器共用解码器，不注册 HTTP 数据源。 */
    fun createLocalExoPlayer(context: Context): ExoPlayer {
        val dataSource = ResolvingDataSource.Factory(
            DefaultDataSource.Factory(context, FileDataSource.Factory())
        ) { dataSpec ->
            require(LocalReadingPolicy.acceptsAddress(dataSpec.uri.toString())) {
                "本地版仅支持设备中的音频文件"
            }
            dataSpec
        }
        return ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder().setBufferDurationsMs(
                    DefaultLoadControl.DEFAULT_MIN_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_MAX_BUFFER_MS,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_MS / 10,
                    DefaultLoadControl.DEFAULT_BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS / 10,
                ).build()
            )
            .setMediaSourceFactory(DefaultMediaSourceFactory(context).setDataSourceFactory(dataSource))
            .build()
    }
}
