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

package arcnode.nullprotect.server.paper.captcha

import arcnode.nullprotect.server.paper.plugin
import arcnode.nullprotect.server.paper.utils.AWTMapRenderer
import arcnode.nullprotect.server.paper.utils.CaptchaConfiguration
import arcnode.nullprotect.server.paper.utils.runOnScheduler
import com.google.common.hash.Hashing
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.map.MapView
import java.io.ByteArrayInputStream
import java.util.UUID
import java.util.concurrent.ThreadLocalRandom
import javax.imageio.ImageIO
import kotlin.io.path.*

class CaptchaManager: Listener {
    private val captcha = HashMap<UUID, StartedCaptcha>()
    private val lastSuccessCaptcha = HashMap<UUID, Long>()

    val config: CaptchaConfiguration =
        (plugin.conf.getConfigurationSection("captcha") ?: throw NullPointerException("captcha @ config.yml")).let {
            CaptchaConfiguration(
                it.getBoolean("chest"),
                it.getBoolean("furnace"),
                it.getBoolean("book"),
                it.getBoolean("image"),

                it.getInt("min-interval", 600) * 1000,
                it.getInt("timeout", 30) * 1000,

                it.getInt("auto.lumbering", 5),
                it.getInt("auto.mining", 5),
                it.getInt("auto.mining_deepslate", 5),
                it.getInt("auto.fishing", 5)
            )
        }

    val imagesCache: CaptchaImageCache by lazy {
        val path = plugin.dataPath.resolve("captcha.images.json")
        if (path.exists()) {
            plugin.gson.fromJson(path.reader(), CaptchaImageCache::class.java)
        } else CaptchaImageCache(Bukkit.getWorlds()[0].uid.toString(), mutableMapOf())
    }

    // Load images
    val images: Map<String, List<MapView>> = if (config.image) {
        plugin.slF4JLogger.info("Loading captcha images")
        val load = mutableMapOf<String, List<MapView>>()
        val imagesDir = plugin.dataPath.resolve("captcha")
        val hash = Hashing.sha256()
        var world = Bukkit.getWorld(UUID.fromString(imagesCache.world))
        if (world == null) {
            plugin.slF4JLogger.warn("Cached world not found, rebuilding")
            world = Bukkit.getWorlds()[0]
            imagesCache.world = Bukkit.getWorlds()[0].uid.toString()
            imagesCache.cache.clear()
        }

        if (imagesDir.isDirectory()) {
            for (entry in imagesDir.listDirectoryEntries()) {
                if (!entry.isDirectory())   // Not valid directory
                    continue

                val list = mutableListOf<MapView>()
                for (img in entry.listDirectoryEntries("*.png")) {
                    val data = img.readBytes()
                    val hash = hash.hashBytes(data).toString()

                    val image = ImageIO.read(ByteArrayInputStream(data))
                    if (image.width != 128 || image.height != 128)
                        plugin.slF4JLogger.warn("Bad image size (${image.width} * ${image.height}) detected on \"${img.name}\" in type \"${entry.name}\", image may not be rendered properly")
                    val rd = AWTMapRenderer(image)

                    val view = if (imagesCache.cache.containsKey(hash)) {  // Use existing
                        Bukkit.getMap(imagesCache.cache[hash]!!)
                            ?: Bukkit.createMap(world!!)    // Deleted map or world reset?
                    } else {    // Create and add to cache
                        val view = Bukkit.createMap(world!!)
                        view
                    }
                    imagesCache.cache[hash] = view.id
                    view.addRenderer(rd)
                    list.add(view)
                }
                if (list.isEmpty()) {   // Check empty
                    plugin.slF4JLogger.warn("Images folder for type \"${entry.name}\" is empty")
                } else {
                    plugin.slF4JLogger.info("Loaded ${list.size} images for type \"${entry.name}\"")
                    load[entry.name] = list
                }
            }
        } else {
            plugin.slF4JLogger.warn("Images captcha enabled but no images exists")
        }

        plugin.slF4JLogger.info("Loaded ${load.size} types")
        plugin.dataPath.resolve("captcha.images.json").writeText(plugin.gson.toJson(imagesCache))
        load.toMap()
    } else {
        emptyMap()
    }

    @EventHandler   // Inventory click
    fun onPlayerInventoryClick(event: InventoryClickEvent) {
        val who = event.whoClicked
        if (who !is Player)
            return
        val captcha = this.getCaptcha(who) ?: return

        if(captcha.captcha is IInventoryCaptcha) {
            captcha.captcha.click(event.slot, event.rawSlot)
        }
        event.isCancelled = true
    }

