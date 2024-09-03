package arcnode.nullprotect.server.paper.utils

import arcnode.nullprotect.server.paper.plugin
import io.papermc.paper.threadedregions.scheduler.ScheduledTask
import org.bukkit.entity.Player

fun Player.runOnScheduler(runnable: (ScheduledTask) -> Unit) {
    this.scheduler.run(plugin, runnable) {}
}
