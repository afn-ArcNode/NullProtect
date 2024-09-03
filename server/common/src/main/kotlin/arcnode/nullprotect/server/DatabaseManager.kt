package arcnode.nullprotect.server

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.jetbrains.exposed.sql.Database

class DatabaseManager {
    private val db: Database

    constructor(jdbcUrl: String, username: String, password: String) {
        val conf = HikariConfig()
        conf.jdbcUrl = jdbcUrl
        conf.username = username
        conf.password = password

        db = Database.connect(HikariDataSource(conf))
    }

    constructor(jdbcUrl: String) {
        val conf = HikariConfig()
        conf.jdbcUrl = jdbcUrl

        db = Database.connect(HikariDataSource(conf))
    }
}