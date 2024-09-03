package arcnode.nullprotect.server.paper.commands

import arcnode.nullprotect.server.paper.plugin
import cn.afternode.commons.bukkit.kotlin.message

val MSG_HEADER by lazy { message {
    useStyleStack()
    gradient("<bold>NullProtect</bold>", 0xC63D2F, 0xFFBB5C)
    text(" v${plugin.description.version}")
} }

val MSG_NO_PERMISSION_FULL by lazy { plugin.context.message {
    append(MSG_HEADER)
    line()
    text("You have no permission to do this")
} }

val MSG_NO_PERMISSION by lazy { plugin.context.message {
    text("You have no permission to do this")
} }
