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

data class ActivationConfiguration(
    val enabled: Boolean,
    val timout: Long,
    val blockingChat: Boolean,
    val blockingMove: Boolean,
    val blockingInteract: Boolean
)

data class HWIDConfiguration(
    val enabled: Boolean,
    val checkInterval: Long,
    val checkTimeout: Long,
    val matchMode: Int,
    val hwidOnBlackListOp: List<String>
)
