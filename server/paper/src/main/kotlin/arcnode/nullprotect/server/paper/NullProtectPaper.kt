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

package arcnode.nullprotect.server.paper

import arcnode.nullprotect.network.PacketIO
import arcnode.nullprotect.server.DatabaseManager
import arcnode.nullprotect.server.paper.captcha.CaptchaManager
import arcnode.nullprotect.server.paper.commands.ActivateCommand
import arcnode.nullprotect.server.paper.commands.MainCommand
import arcnode.nullprotect.server.paper.listeners.AccountActivationListener
import arcnode.nullprotect.server.paper.listeners.FakePluginListener
import arcnode.nullprotect.server.paper.network.NetworkManager
import arcnode.nullprotect.server.paper.utils.ActivationConfiguration
import arcnode.nullprotect.server.paper.utils.FakeConfiguration
import arcnode.nullprotect.server.paper.utils.HWIDConfiguration
import arcnode.nullprotect.server.paper.utils.ModsConfiguration
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

val hwidChannelReq by lazy { ResourceLocation(PacketIO.NAMESPACE, PacketIO.PATH_HWID_REQUEST) }
val hwidChannelResp by lazy { ResourceLocation(PacketIO.NAMESPACE, PacketIO.PATH_HWID_RESPONSE) }
val hwidChannelRespStr by lazy { hwidChannelResp.toString() }
val modsChannelReq by lazy { ResourceLocation(PacketIO.NAMESPACE, PacketIO.PATH_MODS_REQUEST) }
val modsChannelRespStr by lazy { ResourceLocation(PacketIO.NAMESPACE, PacketIO.PATH_MODS_RESPONSE).toString() }

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
    lateinit var captcha: CaptchaManager
        private set

    lateinit var executor: ExecutorService
        private set

    // Configurations
//    val hwidEnabled by lazy { this.conf.getBoolean("hwid.enabled") }
//    val hwidCheckInterval by lazy { this.conf.getInt("hwid.check-interval") }   // seconds
//    val hwidCheckTimeout by lazy { this.conf.getInt("hwid.timeout")*1000 }  // millis
//    val hwidMatchMode by lazy { when (this.conf.getString("hwid.mode") ?: "none") {
//        "none" -> 0
//        "whitelist" -> 1
//        "blacklist" -> 2
//        else -> 0
//    } } // 0-none 1-whitelist 2-blacklist
    val hwidOnBlackListOp: List<String> by lazy { this.conf.getStringList("hwid.on-blacklist") }
    val hwidConfiguration by lazy {
        val sec = this.conf.getConfigurationSection("hwid") ?: throw NullPointerException("hwid @ config.yml")
        HWIDConfiguration(
            sec.getBoolean("enabled"),
            sec.getInt("check-interval").toLong(),  // seconds
            sec.getInt("timeout") * 1000L,  // millis
            sec.getBoolean("bind"),
            when (sec.getString("hwid.mode") ?: "none") {
                "none" -> 0
                "whitelist" -> 1
                "blacklist" -> 2
                else -> 0
            },  // 0-none 1-whitelist 2-blacklist
            sec.getStringList("on-blacklist")
        )
    }
    val activationConfig by lazy {
        val conf = this.conf.getConfigurationSection("activation") ?: throw NullPointerException("activation @ config.yml")
        ActivationConfiguration(
            conf.getBoolean("enabled"),
            TimeUnit.SECONDS.toMillis(conf.getInt("timeout").toLong()),
            conf.getBoolean("blocking.chat"),
            conf.getBoolean("blocking.move"),
            conf.getBoolean("blocking.interact")
    ) }
    val fakeConfiguration by lazy {
        val conf = this.conf.getConfigurationSection("fake") ?: throw NullPointerException("fake @ config.yml")
        FakeConfiguration(
            conf.getBoolean("enabled", true),
            conf.getBoolean("fake-version", true),
            conf.getConfigurationSection("fake-version-plugins") ?: throw NullPointerException("fake.fake-version-plugins @ config.yml"),
            conf.getBoolean("hide-self", true)
        )
    }
    val modsConfiguration by lazy {
        val conf = this.conf.getConfigurationSection("mods") ?: throw NullPointerException("mods @ config.yml")
        ModsConfiguration(
            conf.getBoolean("enabled", false),
            conf.getInt("check-interval").toLong(),  // seconds
            conf.getInt("timeout") * 1000L,  // millis
        )
    }

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

        Bukkit.getPluginManager().registerEvents(this.network, this)
        if (this.hwidConfiguration.enabled) {   // Hwid checker
            Bukkit.getAsyncScheduler().runAtFixedRate(
                this,
                network::runHwidCheck,
                1,
                this.hwidConfiguration.checkInterval.toLong(),
                TimeUnit.SECONDS
            )
            Bukkit.getMessenger().registerIncomingPluginChannel(this, hwidChannelRespStr, this.network)
        }
        if (this.modsConfiguration.enabled) {   // Mods checker
            Bukkit.getAsyncScheduler()
                .runAtFixedRate(this, network::runModsCheck, 1, this.modsConfiguration.checkInterval, TimeUnit.SECONDS)
            Bukkit.getMessenger().registerIncomingPluginChannel(this, modsChannelRespStr, this.network)
        }

        // Register activation
        if (activationConfig.enabled) {
            Bukkit.getPluginManager().registerEvents(AccountActivationListener, this)
            if (this.activationConfig.timout != -1L)    // Enable activation timeout
                Bukkit.getAsyncScheduler().runAtFixedRate(this, AccountActivationListener::runActCheck, 1, 10, TimeUnit.SECONDS)
            ActivateCommand.register("nullprot")
        }

        // Register fake
        if (fakeConfiguration.enabled) {
            FakePluginListener.init()
        }

        // Captcha
        if (conf.getBoolean("captcha.enabled")) {
            captcha = CaptchaManager()
            Bukkit.getPluginManager().registerEvents(this.captcha, this)
        }

        MainCommand.register("nullprotect")
    }

    fun runAsync(runnable: () -> Unit) = this.executor.execute(runnable)
    fun runBlockingCoroutine(runnable: suspend () -> Unit) = this.runAsync { runBlocking { runnable() } }

    fun hasCaptcha() = ::captcha.isInitialized
}
