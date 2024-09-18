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

import arcnode.nullprotect.server.paper.commands.MSG_CLICK_TO_COMPLETE_CAPTCHA
import arcnode.nullprotect.server.paper.plugin
import cn.afternode.commons.bukkit.kotlin.message
import cn.afternode.commons.bukkit.kotlin.pushStyle
import io.netty.util.internal.ThreadLocalRandom
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.TextColor
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.Inventory
import org.bukkit.inventory.InventoryView
import org.bukkit.inventory.ItemStack
import java.awt.Color

private val items by lazy { Material.entries
    .filterNot { it.isAir || it.isBlock }}

class ChestInventoryCaptcha(override val player: Player): IInventoryCaptcha {
    val inv: Inventory
    val target: Material
    val targetAt: Int
    val view: InventoryView

    init {
        val pool = ArrayList(items)
        val rd = ThreadLocalRandom.current()
        this.target = pool.removeAt(rd.nextInt(10))
        this.inv = Bukkit.createInventory(player, 27, message(styleStack = true) {
            pushStyle {
                bold(true)
                color(Color.red)
            }
            text("CAPTCHA", Color.red)
            popStyle()
            text(" | ", Color.gray)

            text("Click ", Color.red)
            append(Component.translatable("item.minecraft.${target.name.lowercase()}").color(TextColor.color(Color.green.rgb)))
            text(" to complete", Color.red)
        })
        this.targetAt = rd.nextInt(inv.size)

        // Generate
        for (i in 0 until this.inv.size) {
            if (i == targetAt) {    // Target item
                val t = ItemStack(this.target)
                t.editMeta {
                    it.lore(listOf(MSG_CLICK_TO_COMPLETE_CAPTCHA))
                }
                this.inv.setItem(i, t)
            } else {    // Trap item
                this.inv.setItem(i, ItemStack(pool.removeAt(rd.nextInt(pool.size))))
            }
        }

        this.view = this.player.openInventory(inv)!!
    }

    override fun click(slot: Int, rawSlot: Int) {
        if (slot < 0)   // Prevent click out of inventory
            return

        if (slot == this.targetAt) {
            plugin.captcha.accept(this.player)
        } else plugin.captcha.fail(this.player)
    }
}