path = 'app/src/main/java/com/google/ai/edge/gallery/agent/UniversalAgentRuntimeExecutor.kt'
with open(path, 'r') as f:
    content = f.read()

old_when = """                    when (result) {
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
new_when = """                    when (result) {
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

content = content.replace(old_when, new_when)
with open(path, 'w') as f:
    f.write(content)
