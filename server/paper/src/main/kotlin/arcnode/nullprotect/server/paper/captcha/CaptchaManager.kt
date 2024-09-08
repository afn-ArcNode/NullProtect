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
import arcnode.nullprotect.server.paper.utils.runOnScheduler
import net.kyori.adventure.text.Component
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom

class CaptchaManager: Listener {
    private val captcha = HashMap<UUID, StartedCaptcha>()

    @EventHandler
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

    @EventHandler
    fun onPlayerInventoryClosed(event: InventoryCloseEvent) {
        val who = event.player
        if (who !is Player)
            return
        val captcha = this.getCaptcha(who) ?: return

        if (captcha.captcha is IInventoryCaptcha) {
            fail(who)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        fail(event.player)
    }

    fun fail(player: Player) {
        if (isInCaptcha(player)) {
            player.runOnScheduler {
                plugin.slF4JLogger.info("(${player.name}) Captcha failed")
                player.kick(Component.text("Captcha failed"))
                getCaptcha(player)?.onComplete?.invoke(false)
                this.captcha.remove(player.uniqueId)
            }
        }
    }

    fun accept(player: Player) {
        if (isInCaptcha(player)) {
            val c = getCaptcha(player) ?: return
            this.captcha.remove(player.uniqueId)
            c.onComplete?.invoke(true)
            if (c.captcha is IInventoryCaptcha)
                player.closeInventory()
        }
    }

    fun isInCaptcha(player: Player) = this.captcha.containsKey(player.uniqueId)

    fun getCaptcha(player: Player) = this.captcha[player.uniqueId]

    fun start(player: Player, onComplete: ((Boolean) -> Unit)? = null) {
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
