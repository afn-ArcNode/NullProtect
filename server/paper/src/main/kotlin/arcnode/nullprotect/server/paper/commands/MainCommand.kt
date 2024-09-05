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

package arcnode.nullprotect.server.paper.commands

import arcnode.nullprotect.server.paper.plugin
import arcnode.nullprotect.server.paper.utils.runOnScheduler
import cn.afternode.commons.bukkit.BukkitResolver
import cn.afternode.commons.bukkit.kotlin.*
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import java.awt.Color
import java.util.UUID

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
                "unbind" -> unbind(sender, *args)
                "activation" -> activation(sender, *args)
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
            if (plugin.hwidConfiguration.enabled && plugin.hwidConfiguration.matchMode != 0 && sender.hasPermission(PERM_CMD_HWID))
                line().text("hwid [add|remove] [hwid] -     Add/Remove HWID in whitelist/blacklist")
            if (plugin.hwidConfiguration.binding && sender.hasPermission(PERM_CMD_UNBIND))
                line().text("unbind [player]    -   Unbind player with HWID")
            if (plugin.activationConfig.enabled && sender.hasPermission(PERM_CMD_ACTIVATION))
                line().text("activation [check|generate] (player) -     Generate activation code or check player account activation")
        }
    }

    private fun refreshCache(sender: CommandSender) {
        if (!sender.hasPermission(PERM_CMD_REFRESH_CACHE)) {
            sender.sendMessage(MSG_NO_PERMISSION)
            return
        }

        plugin.runAsync {
            plugin.database.accountActivationCode.clearCache()
            plugin.database.accountActivation.clearCache()
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
            sender.sendMessage(MSG_INVALID_PARAMS)
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
                if (plugin.hwidConfiguration.enabled) {
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
            sender.sendMessage(MSG_INVALID_PARAMS)
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
                        if (plugin.hwidConfiguration.matchMode == 2) {
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
                        if (plugin.hwidConfiguration.matchMode == 1) {
                            val players = plugin.network.getPlayerByHwid(hwid)
                            if (players.isNotEmpty()) {
                                for (player in players) {
                                    plugin.slF4JLogger.info("(${sender.name}) Kicking player \"${player.name}\" due to blacklist/whitelist change")
                                    player.runOnScheduler {
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

    private fun activation(sender: CommandSender, vararg args: String) {    // 0:activation 1:[check|generate] 2:(player)
        if (!plugin.activationConfig.enabled) {
            this.help(sender)
            return
        }
        if (args.size < 2) {    // Not enough params
            sender.sendMessage(MSG_INVALID_PARAMS)
            return
        }
        if (!sender.hasPermission(PERM_CMD_ACTIVATION)) {   // No permission
            sender.sendMessage(MSG_NO_PERMISSION)
            return
        }

        val op = args[1].lowercase()
        if (
            (op != "check" && op != "generate") ||
            (op == "check" && args.size != 3) ||
            (op == "generate" && args.size != 2)
            ) {    // Invalid operation
            sender.sendMessage(MSG_INVALID_PARAMS)
            return
        }

        plugin.runBlockingCoroutine {
            if (op == "check") {    // Activation check
                val player = BukkitResolver.resolvePlayer(args[2])
                val model = plugin.database.accountActivation.find(player.uniqueId)
                plugin.context.sendMessage(sender) {
                    if (model == null) {
                        text("This player was not activated")
                    } else {
                        text("This player was activated with code ")
                        append(message {
                            text(model.code)
                            click { copy(model.code) }
                        })
                    }
                }
            } else {    // Code generation
                val gen = plugin.database.accountActivationCode.add(
                    UUID.randomUUID().toString().replace("-", ""),
                    sender.name
                )
                plugin.context.sendMessage(sender) {
                    if (gen == null) {
                        text("Failed to generated code (Unknown database error)")
                    } else {
                        text("Generated: ")
                        append(message {
                            text(gen.code)
                            click { copy(gen.code) }
                        })
                        plugin.slF4JLogger.info("(${sender.name}) Generated activation code ${gen.code}")
                    }
                }
            }
        }
    }

    private fun unbind(sender: CommandSender, vararg args: String) {    // 0:unbind 1:[player]
        if (!sender.hasPermission(PERM_CMD_UNBIND)) {
            sender.sendMessage(MSG_NO_PERMISSION)
            return
        }
        if (args.size != 2) {   // 2 parameters required
            sender.sendMessage(MSG_INVALID_PARAMS)
            return
        }

        val target = BukkitResolver.resolvePlayer(args[1])

        if (target == null) {   // Not found
            plugin.context.message(sender).text("Cannot find player with \"${args[1]}\"").send()
        } else {
            plugin.runBlockingCoroutine {
                plugin.database.hwidBinding.remove(target.uniqueId)
                plugin.context.message(sender).text("Operation completed").send()
                plugin.slF4JLogger.info("(${sender.name}) Unbind player \"${target.name}\" from HWID")
            }
        }
    }

    override fun tab(sender: CommandSender, vararg args: String): MutableList<String> = commandSuggestion {
        if (args.size == 1) {
            add(args[0], "refreshCaches", "hwid", "info", "activation", "unbind")
        } else if (args.size == 2) {
            if (args[0] == "info" || args[0] == "unbind")
                players(args[1])
            if (args[0] == "hwid")
                add(args[1], "add", "remove")
            if (args[0] == "activation")
                add(args[1], "generate", "check")
        } else if (args.size == 3) {
            if (args[0] == "activation" && args[1] == "check") {
                players(args[2])
                add(args[2], "uuid:")
            }
        }
    }
}