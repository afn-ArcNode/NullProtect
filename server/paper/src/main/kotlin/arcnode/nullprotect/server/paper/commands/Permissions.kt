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

import cn.afternode.commons.bukkit.kotlin.createPermission

val PERM_ROOT by createPermission("nullprotect")
val PERM_BYPASS_MODS by createPermission("bypassMods", PERM_ROOT)

val PERM_CMD by createPermission("commands", PERM_ROOT)
val PERM_CMD_REFRESH_CACHE by createPermission("refreshCache", PERM_CMD)
val PERM_CMD_INFO by createPermission("info", PERM_CMD)
val PERM_CMD_HWID by createPermission("hwid", PERM_CMD)
val PERM_CMD_ACTIVATION by createPermission("activation", PERM_CMD)
val PERM_CMD_UNBIND by createPermission("unbind", PERM_CMD)
val PERM_CMD_MODS by createPermission("mods", PERM_CMD)
