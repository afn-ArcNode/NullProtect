package arcnode.nullprotect.server.paper;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class HWIDBanCommand extends Command {
    protected HWIDBanCommand() {
        super("ban-hwid");
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String s, @NotNull String[] args) {


        return true;
    }
}
