package io.legado.app.help.readaloud.cast

import android.content.Context
import io.legado.app.R
import io.legado.app.domain.gateway.ReadAloudSettingsGateway
import io.legado.app.domain.gateway.ReadAloudVoiceGateway
import io.legado.app.domain.model.readaloud.ReadAloudVoice
import io.legado.app.domain.model.readaloud.SystemTtsVoiceConfig
import io.legado.app.help.readaloud.playback.SystemTtsFileSynthesizer
import io.legado.app.ui.config.readConfig.ReadConfig
import io.legado.app.utils.GSON
import org.koin.core.context.GlobalContext
import java.io.File

/** 使用系统语音引擎试听本地音色，不执行云端或 HTTP 合成。 */
object VoiceAudition {

    suspend fun synthesize(context: Context, voiceId: String, text: String): File? {
        if (voiceId.isBlank() || text.isBlank()) return null
        val koin = GlobalContext.get()
        val voice = koin.get<ReadAloudVoiceGateway>().getVoice(voiceId) ?: return null
        val output = File(File(context.cacheDir, "voice_audition"), "${voice.id.hashCode()}.audio")
        val success = when (voice.engineType) {
            ReadAloudVoice.ENGINE_SYSTEM -> {
                val config = runCatching {
                    GSON.fromJson(voice.traitsJson, SystemTtsVoiceConfig::class.java)
                }.getOrNull() ?: SystemTtsVoiceConfig()
                SystemTtsFileSynthesizer(context).synthesize(
                    engine = voice.engineId,
                    voiceName = voice.speakerId,
                    text = text,
                    output = output,
                    speechRate = config.speechRate ?: globalSpeechRate(),
                    pitch = config.pitch ?: 1f,
                )
            }

            else -> false
        }
        return output.takeIf { success && it.length() > 0 }
    }

    /** 与 TTSReadAloudService.applyPreset 同源的全局语速，音色自身没配语速时用它。 */
    private fun globalSpeechRate(): Float = when {
        ReadConfig.ttsFollowSys -> 1f
        else -> (ReadConfig.ttsSpeechRate + 5) / 10f
    }

    /** 通用试听文本：用户在角色声音池里自定义过就用他的，否则用内置示例句。 */
    fun defaultPreviewText(context: Context): String =
        GlobalContext.get().get<ReadAloudSettingsGateway>().currentSettings.voicePreviewText
            ?.takeIf { it.isNotBlank() }
            ?: context.getString(R.string.cloud_tts_voice_preview_text)
}
