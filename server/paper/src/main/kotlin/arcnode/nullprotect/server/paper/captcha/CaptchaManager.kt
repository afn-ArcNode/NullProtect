/*
 *    Copyright 2024 ArcNode (AFterNode)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package arcnode.nullprotect.server.paper.captcha

import arcnode.nullprotect.server.paper.plugin
import arcnode.nullprotect.server.paper.utils.CaptchaConfiguration
import arcnode.nullprotect.server.paper.utils.runOnScheduler
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

class CaptchaManager: Listener {
    private val captcha = HashMap<UUID, StartedCaptcha>()
    private val lastSuccessCaptcha = HashMap<UUID, Long>()

    private val config: CaptchaConfiguration =
        (plugin.conf.getConfigurationSection("captcha") ?: throw NullPointerException("captcha @ config.yml")).let {
            CaptchaConfiguration(
                it.getInt("min-interval", 600) * 1000,
                it.getInt("timeout", 30) * 1000,
                it.getInt("auto.lumbering", 5),
                it.getInt("auto.mining", 5),
                it.getInt("auto.mining_deepslate", 5),
                it.getInt("auto.fishing", 5)
            )
        }

    @EventHandler   // Inventory click
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val who = event.whoClicked
        if (who !is Player)
            return
        val captcha = this.getCaptcha(who) ?: return

        if(captcha.captcha is IInventoryCaptcha) {
            captcha.captcha.click(event.slot, event.rawSlot)
            event.isCancelled = true
        }
    }

    @EventHandler   // No inventory close during captcha
    fun onPlayerInventoryClosed(event: InventoryCloseEvent) {
        val who = event.player
        if (who !is Player)
            return
        val captcha = this.getCaptcha(who) ?: return

        if (captcha.captcha is IInventoryCaptcha) {
            fail(who)
        }
    }

    @EventHandler   // No movement during captcha
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (isInCaptcha(event.player))
            event.isCancelled = true
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        this.lastSuccessCaptcha.remove(event.player.uniqueId)   // Reset last success
        this.captcha.remove(event.player.uniqueId)  // Remove current
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        this.lastSuccessCaptcha[event.player.uniqueId] = System.currentTimeMillis() // Don't trigger captcha immediately
    }

    @EventHandler   // Mining/Lumbering trigger
    fun onBlockBreak(event: BlockBreakEvent) {
        if (System.currentTimeMillis() - (this.lastSuccessCaptcha[event.player.uniqueId] ?: 0) < config.minInterval)
            return  // Interval

        val mat = event.block.type.name
        val rd = ThreadLocalRandom.current().nextInt(0, 100)
        if (
            (mat.endsWith("LOG") && rd < config.autoLumbering) ||  // Logs
            (mat.endsWith("_ORE") && rd < config.autoMining) ||     // Mining
            (mat.startsWith("DEEPSLATE") && rd < config.autoMiningDeepslate)    // Mining (deepslate
            ) {
            start(event.player)
        }
    }

    @EventHandler
    fun onFishing(event: PlayerFishEvent) { // Fishing trigger
        if (System.currentTimeMillis() - (this.lastSuccessCaptcha[event.player.uniqueId] ?: 0) < config.minInterval)
            return  // Interval

        if (ThreadLocalRandom.current().nextInt(0, 100) < config.autoFishing)
            start(event.player)
    }

    fun fail(player: Player) {
        if (isInCaptcha(player)) {
            player.runOnScheduler {
                val cap = this.captcha.remove(player.uniqueId) ?: return@runOnScheduler
                plugin.slF4JLogger.info("(${player.name}) Captcha failed")
                player.kick(Component.text("Captcha failed"))
                cap.onComplete?.invoke(false)
            }
        }
    }

    fun accept(player: Player) {
        if (isInCaptcha(player)) {
            val c = this.captcha.remove(player.uniqueId) ?: return
            c.onComplete?.invoke(true)
            if (c.captcha is IInventoryCaptcha)
                player.closeInventory()
            this.lastSuccessCaptcha[player.uniqueId] = System.currentTimeMillis()   // Record success
        }
    }

    fun isInCaptcha(player: Player) = this.captcha.containsKey(player.uniqueId)

    fun getCaptcha(player: Player) = this.captcha[player.uniqueId]

    fun start(player: Player, force: Boolean = false, onComplete: ((Boolean) -> Unit)? = null) {
        if (!isInCaptcha(player)) {
            player.runOnScheduler {
                val cap = CaptchaType.entries[ThreadLocalRandom.current().nextInt(CaptchaType.entries.size)].creator.invoke(player)
                val st = StartedCaptcha(cap, player.uniqueId, onComplete)
                this.captcha[player.uniqueId] = st
            }
        }
    }
}

data class StartedCaptcha(
    val captcha: ICaptcha,
    val player: UUID,
    val onComplete: ((Boolean) -> Unit)? = null
)

enum class CaptchaType(val creator: (Player) -> ICaptcha) {
    CHEST({ ChestInventoryCaptcha(it) }),
    FURNACE({ FurnaceInventoryCaptcha(it) }),
    BOOK({ BookCaptcha(it) })
}
