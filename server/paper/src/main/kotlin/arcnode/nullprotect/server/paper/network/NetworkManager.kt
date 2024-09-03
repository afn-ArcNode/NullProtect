package arcnode.nullprotect.server.paper.network

import arcnode.nullprotect.network.PacketIO
import arcnode.nullprotect.server.paper.NullProtectPaper
import arcnode.nullprotect.server.paper.hwidChannel
import arcnode.nullprotect.server.paper.hwidChannelStr
import arcnode.nullprotect.server.paper.plugin
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*

class NetworkManager: Listener, PluginMessageListener {
    private val rtHwid = mutableMapOf<UUID, String>()
    private val dummyPacket by lazy { PacketIO.dummy() }

    operator fun get(id: UUID): String? = rtHwid[id]

    fun runHwidCheck(tk: ScheduledTask) {
        val now = System.currentTimeMillis()

        for (player in Bukkit.getOnlinePlayers()) {
            if (!rtHwid.containsKey(player.uniqueId) && now - player.lastLogin > 10000) {   // Timed out
                player.scheduler    // Folia compatibility
                    .run(plugin, { player.kick(Component.text("Verification timed out")) }, {})
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {
        PacketEvents.getAPI().playerManager.sendPacket(e.player, WrapperPlayServerPluginMessage(
            hwidChannel,
            dummyPacket
        ))
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel == hwidChannelStr) {    // HWID packet
            if (rtHwid.containsKey(player.uniqueId)) {  // Invalid

            } else {
                val dec = PacketIO.decode(message)
                this.rtHwid[player.uniqueId] = dec.value
                plugin.slF4JLogger.info("HWID of ${player.name} is ${dec.value}")
            }
        }
    }
}