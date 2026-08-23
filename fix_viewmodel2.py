path = 'app/src/main/java/com/google/ai/edge/gallery/ui/llmchat/LlmChatViewModel.kt'
with open(path, 'r') as f:
    content = f.read()

old_block = """          is AgentEvent.LoopCancelled -> {
            setInProgress(false)
            setPreparing(false)
          }
        }"""
new_block = """          is AgentEvent.LoopCancelled -> {
            setInProgress(false)
            setPreparing(false)
          }
          else -> {}
        }"""
content = content.replace(old_block, new_block)

with open(path, 'w') as f:
    f.write(content)
