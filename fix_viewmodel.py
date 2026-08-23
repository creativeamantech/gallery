path = 'app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

import re

# Find the end of the when block by searching for "is AgentEvent.LoopTerminated" and its block
# It might be easier to replace `is AgentEvent.LoopTerminated -> { ... }` with `... \n else -> {}`
# Let's just do a naive replace first
old_code = """          is AgentEvent.LoopTerminated -> {
            val lastMessage = getLastMessage(model = model)
            val wasLoading = lastMessage?.type == ChatMessageType.LOADING

            // Remove the last message if it is a "loading" message.
            if (wasLoading) {
              removeLastMessage(model = model)
            }
          }
        }"""
new_code = """          is AgentEvent.LoopTerminated -> {
            val lastMessage = getLastMessage(model = model)
            val wasLoading = lastMessage?.type == ChatMessageType.LOADING

            // Remove the last message if it is a "loading" message.
            if (wasLoading) {
              removeLastMessage(model = model)
            }
          }
          else -> {}
        }"""
content = content.replace(old_code, new_code)
with open(path, 'w') as f:
    f.write(content)

