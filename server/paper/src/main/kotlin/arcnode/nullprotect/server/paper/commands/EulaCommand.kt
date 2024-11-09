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
import cn.afternode.commons.bukkit.kotlin.BaseCommand
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player

object EulaCommand: BaseCommand("eula") {
    override fun exec(sender: CommandSender, vararg args: String) {
        if (sender is Player && plugin.hasEula()) {
            val operation = args.getOrNull(0)?.lowercase() ?: return
            if (operation == "accept") {
                plugin.eula.accept(sender)
            } else {
                sender.kick(plugin.eula.denyKick)
            }
        }
    }
}