package arcnode.nullprotect.server.paper;

import arcnode.nullprotect.network.PacketIO;
import arcnode.nullprotect.server.DatabaseManager;
import cn.afternode.commons.bukkit.BukkitPluginContext;
import cn.afternode.commons.bukkit.message.MessageBuilder;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class NullProtectPaper extends JavaPlugin {
    public static final ResourceLocation CHANNEL = new ResourceLocation(PacketIO.NAMESPACE, PacketIO.PATH_HWID);

    @Getter
    private static NullProtectPaper instance;

    @Getter
    private BukkitPluginContext context;
    @Getter
    private YamlConfiguration conf;
    @Getter
    private DatabaseManager database;
    @Getter
    private UserManager user;

    @Getter
    private byte[] dummyPacket;

    @Override
    public void onLoad() {
        NullProtectPaper.instance = this;
        saveDefaultConfig();
    }

    @Override
    public void onEnable() {
        Logger log = getSLF4JLogger();

        this.context = new BukkitPluginContext(this);
        this.context.setMessageLinePrefix(new MessageBuilder()
                .gradient("[NullProtect] ", 0xC63D2F, 0xFFBB5C)
                .build());

        try {
            this.conf = this.context.upgradeConfiguration("config.yml");
        } catch (Throwable t) {
            throw new RuntimeException("Unable to upgrade/read configuration", t);
        }

        try {
            log.info("Setting up database");
            ConfigurationSection sec = Objects.requireNonNull(this.conf.getConfigurationSection("database"), "database @ config.yml");
            String type = Objects.requireNonNullElse(sec.getString("type"), "SQLite");
            if (type.equalsIgnoreCase("SQLite")) {
                this.database = new DatabaseManager("jdbc:sqlite:./plugins/%s/data".formatted(this.getName()));
            } else if (type.equalsIgnoreCase("MySQL")) {
                this.database = new DatabaseManager(
                        "jdbc:mysql://%s:%s/%s".formatted(
                                Objects.requireNonNull(sec.getString("host"), "database.host @ config.yml"),
                                sec.getInt("port"),
                                Objects.requireNonNull(sec.getString("database"), "database.database @ config.yml")
                        ),
                        Objects.requireNonNull(sec.getString("username"), "database.username @ config.yml"),
                        Objects.requireNonNull(sec.getString("password"), "database.password @ config.yml")
                );
            } else throw new IllegalArgumentException("Unknown database type " + type);
        } catch (Throwable t) {
            throw new RuntimeException("Unable to setup database", t);
        }

        this.user = new UserManager();
        Bukkit.getPluginManager().registerEvents(this.user, this);
        this.dummyPacket = PacketIO.dummy();
        Bukkit.getMessenger().registerIncomingPluginChannel(this, CHANNEL.toString(), this.user);

        // Setup task
        Bukkit.getAsyncScheduler().runAtFixedRate(this, user::runHwidCheck, 1, 10, TimeUnit.SECONDS);
    }

    @Override
    public void reloadConfig() {
        try {
            this.conf = this.context.upgradeConfiguration("config.yml");
        } catch (Throwable t) {
            throw new RuntimeException("Unable to reload configuration", t);
        }
    }
}
