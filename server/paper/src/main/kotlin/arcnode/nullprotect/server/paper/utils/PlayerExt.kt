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

package arcnode.nullprotect.server.paper.utils

import arcnode.nullprotect.server.paper.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.entity.Player
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType

fun Player.runOnScheduler(runnable: (ScheduledTask) -> Unit) {
    this.scheduler.run(plugin, runnable) {}
}

private lateinit var playerAddChannel: MethodHandle

fun Player.openChannel(vararg name: String) {
    if (!this.javaClass.name.endsWith("CraftPlayer")) return    // not a valid player

    if (!::playerAddChannel.isInitialized) {
        val mh = MethodHandles.lookup()
        playerAddChannel = mh.findVirtual(this.javaClass, "addChannel", MethodType.methodType(Void.TYPE, String::class.java))
    }

    for (s in name) {
        playerAddChannel.invoke(this, s)
    }
}
