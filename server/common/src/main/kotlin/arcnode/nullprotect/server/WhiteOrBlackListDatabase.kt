package arcnode.nullprotect.server

import com.google.common.cache.CacheBuilder
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.selectAll
import java.util.concurrent.TimeUnit

class WhiteOrBlackListDatabase(private val db: DatabaseManager) {
    private val cache = CacheBuilder.newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build<String, Boolean>()

    suspend fun exists(hwid: String): Boolean {
        var exists = this.cache.getIfPresent(hwid)
        if (exists == null) {
            exists = this.exists0(hwid)
            this.cache.put(hwid, exists)    // Update cache (from database)
        }
        return exists
    }

    private suspend fun exists0(hwid: String) = db.use {
        WhiteOrBlackListTable
            .selectAll()
            .where { WhiteOrBlackListTable.hwid eq hwid }
            .singleOrNull() != null
    }

    suspend fun add(hwid: String) = db.use {
        WhiteOrBlackListTable
            .insert {
                it[WhiteOrBlackListTable.hwid] = hwid
            }
        this@WhiteOrBlackListDatabase.cache.put(hwid, true) // Update cache (Added)
    }

    suspend fun remove(hwid: String) = db.use {
        WhiteOrBlackListTable
            .deleteWhere { WhiteOrBlackListTable.hwid eq hwid }
        this@WhiteOrBlackListDatabase.cache.put(hwid, false)    // Update cache (removed)
    }

    fun clearCache() = this.cache.invalidateAll()
}

object WhiteOrBlackListTable: Table("nullprotect_wl_or_bl") {
    val hwid = varchar("hwid", 32)

    override val primaryKey: PrimaryKey = PrimaryKey(hwid)
}

