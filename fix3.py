import re

path1 = 'app/src/main/java/com/google/ai/edge/gallery/agent/UniversalAgentRuntimeExecutor.kt'
with open(path1, 'r') as f:
    c1 = f.read()

old_block = """                        when (dispatchResult) {
                            is CapabilityResult.Success -> {
                                emit(AgentEvent.CapabilityCompleted(toolCall.name, dispatchResult.data))
                                dispatchResult.data.toStructuredData()
                            }
                            is CapabilityResult.Error -> {
                                emit(AgentEvent.CapabilityFailed(toolCall.name, dispatchResult.message))
                                state.retryCount++
                                if (state.retryCount > maxRetries) {
                                    emit(AgentEvent.Error("Maximum capability retries ($maxRetries) reached."))
                                    state.status = AgentStatus.FAILED
                                }
                                mapOf("error" to dispatchResult.message).toStructuredData()
                            }
                        }"""
new_block = """                        when (dispatchResult) {
                            is CapabilityResult.Success -> {
                                emit(AgentEvent.CapabilityCompleted(toolCall.name, dispatchResult.data))
                                dispatchResult.data.toStructuredData()
                            }
                            is CapabilityResult.Error -> {
                                emit(AgentEvent.CapabilityFailed(toolCall.name, dispatchResult.message))
                                state.retryCount++
                                if (state.retryCount > maxRetries) {
                                    emit(AgentEvent.Error("Maximum capability retries ($maxRetries) reached."))
                                    state.status = AgentStatus.FAILED
                                }
                                mapOf("error" to dispatchResult.message).toStructuredData()
                            }
                            is CapabilityResult.PermissionRequired -> {
                                emit(AgentEvent.CapabilityFailed(toolCall.name, "Permission required"))
                                mapOf("error" to "Permission required", "status" to "permission_required").toStructuredData()
                            }
                            is CapabilityResult.UserActionRequired -> {
                                emit(AgentEvent.CapabilityFailed(toolCall.name, "User action required"))
                                mapOf("error" to "User action required", "status" to "user_action_required").toStructuredData()
                            }
                        }"""
c1 = c1.replace(old_block, new_block)
with open(path1, 'w') as f:
    f.write(c1)
