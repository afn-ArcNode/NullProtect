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

package arcnode.nullprotect.server.paper.eula

import arcnode.nullprotect.server.EulaStateModel
import arcnode.nullprotect.server.paper.plugin
import cn.afternode.commons.bukkit.kotlin.book
import cn.afternode.commons.bukkit.kotlin.message
import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.inventory.Book
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import java.awt.Color
import java.util.UUID

class EulaManager: Listener {
    private val book: Book = book {
        plugin.eulaConfiguration.text.forEach {
            page {
                append(it)
            }
        }
    }
    private val acceptMsg: Component = plugin.context.message {
        text("To play on this server, you have to accept the EULA")
        line()

        if (plugin.eulaConfiguration.mode == 1) {   // Append URL
            append(message {
                text("Open EULA page", Color.cyan)
                click(ClickEvent.openUrl(plugin.eulaConfiguration.link))
            })
            line()
        }

        text("Accept EULA?")
        line()

        append(message {   // Accept
            text("Yes", Color.green)
            click(ClickEvent.runCommand("/nullprotect:eula accept"))
        })
        text(" | ", Color.gray)
        append(message {    // Deny
            text("No", Color.red)
            click(ClickEvent.runCommand("/nullprotect:eula deny"))
        })
    }

    private val onAccept: Component = message {
        plugin.eulaConfiguration.onAccept.forEach {
            line()
            mini(it)
        }
    }
    val denyKick: Component = message {
        plugin.eulaConfiguration.denyKick.forEach {
            line()
            mini(it)
        }
    }

    private val pending = hashSetOf<UUID>()

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        plugin.runBlockingCoroutine {
            if (plugin.database.eula.get(event.player.uniqueId)?.accepted != true) {    // Not accepted
                if (plugin.eulaConfiguration.mode == 0) {   // TEXT mode
                    event.player.openBook(this.book)
                }
                // Send accept message
                event.player.sendMessage(this.acceptMsg)
                this.pending.add(event.player.uniqueId)
            }
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {  // No movement
        if (this.pending.contains(event.player.uniqueId))
            event.isCancelled = true
    }

    @EventHandler
    fun onAsyncChat(event: AsyncChatEvent) {    // Block chat
        if (this.pending.contains(event.player.uniqueId))
            event.isCancelled = true
    }

    fun accept(player: Player) {
        this.pending.remove(player.uniqueId)
        plugin.runBlockingCoroutine {   // Update database
            plugin.database.eula.update(EulaStateModel(player.uniqueId, true))
        }
        plugin.slF4JLogger.info("(${player.name}) EULA accepted")
        player.sendMessage(this.onAccept)
    }
}
