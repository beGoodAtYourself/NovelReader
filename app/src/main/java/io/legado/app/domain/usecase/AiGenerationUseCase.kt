package io.legado.app.domain.usecase

import io.legado.app.domain.gateway.AiStreamEvent
import io.legado.app.domain.gateway.AiTextGateway
import io.legado.app.domain.model.AiGenerateRequest
import kotlinx.coroutines.flow.Flow

/** 旧阅读状态仍可能持有 AI 操作；由本地能力边界统一返回不支持。 */
class AiGenerationUseCase(private val aiTextGateway: AiTextGateway) {
    suspend fun generate(request: AiGenerateRequest): String {
        val output = StringBuilder()
        generateStream(request).collect { event ->
            if (event is AiStreamEvent.Content) {
                output.append(event.text)
            }
        }
        return output.toString().trim()
            .ifEmpty { error("AI returned empty text") }
    }

    fun generateStream(request: AiGenerateRequest): Flow<AiStreamEvent> =
        aiTextGateway.generateStream(request)

    companion object {
        const val CACHE_PROMPT_VERSION = "tool-aware-generation-v1"
    }
}
