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
import cn.afternode.commons.bukkit.kotlin.message

val MSG_HEADER by lazy { message {
    useStyleStack()
    gradient("<bold>NullProtect</bold>", 0xC63D2F, 0xFFBB5C)
    text(" v${plugin.description.version}")
} }

val MSG_PLAYER_ONLY by lazy { plugin.context.message {
    text("Player-only command")
} }

val MSG_INVALID_PARAMS by lazy { plugin.context.message {
    text("Invalid parameters")
} }

val MSG_NO_PERMISSION_FULL by lazy { plugin.context.message {
    append(MSG_HEADER)
    line()
    text("You have no permission to do this")
} }

val MSG_NO_PERMISSION by lazy { plugin.context.message {
    text("You have no permission to do this")
} }

val MSG_FEAT_DISABLED by lazy { plugin.context.message {
    text("This feature is disabled")
} }
