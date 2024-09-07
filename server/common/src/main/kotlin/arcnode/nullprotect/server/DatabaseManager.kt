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

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.pool.HikariPool
import kotlinx.coroutines.asCoroutineDispatcher
import org.apache.logging.log4j.Level
import org.apache.logging.log4j.Logger
import org.apache.logging.log4j.core.config.Configurator
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.exposedLogger
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.concurrent.ExecutorService

class DatabaseManager {
    private val db: Database
    private val executor: ExecutorService

    lateinit var accountActivationCode: AccountActivationCodeDatabase
        private set
    lateinit var accountActivation: AccountActivationDatabase
        private set
    lateinit var hwidBinding: HWIDBindingDatabase
        private set
    lateinit var whiteOrBlackList: WhiteOrBlackListDatabase
        private set

    constructor(executor: ExecutorService, jdbcUrl: String, username: String, password: String) {
        this.executor = executor

        val conf = HikariConfig()
        conf.jdbcUrl = jdbcUrl
        conf.username = username
        conf.password = password
        preSetup(conf)

        db = Database.connect(HikariDataSource(conf))
        this.setup()
    }

    constructor(executor: ExecutorService, jdbcUrl: String) {
        this.executor = executor

        val conf = HikariConfig()
        conf.jdbcUrl = jdbcUrl
        preSetup(conf)

        db = Database.connect(HikariDataSource(conf))
        this.setup()
    }

    private fun preSetup(conf: HikariConfig) {
//        val unsafeField = Unsafe::class.java.getDeclaredField("theUnsafe")
//        unsafeField.isAccessible = true
//        val unsafe = unsafeField.get(null) as Unsafe
//
//        val cl = Class.forName("org.jetbrains.exposed.sql.SQLLogKt")
//        val f = cl.getDeclaredField("exposedLogger")
//        val off = unsafe.staticFieldOffset(f)
//        unsafe.putObject(cl, off, NOPLogger.NOP_LOGGER)

        conf.poolName = "NullProtect"

        Configurator.setLevel(exposedLogger.name, Level.OFF)
        Configurator.setLevel(HikariConfig::class.java.name, Level.OFF)
        Configurator.setLevel(HikariDataSource::class.java.name, Level.OFF)
        Configurator.setLevel(HikariPool::class.java.name, Level.OFF)
    }

    private fun setup() {
        transaction(db) {
            SchemaUtils.createMissingTablesAndColumns(
                AccountActivationCodeTable,
                AccountActivationTable,
                HWIDBindingTable,
                WhiteOrBlackListTable
            )
        }

        this.accountActivationCode = AccountActivationCodeDatabase(this)
        this.accountActivation = AccountActivationDatabase(this)
        this.hwidBinding = HWIDBindingDatabase(this)
        this.whiteOrBlackList = WhiteOrBlackListDatabase(this)
    }

    suspend fun <T> use(stmt: Transaction.() -> T): T = newSuspendedTransaction(context = executor.asCoroutineDispatcher(), db = db, statement = stmt)
}