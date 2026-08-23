import re

path1 = 'app/src/main/java/com/google/ai/edge/gallery/agent/UniversalAgentRuntimeExecutor.kt'
with open(path1, 'r') as f:
    c1 = f.read()

# Replace the specific old when block with the new one
old_block1 = """                    when (result) {
                        is CapabilityResult.Success -> {
                            val structuredData = StructuredData(
                                StructuredData.Type.OBJECT,
                                children = result.data.mapValues { (_, v) -> v.toStructuredData() }
                            )
                            AgentState.ModelContent.ToolResult(toolName, callId, structuredData)
                        }
                        is CapabilityResult.Error -> {
                            val errorData = StructuredData(
                                StructuredData.Type.OBJECT,
                                children = mapOf("error" to result.message.toStructuredData())
                            )
                            AgentState.ModelContent.ToolResult(toolName, callId, errorData)
                        }
                    }"""
new_block1 = """                    when (result) {
                        is CapabilityResult.Success -> {
                            val structuredData = StructuredData(
                                StructuredData.Type.OBJECT,
                                children = result.data.mapValues { (_, v) -> v.toStructuredData() }
                            )
                            AgentState.ModelContent.ToolResult(toolName, callId, structuredData)
                        }
                        is CapabilityResult.Error -> {
                            val errorData = StructuredData(
                                StructuredData.Type.OBJECT,
                                children = mapOf("error" to result.message.toStructuredData())
                            )
                            AgentState.ModelContent.ToolResult(toolName, callId, errorData)
                        }
                        is CapabilityResult.PermissionRequired -> {
                            val msg = "Permission required: ${result.permissions.joinToString()}"
                            val errorData = StructuredData(
                                StructuredData.Type.OBJECT,
                                children = mapOf("error" to msg.toStructuredData(), "status" to "permission_required".toStructuredData())
                            )
                            AgentState.ModelContent.ToolResult(toolName, callId, errorData)
                        }
                        is CapabilityResult.UserActionRequired -> {
                            val msg = "User action required: ${result.actionType}"
                            val errorData = StructuredData(
                                StructuredData.Type.OBJECT,
                                children = mapOf("error" to msg.toStructuredData(), "status" to "user_action_required".toStructuredData())
                            )
                            AgentState.ModelContent.ToolResult(toolName, callId, errorData)
                        }
                    }"""

c1 = c1.replace(old_block1, new_block1)
with open(path1, 'w') as f:
    f.write(c1)

path2 = 'app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatViewModel.kt'
with open(path2, 'r') as f:
    c2 = f.read()

old_block2 = """          is AgentEvent.LoopTerminated -> {
            val lastMessage = getLastMessage(model = model)
            val wasLoading = lastMessage?.type == ChatMessageType.LOADING

            // Remove the last message if it is a "loading" message.
            // This will only be done once.
            if (wasLoading) {
              removeLastMessage(model = model)
            }
          }
        }"""
new_block2 = """          is AgentEvent.LoopTerminated -> {
            val lastMessage = getLastMessage(model = model)
            val wasLoading = lastMessage?.type == ChatMessageType.LOADING

            // Remove the last message if it is a "loading" message.
            // This will only be done once.
            if (wasLoading) {
              removeLastMessage(model = model)
            }
          }
          else -> {}
        }"""
c2 = c2.replace(old_block2, new_block2)
with open(path2, 'w') as f:
    f.write(c2)
