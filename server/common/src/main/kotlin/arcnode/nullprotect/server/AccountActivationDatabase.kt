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
