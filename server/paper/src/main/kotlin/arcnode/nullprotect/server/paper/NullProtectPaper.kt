package arcnode.nullprotect.server.paper

import arcnode.nullprotect.network.PacketIO
import arcnode.nullprotect.server.DatabaseManager
import arcnode.nullprotect.server.paper.commands.MainCommand
import arcnode.nullprotect.server.paper.network.NetworkManager
import cn.afternode.commons.bukkit.BukkitPluginContext
import cn.afternode.commons.bukkit.kotlin.message
import com.github.retrooper.packetevents.resources.ResourceLocation
import kotlinx.coroutines.runBlocking
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

val hwidChannel by lazy { ResourceLocation(PacketIO.NAMESPACE, PacketIO.PATH_HWID) }
val hwidChannelStr by lazy { hwidChannel.toString() }

lateinit var plugin: NullProtectPaper
    private set

class NullProtectPaper: JavaPlugin() {
    val context: BukkitPluginContext by lazy { BukkitPluginContext(this) }
    lateinit var conf: YamlConfiguration
        private set
    lateinit var database: DatabaseManager
        private set
    lateinit var network: NetworkManager
        private set

    lateinit var executor: ExecutorService
        private set

    // Configurations
    val hwidEnabled by lazy { this.conf.getBoolean("hwid.enabled") }
    val hwidCheckInterval by lazy { this.conf.getInt("hwid.check-interval") }   // seconds
    val hwidCheckTimeout by lazy { this.conf.getInt("hwid.timeout")*1000 }  // millis
    val hwidMatchMode by lazy { when (this.conf.getString("hwid.mode") ?: "none") {
        "none" -> 0
        "whitelist" -> 1
        "blacklist" -> 2
        else -> 0
    } } // 0-none 1-whitelist 2-blacklist
    val hwidOnBlackListOp by lazy { this.conf.getStringList("hwid.on-blacklist") }

    override fun onLoad() {
        plugin = this
        if (!this.dataFolder.exists())
            this.dataFolder.mkdirs()
    }

    override fun onEnable() {
        val log = slF4JLogger
        this.context.messageLinePrefix = message {
            gradient("[NullProtect] ", 0xC63D2F, 0xFFBB5C)
        }

        try {
            this.conf = context.upgradeConfiguration("config.yml")
        } catch (t: Throwable) {
            throw RuntimeException("Unable to upgrade/read configuration", t)
        }

        // Thread pool
        log.info("Starting thread pool")
        val threadPoolSettings = this.conf.getConfigurationSection("async") ?: throw NullPointerException("async @ config.yml")
        if (threadPoolSettings.getString("mode") == "virtual") {
            this.executor = Executors.newVirtualThreadPerTaskExecutor()
        } else {
            this.executor = ThreadPoolExecutor(threadPoolSettings.getInt("core", 10), threadPoolSettings.getInt("max", 30), 30, TimeUnit.SECONDS, LinkedBlockingDeque())
        }

        try {   // Database
            log.info("Setting up database")
            val sec = conf.getConfigurationSection("database") ?: throw NullPointerException("database @ config.yml")
            val type = sec.getString("type") ?: "SQLite"
            if (type.equals("SQLite", ignoreCase = true)) {
                this.database = DatabaseManager(this.executor, "jdbc:sqlite:./plugins/${this.name}/data")
            } else if (type.equals("MySQL", ignoreCase = true)) {
                this.database = DatabaseManager(
                    this.executor,
                    "jdbc:mysql://${sec.getString("host") ?: throw NullPointerException("database.host @ config.yml")}:${sec.getInt("port", 3306)}/${sec.getString("database") ?: throw NullPointerException ("database.database @ config.yml")}",
                    sec.getString("username") ?: throw NullPointerException("database.username @ config.yml") ,
                    sec.getString("password") ?: "database.password @ config.yml"
                )
            } else throw IllegalArgumentException("Unknown database type $type")
        } catch (t: Throwable) {
            throw RuntimeException("Unable to setup database", t)
        }

        // Register networking
        this.network = NetworkManager()
        Bukkit.getMessenger().registerIncomingPluginChannel(this, hwidChannelStr, this.network)
        Bukkit.getPluginManager().registerEvents(this.network, this)
        if (this.hwidEnabled)   // Hwid checker
            Bukkit.getAsyncScheduler().runAtFixedRate(this, network::runHwidCheck, 1, this.hwidCheckInterval.toLong(), TimeUnit.SECONDS)

        MainCommand.register("nullprotect")
    }

    fun runAsync(runnable: () -> Unit) = this.executor.execute(runnable)
    fun runBlockingCoroutine(runnable: suspend () -> Unit) = this.runAsync { runBlocking { runnable() } }
}
