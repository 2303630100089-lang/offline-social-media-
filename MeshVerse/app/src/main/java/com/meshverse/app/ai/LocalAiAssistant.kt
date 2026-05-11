package com.meshverse.app.ai

object LocalAiAssistant {

    enum class Action {
        SEND_SOS,
        FIND_HOSPITAL,
        START_WALKIE_TALKIE,
        OPEN_MARKETPLACE,
        UNKNOWN
    }

    data class CommandResult(
        val action: Action,
        val message: String = ""
    )

    fun routeCommand(prompt: String): CommandResult {
        val normalized = prompt.trim().lowercase()
        return when {
            normalized.contains("send sos") || normalized.startsWith("sos") -> CommandResult(Action.SEND_SOS, prompt)
            normalized.contains("hospital") -> CommandResult(Action.FIND_HOSPITAL)
            normalized.contains("walkie") || normalized.contains("push to talk") -> CommandResult(Action.START_WALKIE_TALKIE)
            normalized.contains("marketplace") -> CommandResult(Action.OPEN_MARKETPLACE)
            else -> CommandResult(Action.UNKNOWN)
        }
    }
}
