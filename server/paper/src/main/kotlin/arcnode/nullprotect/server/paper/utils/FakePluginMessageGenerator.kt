package arcnode.nullprotect.server.paper.utils

import arcnode.nullprotect.server.getFieldObject
import arcnode.nullprotect.server.getMethodOpt
import arcnode.nullprotect.server.paper.listeners.FakePluginListener.FakePlugin
import cn.afternode.commons.bukkit.kotlin.message
import cn.afternode.commons.bukkit.kotlin.sub
import cn.afternode.commons.bukkit.message.MessageBuilder
import com.google.common.collect.Lists
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.JoinConfiguration
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.awt.Color
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

class FakePluginMessageGenerator(private val plugins: Collection<FakePlugin>) {
    val cl = Bukkit.getServer().javaClass.classLoader.loadClass("io.papermc.paper.command.PaperPluginsCommand")
    private val mh = MethodHandles.privateLookupIn(cl, MethodHandles.lookup())

    val legacyPluginStar = cl.getFieldObject<Component>("LEGACY_PLUGIN_STAR")
    val pluginTick = cl.getFieldObject<Component>("PLUGIN_TICK")
    val purpur = cl.getMethodOpt("getAuthors") != null  // method added by purpur
    val header = mh.findStatic(cl, "header", MethodType.methodType(Component::class.java, java.lang.String::class.java, Integer.TYPE, Integer.TYPE, java.lang.Boolean.TYPE))

    val enabledColor = Color(NamedTextColor.GREEN.value())

    private fun hover(plugin: FakePlugin) = message {
        text("Version: ").text(plugin.version, enabledColor)
        if (plugin.author != null)
            line().text("Author: ").text(plugin.author, enabledColor)
    }

    private fun formatProvider(plugin: FakePlugin, sender: CommandSender): Component = message {
        if (plugin.legacy)
            append(legacyPluginStar)
        sub {
            text(plugin.name, enabledColor)

            if (purpur && sender.hasPermission("bukkit.command.version")) // purpur specified
            {
                click(ClickEvent.suggestCommand("/version ${plugin.name}"))
                hover(HoverEvent.showText(hover(plugin)))
            }
        }
    }

    private fun formatProviders(plugins: Set<FakePlugin>, sender: CommandSender): List<Component> {
        val list = arrayListOf<Component>()
        list += plugins.map { this.formatProvider(it, sender) }

        var first = true
        val formatted = arrayListOf<Component>()
        for (sub in Lists.partition(list, 10)) {
            formatted += message {
                if (first) {
                    append(pluginTick)
                    first = false
                }
                append(Component.join(JoinConfiguration.commas(true), sub))
            }
        }

        return formatted
    }

    fun get(sender: CommandSender): List<Component>? {
        if (this.plugins.isEmpty())
            return null

        val paper = hashSetOf<FakePlugin>()
        val spigot = hashSetOf<FakePlugin>()
        for (pl in this.plugins) {
            if (pl.isPaperPlugin())
                paper += pl
            else spigot += pl
        }

        val result = arrayListOf<Component>()
        if (purpur || paper.isNotEmpty())
            result += this.header.invoke("Paper Plugins", 116097, paper.size, true) as Component
        result += formatProviders(paper, sender)
        if (purpur || spigot.isNotEmpty())
            result += this.header.invoke("Bukkit Plugins", 11565062, spigot.size, true) as Component
        result += formatProviders(spigot, sender)

        return result
    }
}