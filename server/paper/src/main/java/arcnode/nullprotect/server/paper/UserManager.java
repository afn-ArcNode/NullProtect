package arcnode.nullprotect.server.paper;

import arcnode.nullprotect.network.HardwareIdentifyData;
import arcnode.nullprotect.network.PacketIO;
import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPluginMessage;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UserManager implements Listener, PluginMessageListener {
    private final Map<UUID, String> hwid = new HashMap<>();

    public String get(Player player) {
        return hwid.get(player.getUniqueId());
    }

    public void set(Player player, String value) {
        this.hwid.put(player.getUniqueId(), value);
    }

    // HWID timeout check
    public void runHwidCheck(ScheduledTask task) {
        long now = System.currentTimeMillis();

        for (Player p : Bukkit.getOnlinePlayers()) {
            if (!hwid.containsKey(p.getUniqueId()) && now - p.getLastLogin() > 10000) { // Timed out
                p.getScheduler().run(NullProtectPaper.getInstance(), t -> p.kick(Component.text("Verification timed out")), () -> {});
            }
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        // Send HWID request
        PacketEvents.getAPI().getPlayerManager().sendPacket(event.getPlayer(), new WrapperPlayServerPluginMessage(
                NullProtectPaper.CHANNEL,
                new byte[0]
        ));
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] message) {
        HardwareIdentifyData id = PacketIO.decode(message);
        this.hwid.put(player.getUniqueId(), id.value());
        NullProtectPaper.getInstance().getSLF4JLogger().info("HWID of {} is {}", player.getName(), id.value());
    }
}
