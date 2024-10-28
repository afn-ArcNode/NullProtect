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

package arcnode.nullprotect.server

import com.google.common.cache.CacheBuilder
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.UUID
import java.util.concurrent.TimeUnit

class EULADatabase(private val db: DatabaseManager) {
    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build<UUID, EulaStateModel>()

    suspend fun get(player: UUID) = cache.getIfPresent(player) ?: db.use {
        EULATable
            .selectAll()
            .where { EULATable.player eq player }
            .singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(player, this) }
    }

    suspend fun update(model: EulaStateModel) = db.use {
        EULATable
            .replace {
                it[player] = model.player
                it[accepted] = model.accepted
            }
            .resultedValues
            ?.singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(player, this) }
    }

    fun clearCache() = this.cache.invalidateAll()

    private fun map(r: ResultRow): EulaStateModel = EulaStateModel(
        r[EULATable.player],
        r[EULATable.accepted]
    )
}

object EULATable: Table("nullprot_eula") {
    val player = uuid("player")
    val accepted = bool("accepted")

    override val primaryKey: PrimaryKey = PrimaryKey(player)
}

data class EulaStateModel(
    val player: UUID,
    var accepted: Boolean
)
