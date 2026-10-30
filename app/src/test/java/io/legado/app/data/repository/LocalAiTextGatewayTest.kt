package io.legado.app.data.repository

import io.legado.app.domain.model.AiGenerateRequest
import io.legado.app.domain.model.AiModelConfig
import io.legado.app.domain.model.AiProviderConfig
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalAiTextGatewayTest {
    @Test
    fun restoredProviderCannotActivateCloudRequests() = runBlocking {
        val gateway = LocalAiTextGateway()
        val provider = AiProviderConfig("old", "旧配置", "openai", "https://example.invalid", "")
        val request = AiGenerateRequest(AiModelConfig("old", provider, "旧模型", "old"), emptyList())
        assertTrue(gateway.generate(request).exceptionOrNull() is UnsupportedOperationException)
        assertTrue(gateway.fetchModels(provider).exceptionOrNull() is UnsupportedOperationException)
        assertTrue(runCatching { gateway.generateStream(request).toList() }.exceptionOrNull() is UnsupportedOperationException)
    }
}
