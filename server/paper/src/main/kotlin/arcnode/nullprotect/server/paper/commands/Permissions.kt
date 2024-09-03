package arcnode.nullprotect.server.paper.commands

import cn.afternode.commons.bukkit.kotlin.createPermission

val PERM_ROOT by createPermission("nullprotect")

val PERM_CMD by createPermission("commands", PERM_ROOT)
val PERM_CMD_REFRESH_CACHE by createPermission("refreshCache", PERM_CMD)
