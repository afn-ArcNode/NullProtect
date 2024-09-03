package arcnode.nullprotect.server.paper.commands

import arcnode.nullprotect.server.paper.plugin
import cn.afternode.commons.bukkit.kotlin.BaseCommand
import cn.afternode.commons.bukkit.kotlin.commandSuggestion
import cn.afternode.commons.bukkit.kotlin.message
import org.bukkit.command.CommandSender

object MainCommand: BaseCommand("nullprotect") {
    init {
        this.aliases.add("nprot")
    }

    override fun exec(sender: CommandSender, vararg args: String) {
        if (!sender.hasPermission(PERM_CMD)) {
            sender.sendMessage(MSG_NO_PERMISSION_FULL)
        }

        if (args.isEmpty()) {
            help(sender)
        } else {
            when (args[0]) {
                "refreshCaches" -> refreshCache(sender)
                else -> help(sender)
            }
        }
    }

    private fun help(sender: CommandSender) {
        plugin.context.message(sender) {
            if (sender.hasPermission(PERM_CMD_REFRESH_CACHE))
                text("refreshCaches -   Refresh database caches")
        }
    }

    private fun refreshCache(sender: CommandSender) {
        if (!sender.hasPermission(PERM_CMD_REFRESH_CACHE)) {
            sender.sendMessage(MSG_NO_PERMISSION)
            return
        }

        plugin.runAsync {
            plugin.database.whiteOrBlackList.clearCache()
            plugin.context.message(sender).text("All caches clear").send()
        }
    }

    override fun tab(sender: CommandSender, vararg args: String): MutableList<String> = commandSuggestion {
        if (args.size == 1) {
            add(args[0], "refreshCaches")
        }
    }
}