    @EventHandler
    fun onPlayerInteract(event: PlayerInteractEvent) {  // Interact trigger
        val cap = this.getCaptcha(event.player)?.captcha ?: return
        if (cap is IInteractCaptcha)
            cap.interact()
    }

    @EventHandler   // No inventory close during captcha
    fun onPlayerInventoryClosed(event: InventoryCloseEvent) {
        val who = event.player
        if (who !is Player)
            return
        val captcha = this.getCaptcha(who) ?: return

        if (captcha.captcha is IInventoryCaptcha) {
            fail(who)
        }
    }

    @EventHandler   // No movement during captcha
    fun onPlayerMove(event: PlayerMoveEvent) {
        if (isInCaptcha(event.player)) {
            if (event.from.x != event.to.x || event.to.z != event.from.z)   // Don't block Y/Yaw/Pitch changes
                event.isCancelled = true
        }
    }

    @EventHandler
    fun onPlayerQuit(event: PlayerQuitEvent) {
        this.lastSuccessCaptcha.remove(event.player.uniqueId)   // Reset last success
        val current = this.captcha.remove(event.player.uniqueId)  // Remove current
        current?.captcha?.complete()
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        this.lastSuccessCaptcha[event.player.uniqueId] = System.currentTimeMillis() // Don't trigger captcha immediately
    }

    @EventHandler   // Mining/Lumbering trigger
    fun onBlockBreak(event: BlockBreakEvent) {
        if (System.currentTimeMillis() - (this.lastSuccessCaptcha[event.player.uniqueId] ?: 0) < config.minInterval)
            return  // Interval

        val mat = event.block.type.name
        val rd = ThreadLocalRandom.current().nextInt(0, 100)
        if (
            (mat.endsWith("LOG") && rd < config.autoLumbering) ||  // Logs
            (mat.endsWith("_ORE") && rd < config.autoMining) ||     // Mining
            (mat.startsWith("DEEPSLATE") && rd < config.autoMiningDeepslate)    // Mining (deepslate
            ) {
            start(event.player)
        }
    }

    @EventHandler
    fun onFishing(event: PlayerFishEvent) { // Fishing trigger
        if (System.currentTimeMillis() - (this.lastSuccessCaptcha[event.player.uniqueId] ?: 0) < config.minInterval)
            return  // Interval

        if (ThreadLocalRandom.current().nextInt(0, 100) < config.autoFishing)
            start(event.player)
    }

    fun fail(player: Player) {
        if (isInCaptcha(player)) {
            player.runOnScheduler {
                val cap = this.captcha.remove(player.uniqueId) ?: return@runOnScheduler
                plugin.slF4JLogger.info("(${player.name}) Captcha failed")
                cap.captcha.complete()
                player.kick(Component.text("Captcha failed"))
                cap.onComplete?.invoke(false)
            }
        }
    }

    fun accept(player: Player) {
        if (isInCaptcha(player)) {
            val c = this.captcha.remove(player.uniqueId) ?: return
            c.onComplete?.invoke(true)
            if (c.captcha is IInventoryCaptcha)
                player.closeInventory()
            c.captcha.complete()
            this.lastSuccessCaptcha[player.uniqueId] = System.currentTimeMillis()   // Record success
        }
    }

    fun isInCaptcha(player: Player) = this.captcha.containsKey(player.uniqueId)

    fun getCaptcha(player: Player) = this.captcha[player.uniqueId]

    fun start(player: Player, force: Boolean = false, onComplete: ((Boolean) -> Unit)? = null) {
        if (!isInCaptcha(player)) {
            player.runOnScheduler {
                val cap = CaptchaType.pool[ThreadLocalRandom.current().nextInt(CaptchaType.pool.size)].creator.invoke(player)
                val st = StartedCaptcha(cap, player.uniqueId, onComplete)
                this.captcha[player.uniqueId] = st
            }
        }
    }
}

data class StartedCaptcha(
    val captcha: ICaptcha,
    val player: UUID,
    val onComplete: ((Boolean) -> Unit)? = null
)

enum class CaptchaType(val creator: (Player) -> ICaptcha, val available: () -> Boolean) {
    CHEST({ ChestInventoryCaptcha(it) }, { plugin.captcha.config.chest }),
    FURNACE({ FurnaceInventoryCaptcha(it) }, { plugin.captcha.config.furnace }),
    BOOK({ BookCaptcha(it) }, { plugin.captcha.config.book }),
    IMAGE({ ImageCaptcha(it) }, { plugin.captcha.config.image });

    companion object {
        val pool by lazy {
            CaptchaType.entries
                .toMutableList()
                .filter { it.available() }
                .toList()
        }
    }
}
