package arcnode.nullprotect.server

import com.google.common.cache.CacheBuilder
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import java.util.concurrent.TimeUnit

class AccountActivationCodeDatabase(private val db: DatabaseManager) {
    private val cache = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, AccountActivationCodeModel>()

    suspend fun get(code: String) = cache.getIfPresent(code) ?: db.use {
        AccountActivationCodeTable
            .selectAll()
            .where { AccountActivationCodeTable.code eq code }
            .singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(code, this) }
    }

    suspend fun add(code: String, by: String) = db.use {
        AccountActivationCodeTable
            .insert {
                it[AccountActivationCodeTable.code] = code
                it[AccountActivationCodeTable.by] = by
            }
            .resultedValues
            ?.singleOrNull()
            ?.let(::map)
            ?.apply { cache.put(code, this) }
    }

    suspend fun remove(code: String) = db.use {
        AccountActivationCodeTable
            .deleteWhere { AccountActivationCodeTable.code eq code }
            .apply { cache.invalidate(code) } != 0
    }

    fun clearCache() = cache.invalidateAll()

    private fun map(r: ResultRow) = AccountActivationCodeModel(
        r[AccountActivationCodeTable.code],
        r[AccountActivationCodeTable.by]
    )
}

object AccountActivationCodeTable: Table("nullprot_acc_code") {
    val code = varchar("code", 32)
    val by = varchar("user", 24)
}

data class AccountActivationCodeModel(
    val code: String,
    val by: String
)
