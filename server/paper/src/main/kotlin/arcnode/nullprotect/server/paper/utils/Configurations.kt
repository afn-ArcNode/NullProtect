package arcnode.nullprotect.server.paper.utils

data class ActivationConfiguration(
    val enabled: Boolean,
    val timout: Long,
    val blockingChat: Boolean,
    val blockingMove: Boolean,
    val blockingInteract: Boolean
)
