package arcnode.nullprotect.server.paper.listeners

import arcnode.nullprotect.server.paper.plugin
import arcnode.nullprotect.server.paper.utils.runOnScheduler
import io.papermc.paper.event.player.AsyncChatEvent
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.Cancellable
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerInteractAtEntityEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID

object AccountActivationListener: Listener {
    private val act = HashMap<UUID, Boolean>()

    val conf by lazy { plugin.activationConfig }

    fun runActCheck(tk: ScheduledTask) {
        val now = System.currentTimeMillis()
        for (player in Bukkit.getOnlinePlayers()) {
            if (act[player.uniqueId] != true && now - player.lastLogin >= conf.timout) {
                player.runOnScheduler { player.kick(Component.text("Account activation timed out")) }
            }
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.database.accountActivation.findAsync(plugin.executor, event.player.uniqueId).thenAccept {   // Not loaded
            act[event.player.uniqueId] = it != null
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onPlayerQuit(event: PlayerQuitEvent) {
        this.act.remove(event.player.uniqueId)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (this.conf.blockingMove) {
            check(event.player, event)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerChat(event: AsyncChatEvent) {
        if (this.conf.blockingChat) {
            check(event.player, event)
        }
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEvent) {
        if (this.conf.blockingInteract)
            this.check(event.player, event)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractAtEntityEvent) {
        if (this.conf.blockingInteract)
            this.check(event.player, event)
    }

    @EventHandler(priority = EventPriority.LOWEST, ignoreCancelled = true)
    fun onPlayerInteract(event: PlayerInteractEntityEvent) {
        if (this.conf.blockingInteract)
            this.check(event.player, event)
    }

    private fun check(player: Player, event: Cancellable) {
        if (act[player.uniqueId] == false) {  // Already loaded and not activated
            event.isCancelled = true
        } else {
            act[player.uniqueId] = false    // Prevent duplicate call
            plugin.database.accountActivation.findAsync(plugin.executor, player.uniqueId).thenAccept {   // Not loaded
                act[player.uniqueId] = it != null
            }
        }
    }

    fun onActivated(id: UUID) {
        this.act[id] = true
    }
}