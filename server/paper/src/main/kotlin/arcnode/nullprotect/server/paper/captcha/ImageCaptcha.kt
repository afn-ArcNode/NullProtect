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
import cn.afternode.commons.bukkit.kotlin.sendMessage
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.MapMeta
import java.awt.Color

class ImageCaptcha(override val player: Player): IInteractCaptcha {
    private val types = mutableListOf<String>()
    private val target: String

    private val originalItems = mutableMapOf<Int, ItemStack?>()

    init {
        val items = mutableListOf<ItemStack>()

        // Select types from pool
        val pool = plugin.captcha.images
        val keys = pool.keys
        repeat(9) {
            val k = keys.random()
            this.types.add(k)

            // Create items
            val item = ItemStack(Material.FILLED_MAP)
            val meta = item.itemMeta as MapMeta

            meta.mapView = pool[k]!!.random()
            item.itemMeta = meta
            items.add(item)
        }
        this.target = this.types.random()

        // Apply items to hotbar
        val inv = player.inventory
        for (i in 0 until 9) {
            this.originalItems[i] = inv.getItem(i)
            inv.setItem(i, items[i])
        }

        plugin.context.sendMessage(player) {
            text("Please select [")
            text(target, Color.green)
            text("] and right click")
        }
    }

    override fun interact() {
        val result = types.getOrNull(player.inventory.heldItemSlot)
        if (result != target) {
            plugin.captcha.fail(player)
        } else plugin.captcha.accept(player)
    }

    override fun complete() {
        // Set items back
        val inv = player.inventory
        for (it in originalItems) {
            inv.setItem(it.key, it.value)
        }
    }
}

data class CaptchaImageCache(
    var world: String,
    val cache: MutableMap<String, Int>
)
