package com.rtm516.mcxboxbroadcast.bootstrap.pnx;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import org.cloudburstmc.protocol.bedrock.data.command.CommandParamType;
import org.powernukkitx.command.Command;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.command.data.CommandParameter;
import org.powernukkitx.command.tree.ParamList;
import org.powernukkitx.command.utils.CommandLogger;

import java.util.Map;

final class MCXboxBroadcastCommand extends Command {
    private static final String RESTART = "restart";
    private static final String DUMP_SESSION = "dumpsession";
    private static final String ACCOUNTS_LIST = "accounts-list";
    private static final String ACCOUNTS_MODIFY = "accounts-modify";
    private static final String VERSION = "version";

    private final MCXboxBroadcastPlugin plugin;

    MCXboxBroadcastCommand(MCXboxBroadcastPlugin plugin) {
        super(
            "mcxboxbroadcast",
            "Manage MCXboxBroadcast.",
            "/mcxboxbroadcast <restart|dumpsession|accounts|version>",
            new String[]{"mcbroadcast"}
        );
        this.plugin = plugin;

        commandParameters.clear();
        commandParameters.put(RESTART, new CommandParameter[]{literal("action", "restart")});
        commandParameters.put(DUMP_SESSION, new CommandParameter[]{literal("action", "dumpsession")});
        commandParameters.put(ACCOUNTS_LIST, new CommandParameter[]{
            literal("action", "accounts"),
            literal("operation", "list")
        });
        commandParameters.put(ACCOUNTS_MODIFY, new CommandParameter[]{
            literal("action", "accounts"),
            CommandParameter.newEnum("operation", new String[]{"add", "remove"}),
            CommandParameter.newType("sub-session-id", CommandParamType.ID)
        });
        commandParameters.put(VERSION, new CommandParameter[]{literal("action", "version")});
        enableParamTree();
    }

    @Override
    public int execute(
        CommandSender sender,
        String commandLabel,
        Map.Entry<String, ParamList> result,
        CommandLogger commandLogger
    ) {
        String overload = result.getKey();
        ParamList parameters = result.getValue();

        if (!overload.equals(VERSION) && sender.isPlayer()) {
            sender.sendMessage("This command can only be run from the console.");
            return 0;
        }

        switch (overload) {
            case RESTART -> plugin.restart();
            case DUMP_SESSION -> plugin.dumpSession();
            case ACCOUNTS_LIST -> plugin.handleAccounts(sender, "list", null);
            case ACCOUNTS_MODIFY -> plugin.handleAccounts(
                sender,
                parameters.getResult(1),
                parameters.getResult(2)
            );
            case VERSION -> sender.sendMessage("MCXboxBroadcast PNX " + BuildData.VERSION);
            default -> {
                return 0;
            }
        }
        return 1;
    }

    private static CommandParameter literal(String name, String value) {
        return CommandParameter.newEnum(name, new String[]{value});
    }
}
