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
import cn.afternode.commons.bukkit.kotlin.message
import cn.afternode.commons.bukkit.kotlin.pushStyle
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.inventory.InventoryType
import java.awt.Color
import java.util.concurrent.ThreadLocalRandom

// https://wiki.vg/Inventory#Furnace
private const val SLOT_INPUT = 0
private const val SLOT_FUEL = 1
private const val SLOT_OUTPUT = 2

class FurnaceInventoryCaptcha(override val player: Player): IInventoryCaptcha {
    private val targetSlot: Int = ThreadLocalRandom.current().nextInt(SLOT_INPUT, SLOT_OUTPUT)

    init {
        player.openInventory(Bukkit.createInventory(player, InventoryType.FURNACE, message(styleStack = true) {
            pushStyle {
                bold(true)
                color(Color.red)
            }
            text("CAPTCHA", Color.red)
            popStyle()
            text(" | ", Color.gray)

            text("Click ", Color.red)
            text(when (this@FurnaceInventoryCaptcha.targetSlot) {
                SLOT_INPUT -> "INPUT"
                SLOT_FUEL -> "FUEL"
                SLOT_OUTPUT -> "OUTPUT"
                else -> "(ERROR)"
            }, Color.green)
            text(" to complete", Color.red)
        }))
    }

    override fun click(slot: Int, rawSlot: Int) {
        if (slot < 0)
            return

        if (rawSlot == this.targetSlot) {
            plugin.captcha.accept(player)
        } else plugin.captcha.fail(player)
    }
}