package arcnode.nullprotect.server.paper.commands

import arcnode.nullprotect.server.paper.plugin
import cn.afternode.commons.bukkit.kotlin.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.awt.Color

object MainCommand: BaseCommand("nullprotect") {
    init {
        this.aliases.add("nprot")
    }

    override fun exec(sender: CommandSender, vararg args: String) {
        if (!sender.hasPermission(PERM_CMD)) {
            sender.sendMessage(MSG_NO_PERMISSION_FULL)
            return
        }

        if (args.isEmpty()) {
            help(sender)
        } else {
            when (args[0]) {
                "refreshCaches" -> refreshCache(sender)
                "info" -> info(sender, *args)
                "hwid" -> hwid(sender, *args)
                else -> help(sender)
            }
        }
    }

    private fun help(sender: CommandSender) {
        plugin.context.sendMessage(sender) {
            text("Usage: ")
            if (sender.hasPermission(PERM_CMD_REFRESH_CACHE))
                line().text("refreshCaches -   Refresh database caches")
            if (sender.hasPermission(PERM_CMD_INFO))
                line().text("info [player] -   Get player info")
            if (plugin.hwidEnabled && plugin.hwidMatchMode != 0 && sender.hasPermission(PERM_CMD_HWID))
                line().text("hwid [add|remove] [hwid] -     Add/Remove HWID in whitelist/blacklist")
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

    private fun info(sender: CommandSender, vararg args: String) {    // 0:info 1:[player]
        if (!sender.hasPermission(PERM_CMD_INFO)) {
            sender.sendMessage(MSG_NO_PERMISSION)
            return
        }
        if (args.size != 2) {
            plugin.context.message(sender).text("Invalid parameters").send()
            return
        }

        val player = Bukkit.getPlayer(args[1])
        plugin.context.sendMessage(sender) {
            if (player == null) {   // Not found
                text("Cannot find target player with name ${args[1]}")
            } else {
                text("UUID: ")
                append(message {
                    text(player.uniqueId.toString(), Color.green)
                    click {
                        copy(player.uniqueId.toString())
                    }
                })

                // HWID
                if (plugin.hwidEnabled) {
                    line()
                    text("HWID: ")
                    append(message {
                        val hwid = plugin.network[player.uniqueId]
                        text(hwid ?: "(Not received)", Color.green)
                        click {
                            copy(hwid ?: "null")
                        }
                    })
                }
            }
        }
    }

    private fun hwid(sender: CommandSender, vararg args: String) {  // 0:hwid 1:[add|remove] 2:[hwid]
        if (!sender.hasPermission(PERM_CMD_HWID)) {
            sender.sendMessage(MSG_NO_PERMISSION)
            return
        }
        if (args.size != 3) {   // Not enough/Too many parameters
            plugin.context.message(sender).text("Invalid parameters").send()
            return
        }

        val op = args[1].lowercase()
        if (op != "add" && op != "remove") {    // Invalid operation
            plugin.context.message(sender).text("Unknown operation \"$op\"").send()
            return
        }
        val hwid = args[2].lowercase()
        if (hwid.length != 32) {    // Invalid HWID
            plugin.context.message(sender).text("HWID must be a 32-characters string").send()
            return
        }

        plugin.runBlockingCoroutine {
            try {
                plugin.database.whiteOrBlackList.let {
                    if (op == "add") {
                        it.add(hwid)
                        plugin.slF4JLogger.info("(${sender.name}) Adding \"$hwid\" to blacklist/whitelist")


                        // Blacklist operations
                        if (plugin.hwidMatchMode == 2) {
                            val players = plugin.network.getPlayerByHwid(hwid)
                            if (players.isNotEmpty()) {
                                Bukkit.getGlobalRegionScheduler().run(plugin) {
                                    val exec = Bukkit.getConsoleSender()
                                    for (player in players) {
                                        plugin.slF4JLogger.info("(${sender.name}) Executing blacklist commands due to blacklist/whitelist change")
                                        for (cmd in plugin.hwidOnBlackListOp) {
                                            Bukkit.dispatchCommand(exec, cmd.replace("%player%", player.name))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else {
                        it.remove(hwid)
                        plugin.slF4JLogger.info("(${sender.name}) Removing \"$hwid\" from blacklist/whitelist")

                        // Whitelist operations
                        if (plugin.hwidMatchMode == 1) {
                            val players = plugin.network.getPlayerByHwid(hwid)
                            if (players.isNotEmpty()) {
                                Bukkit.getGlobalRegionScheduler().run(plugin) {
                                    for (player in players) {
                                        plugin.slF4JLogger.info("(${sender.name}) Kicking player \"${player.name}\" due to blacklist/whitelist change")
                                        player.kick(Component.text("You are not whitelisted on this server!"))
                                    }
                                }
                            }
                        }
                    }
                }
                plugin.context.message(sender).text("Operation completed").send()
            } catch (t: Throwable) {
                plugin.context.message(sender).text("Error executing operation").send()
                plugin.slF4JLogger.info("(${sender.name}) Error ${ if (op == "add") "adding" else "removing" } \"$hwid\"")
                return@runBlockingCoroutine
            }
        }
    }

    override fun tab(sender: CommandSender, vararg args: String): MutableList<String> = commandSuggestion {
        if (args.size == 1) {
            add(args[0], "refreshCaches", "info")
        } else if (args.size == 2) {
            if (args[0] == "info")
                players(args[1])
            if (args[0] == "hwid")
                add(args[1], "add", "remove")
        }
    }
}