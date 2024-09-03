package arcnode.nullprotect.server.paper

import arcnode.nullprotect.network.PacketIO
import arcnode.nullprotect.server.DatabaseManager
import arcnode.nullprotect.server.paper.network.NetworkManager
import cn.afternode.commons.bukkit.BukkitPluginContext
import cn.afternode.commons.bukkit.kotlin.message
import com.github.retrooper.packetevents.resources.ResourceLocation
import org.bukkit.Bukkit
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.plugin.java.JavaPlugin
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

        try {   // Database
            log.info("Setting up database")
            val sec = conf.getConfigurationSection("database") ?: throw NullPointerException("database @ config.yml")
            val type = sec.getString("type") ?: "SQLite"
            if (type.equals("SQLite", ignoreCase = true)) {
                this.database = DatabaseManager("jdbc:sqlite:./plugins/${this.name}/data")
            } else if (type.equals("MySQL", ignoreCase = true)) {
                this.database = DatabaseManager(
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
        Bukkit.getAsyncScheduler().runAtFixedRate(this, network::runHwidCheck, 1, 10, TimeUnit.SECONDS)
    }
}
