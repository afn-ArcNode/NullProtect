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

import arcnode.nullprotect.server.getFieldObject
import arcnode.nullprotect.server.getMethodOpt
import arcnode.nullprotect.server.paper.commands.PERM_BYPASS_FAKEPL
import arcnode.nullprotect.server.paper.plugin
import arcnode.nullprotect.server.paper.utils.FakePluginMessageGenerator
import arcnode.nullprotect.server.paper.utils.asAWT
import cn.afternode.commons.bukkit.kotlin.message
import cn.afternode.commons.bukkit.message.MessageBuilder
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

    private val fakePluginsKeys = hashSetOf<String>()
    private val fakePlugins = hashMapOf<String, FakePlugin>()
    private val fakeVersionMessages = HashMap<String, Component>()

    private val versionCmd = hashSetOf("version")
    private val pluginCmd = hashSetOf("plugin")

    private lateinit var generator: FakePluginMessageGenerator

    fun init() {
        PacketEvents.getAPI().eventManager.registerListener(this)
        Bukkit.getPluginManager().registerEvents(this, plugin)

        val cm = Bukkit.getCommandMap()
        cm.getCommand("version")?.aliases?.let(versionCmd::addAll)
        cm.getCommand("plugins")?.aliases?.let(pluginCmd::addAll)

        // parse fake plugins
        for ((index, map) in plugin.fakeConfiguration.fakePlugins.withIndex()) {
            val name = map["name"] as? String ?: throw NullPointerException("Missing name in fake plugin $index")
            this.fakePlugins[name.lowercase()] =
                FakePlugin(
                    name,
                    map["author"] as? String,
                    map["version"] as? String ?: "unspecified",
                    when ((map["type"] as? String)?.lowercase() ?: throw java.lang.NullPointerException("Missing plugin type in fake plugin $index")) {
                        "server" -> 0
                        "paper" -> 1
                        else -> throw IllegalArgumentException("Unknown plugin type in fake plugin $index")
                    },
                    (map["legacy"] as? Boolean) ?: false,
                    map["description"] as? String
                )
        }
        this.fakePluginsKeys.clear()
        this.fakePluginsKeys.addAll(this.fakePlugins.keys)

        this.generator = FakePluginMessageGenerator(this.fakePlugins.values)
    }

    private fun getFakeVersionMessage(name: String): Component? {
        val lower = name.lowercase()
        if (fakeVersionMessages.containsKey(lower))
            return fakeVersionMessages[lower]

        // Search from configurations
        val plugin = this.fakePlugins.firstNotNullOfOrNull { entry -> if (entry.key.startsWith(lower)) entry else null } ?: return null
        val rName = plugin.key.lowercase()
        if (fakeVersionMessages.containsKey(rName)) { // Already created
            val get = fakeVersionMessages[rName]!!
            fakeVersionMessages[lower] = get    // Prevent next iteration
            return get
        }

        // Create new
        val pl = plugin.value
        val created = message {
            val green = Color.LIME.asAWT()
            text(pl.name, green)
            text(" version ")
            text(pl.version, green)
            line()

            pl.author?.let {
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
                event.completions.addAll(this.fakePluginsKeys.filter { it.startsWith(name) })
            }
        }
    }

    @EventHandler
    fun onCommandPreProcess(event: PlayerCommandPreprocessEvent) {
        if (event.player.hasPermission(PERM_BYPASS_FAKEPL)) // bypass
            return

        val args = event.message.split(" ").toMutableList()
        var name = args.removeFirstOrNull()?.replaceFirst("/", "")?.lowercase() ?: return
        if (":" in name)
            name = name.split(":").getOrNull(1) ?: return

        if (name in this.versionCmd) {   // Version command
            val pluginName = args.getOrNull(0)?.lowercase() ?: return
            if (pluginName in plugin.fakeConfiguration.hidePlugins) {  // Hide plugins
                event.isCancelled = true
                event.player.sendMessage(fakeNoSuchPlugin)
            }
            if (plugin.fakeConfiguration.fakeVersion)   // Send the fake version message
                this.getFakeVersionMessage(pluginName)?.let {
                    event.isCancelled = true
                    event.player.sendMessage(it)
                }
        } else if (name in this.pluginCmd) {    // plugins command
            val gen = this.generator.get(event.player)
            if (gen != null) {
                event.isCancelled = true
                for (component in gen) {
                    event.player.sendMessage(component)
                }
            }
        }
    }

    // https://wiki.vg/Protocol#Command_Suggestions_Response
    override fun onPacketSend(event: PacketSendEvent) {
        // Process tabs
        if (event.packetType == PacketType.Play.Server.TAB_COMPLETE && lastTab.containsKey(event.user.uuid)) {
            val hide = plugin.fakeConfiguration.hidePlugins
            if (hide.isNotEmpty()) {
                val packet = WrapperPlayServerTabComplete(event)
                packet.commandMatches.removeIf { it.text.lowercase() in hide }    // Remove self from completions
            }
        }
    }

    data class FakePlugin(
        val name: String,
        val author: String?,
        val version: String,
        val type: Int,
        val legacy: Boolean,
        val description: String?
    ) {
        fun isPaperPlugin() = (this.type == 1)
    }
}
