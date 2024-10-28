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

class HWIDBindingDatabase(private val db: DatabaseManager) {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<UUID, HWIDBindingModel>()

    suspend fun find(user: UUID) = cache.getIfPresent(user) ?: db.use {
        HWIDBindingTable
            .selectAll()
            .where { HWIDBindingTable.user eq user }
            .singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(user, this) }
    }

    suspend fun add(user: UUID, hwid: String) = db.use {
        HWIDBindingTable
            .insert {
                it[HWIDBindingTable.user] = user
                it[HWIDBindingTable.hwid] = hwid
            }
            .resultedValues
            ?.singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(user, this) }
    }

    suspend fun remove(user: UUID) = db.use {
        HWIDBindingTable
            .deleteWhere { HWIDBindingTable.user eq user }
            .apply { cache.invalidate(user) } != 0
    }

    fun clearCache() = this.cache.invalidateAll()

    private fun map(r: ResultRow) = HWIDBindingModel(
        r[HWIDBindingTable.user],
        r[HWIDBindingTable.hwid]
    )
}

object HWIDBindingTable: Table("nullprot_hwid_bind") {
    val user = uuid("user")
    val hwid = varchar("uuid", 32)
}

data class HWIDBindingModel(
    val user: UUID,
    val hwid: String
)
