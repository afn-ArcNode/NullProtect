package arcnode.nullprotect.server.paper.network

import arcnode.nullprotect.network.PacketIO
import arcnode.nullprotect.server.paper.hwidChannelReq
import arcnode.nullprotect.server.paper.hwidChannelReqStr
import arcnode.nullprotect.server.paper.hwidChannelRespStr
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
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.plugin.messaging.PluginMessageListener
import java.util.*

class NetworkManager: Listener, PluginMessageListener {
    private val rtHwid = mutableMapOf<UUID, String>()
    private val dummyPacket by lazy { PacketIO.dummy() }

    operator fun get(id: UUID): String? = rtHwid[id]

    fun runHwidCheck(tk: ScheduledTask) {
        val now = System.currentTimeMillis()

        for (player in Bukkit.getOnlinePlayers()) {
            if (!rtHwid.containsKey(player.uniqueId) && now - player.lastLogin > plugin.hwidCheckTimeout) {   // Timed out
                player.scheduler    // Folia compatibility
                    .run(plugin, { player.kick(Component.text("Verification timed out")) }, {})
                plugin.slF4JLogger.info("${player.name}: HWID verification timed out")
            }
        }
    }

    @EventHandler
    fun onPlayerJoin(e: PlayerJoinEvent) {  // Send request
        if (plugin.hwidEnabled) {
            PacketEvents.getAPI().playerManager.sendPacket(e.player, WrapperPlayServerPluginMessage(
                hwidChannelReq,
                dummyPacket
            ))
        }
    }

    @EventHandler
    fun onPlayerQuit(e: PlayerQuitEvent) {  // Remove on exit
        rtHwid.remove(e.player.uniqueId)
    }

    override fun onPluginMessageReceived(channel: String, player: Player, message: ByteArray) {
        if (channel == hwidChannelRespStr) {    // HWID packet
            if (plugin.hwidEnabled)
                this.handleHwid(player, message)
        }
    }

    private fun handleHwid(player: Player, message: ByteArray) {
        if (rtHwid.containsKey(player.uniqueId)) {  // Duplicate
            plugin.slF4JLogger.warn("Duplicate HWID packet received from ${player.name}")
            player.scheduler.run(plugin, {
                player.kick(Component.text("Invalid packet received")) }, {})
        } else {
            val dec = PacketIO.decode(message)
            if (dec.value.length == 32) {   // Valid
                this.rtHwid[player.uniqueId] = dec.value
                plugin.slF4JLogger.info("HWID of ${player.name} is ${dec.value}")

                // Async database operation
                if (plugin.hwidMatchMode != 0) {
                    plugin.runBlockingCoroutine {
                        val exists = plugin.database.whiteOrBlackList.exists(dec.value)
                        if (plugin.hwidMatchMode == 1 && !exists) { // Whitelist
                            player.scheduler.run(plugin, {
                                player.kick(Component.text("You are not whitelisted on this server!"))
                            }, {})
                        } else if (plugin.hwidMatchMode == 2 && exists) {   // Blacklist
                            player.scheduler.run(plugin, {
                                player.kick(Component.text("You are blacklisted on this server!"))
                            }, {})
                            Bukkit.getGlobalRegionScheduler().run(plugin) {
                                val exec = Bukkit.getConsoleSender()
                                for (cmd in plugin.hwidOnBlackListOp) {
                                    Bukkit.dispatchCommand(exec, cmd.replace("%player%", player.name))
                                }
                            }
                        }
                    }
                }
            } else {    // Large HWID
                plugin.slF4JLogger.warn("Invalid HWID packet received from ${player.name} (length: ${dec.value})")
                player.scheduler.run(plugin, {
                    player.kick(Component.text("Invalid packet received")) }, {})
            }
        }
    }
}