package arcnode.nullprotect.server.paper.listeners

import arcnode.nullprotect.server.paper.*
import arcnode.nullprotect.server.paper.utils.openChannel
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent

object GameEventListener: Listener {
    private val hwid by lazy { plugin.hwidConfiguration.enabled }
    private val mods by lazy { plugin.modsConfiguration.enabled }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if (hwid)
            player.openChannel(hwidChannelRespStr, hwidChannelReqStr)
        if (mods)
            player.openChannel(modsChannelRespStr, modsChannelReqStr)
    }
}
