package io.legado.app.data.repository

import io.legado.app.domain.gateway.AiStreamEvent
import io.legado.app.domain.gateway.AiTextGateway
import io.legado.app.domain.model.AiAvailableModel
import io.legado.app.domain.model.AiGenerateRequest
import io.legado.app.domain.model.AiGenerateResponse
import io.legado.app.domain.model.AiProviderConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/** 兼容阅读流程的既有依赖；旧状态请求云端能力时明确失败，不创建网络客户端。 */
class LocalAiTextGateway : AiTextGateway {
    private fun unavailable() = UnsupportedOperationException("本地版不支持云端 AI 功能")

    override suspend fun generate(request: AiGenerateRequest): Result<AiGenerateResponse> =
        Result.failure(unavailable())

    override fun generateStream(request: AiGenerateRequest): Flow<AiStreamEvent> = flow {
        throw unavailable()
    }

    override suspend fun fetchModels(provider: AiProviderConfig): Result<List<AiAvailableModel>> =
        Result.failure(unavailable())
}
