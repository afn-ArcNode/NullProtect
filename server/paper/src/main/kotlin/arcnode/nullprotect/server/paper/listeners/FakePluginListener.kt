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

package arcnode.nullprotect.server.paper.listeners

import arcnode.nullprotect.server.paper.plugin
import arcnode.nullprotect.server.paper.utils.asAWT
import cn.afternode.commons.bukkit.kotlin.message
import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.event.PacketListenerAbstract
import com.github.retrooper.packetevents.event.PacketSendEvent
import com.github.retrooper.packetevents.protocol.packettype.PacketType
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerTabComplete
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerCommandPreprocessEvent
import org.bukkit.event.server.TabCompleteEvent
import java.util.UUID

object FakePluginListener: Listener, PacketListenerAbstract() {
    private val lastTab = mutableMapOf<UUID, String>()

    private val fakeNoSuchPlugin by lazy { message {
        text("This server is not running any plugin by that name.")
        line()
        text("Use /plugins to get a list of plugins.")
    } }

    private val fakeVersionPlugins by lazy { plugin.fakeConfiguration.fakeVersionPlugins.getKeys(false) }
    private val fakeVersionMessages = HashMap<String, Component>()

    fun init() {
        PacketEvents.getAPI().eventManager.registerListener(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)
    }

    private fun getFakeVersionMessage(name: String): Component? {
        val lower = name.lowercase()
        if (fakeVersionMessages.containsKey(name))
            return fakeVersionMessages[name]

        // Search from configurations
        val key = this.fakeVersionPlugins.firstOrNull {
            it.startsWith(name, true)
        } ?: return null
        val conf = plugin.fakeConfiguration.fakeVersionPlugins.getConfigurationSection(key) ?: return null
        val lowerKey = key.lowercase()
        if (fakeVersionMessages.containsKey(lowerKey)) { // Already created
            val get = fakeVersionMessages[lowerKey]!!
            fakeVersionMessages[lower] = get    // Prevent next iteration
            return get
        }

        // Create new
        val created = message {
            val green = Color.LIME.asAWT()
            text(key, green)
            text(" version ")
            text(conf.getString("version", "1.0.0"), green)
            line()

            conf.getString("author")?.let {
                text("Author: ")
                text(it, green)
            }
        }
        this.fakeVersionMessages[lower] = created
        return created
    }

    @EventHandler
    fun onTabComplete(event: TabCompleteEvent) {
        if (event.isCommand && event.sender is Player) {
            val args = event.buffer.split(" ").toMutableList()
            val command = args.removeFirstOrNull() ?: return
            lastTab[(event.sender as Player).uniqueId] = command

            if (plugin.fakeConfiguration.fakeVersion) {   // Fake version completions
                val name = args.firstOrNull() ?: return
                event.completions.addAll(this.fakeVersionPlugins.filter { it.startsWith(name) })
            }
        }
    }

    @EventHandler
    fun onCommandPreProcess(event: PlayerCommandPreprocessEvent) {
        val args = event.message.split(" ").toMutableList()
        val name = args.removeFirstOrNull()?.replaceFirst("/", "") ?: return

        if (name == "version" || name == "ver") {   // Version command
            val pluginName = args.getOrNull(0) ?: return
            if (plugin.fakeConfiguration.hideSelf && "NullProtect".startsWith(pluginName, true)) {  // Hide sel
                event.isCancelled = true
                event.player.sendMessage(fakeNoSuchPlugin)
            }
            if (plugin.fakeConfiguration.fakeVersion)   // Send fake version message
                this.getFakeVersionMessage(pluginName)?.let {
                    event.isCancelled = true
                    event.player.sendMessage(it)
                }
        }
    }

    // https://wiki.vg/Protocol#Command_Suggestions_Response
    override fun onPacketSend(event: PacketSendEvent) {
        // Process tabs
        if (event.packetType == PacketType.Play.Server.TAB_COMPLETE && lastTab.containsKey(event.user.uuid)) {
            val packet = WrapperPlayServerTabComplete(event)
            if (plugin.fakeConfiguration.hideSelf)
                packet.commandMatches.removeIf { it.text.equals("NullProtect") }    // Remove self from completions
        }
    }
}
