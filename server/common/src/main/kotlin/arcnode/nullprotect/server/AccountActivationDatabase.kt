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
import kotlinx.coroutines.runBlocking
import org.jetbrains.exposed.sql.ResultRow
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.UUID
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.TimeUnit

class AccountActivationDatabase(private val db: DatabaseManager) {
    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterAccess(1, TimeUnit.MINUTES)
        .build<UUID, AccountActivationModel>()

    fun findAsync(executor: ExecutorService, player: UUID): CompletableFuture<AccountActivationModel?> = CompletableFuture.supplyAsync({
        runBlocking {
            find(player)
        }
    }, executor)

    suspend fun find(player: UUID) = cache.getIfPresent(player) ?: db.use {
        AccountActivationTable
            .selectAll()
            .where(AccountActivationTable.player eq player)
            .singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(player, this) }
    }

    suspend fun add(player: UUID, code: String) = db.use {
        AccountActivationTable
            .insert {
                it[AccountActivationTable.player] = player
                it[since] = System.currentTimeMillis()
                it[AccountActivationTable.code] = code
            }
            .resultedValues
            ?.singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(player, this) }
    }

    fun clearCache() = this.cache.invalidateAll()

    private fun map(r: ResultRow) = AccountActivationModel(
        r[AccountActivationTable.player],
        r[AccountActivationTable.since],
        r[AccountActivationTable.code]
    )
}

object AccountActivationTable: Table("nullprot_acc_act") {
    val player = uuid("player")
    val since = long("since")
    val code = varchar("code", 32).uniqueIndex("code")

    override val primaryKey: PrimaryKey = PrimaryKey(player)
}

data class AccountActivationModel(
    val player: UUID,
    val since: Long,
    val code: String
)
