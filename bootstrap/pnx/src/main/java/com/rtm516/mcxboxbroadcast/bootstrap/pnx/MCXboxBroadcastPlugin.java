package com.rtm516.mcxboxbroadcast.bootstrap.pnx;

import com.rtm516.mcxboxbroadcast.core.BuildData;
import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManager;
import com.rtm516.mcxboxbroadcast.core.configs.ConfigLoader;
import com.rtm516.mcxboxbroadcast.core.configs.CoreConfig;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionCreationException;
import com.rtm516.mcxboxbroadcast.core.exceptions.SessionUpdateException;
import com.rtm516.mcxboxbroadcast.core.notifications.NotificationManager;
import com.rtm516.mcxboxbroadcast.core.notifications.SlackNotificationManager;
import com.rtm516.mcxboxbroadcast.core.storage.FileStorageManager;
import org.powernukkitx.Server;
import org.powernukkitx.command.CommandSender;
import org.powernukkitx.plugin.PluginBase;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.TimeUnit;

public final class MCXboxBroadcastPlugin extends PluginBase {
    private Logger logger;
    private NotificationManager notificationManager;
    private SessionManager sessionManager;
    private SessionInfo sessionInfo;
    private CoreConfig config;
    private MCXboxBroadcastCommand broadcastCommand;

    @Override
    public void onEnable() {
        logger = new PNXLoggerImpl(getLogger());
        logger.info("Starting MCXboxBroadcast PNX " + BuildData.VERSION + " for Bedrock "
            + Constants.BEDROCK_CODEC.getMinecraftVersion() + " ("
            + Constants.BEDROCK_CODEC.getProtocolVersion() + ")");

        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            logger.error("Failed to create data folder, plugin will not start!");
            setEnabled(false);
            return;
        }

        try {
            config = ConfigLoader.loadConfig(new File(getDataFolder(), "config.yml"), "Extension");
        } catch (IOException e) {
            logger.error("Failed to load config, plugin will not start!", e);
            setEnabled(false);
            return;
        }

        notificationManager = new SlackNotificationManager(logger, config.notifications());
        broadcastCommand = new MCXboxBroadcastCommand(this);
        getServer().getCommandMap().register("mcxboxbroadcast", broadcastCommand);
        startSession();
    }

    @Override
    public void onDisable() {
        if (sessionManager != null) {
            sessionManager.shutdown();
        }
        if (broadcastCommand != null) {
            broadcastCommand.unregister(getServer().getCommandMap());
        }
    }

    void handleAccounts(CommandSender sender, String operation, String accountId) {
        if (operation.equals("list")) {
            sessionManager.listSessions();
            return;
        }
        if (operation.equals("add")) {
            sessionManager.addSubSession(accountId);
        } else if (operation.equals("remove")) {
            sessionManager.removeSubSession(accountId);
        }
    }

    void dumpSession() {
        logger.info("Dumping session responses to 'lastSessionResponse.json' and 'currentSessionResponse.json'");
        sessionManager.dumpSession();
    }

    void restart() {
        sessionManager.shutdown();
        startSession();
    }

    private void startSession() {
        sessionManager = new SessionManager(
            new FileStorageManager(getDataFolder().toString(), new File(getDataFolder(), "screenshot.jpg").toString()),
            notificationManager,
            logger
        );
        sessionManager.setNetherNetPortRange(config.session().icePortRange().min(), config.session().icePortRange().max());
        sessionManager.scheduledThread().execute(this::initializeSession);
    }

    private void initializeSession() {
        Server server = getServer();
        String ip = config.session().remoteAddress();
        if (ip.equalsIgnoreCase("auto")) {
            ip = server.getIp();
            try {
                InetAddress address = InetAddress.getByName(ip);
                if (address.isSiteLocalAddress() || address.isAnyLocalAddress() || address.isLoopbackAddress()) {
                    HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create("https://ipv4.icanhazip.com"))
                        .GET()
                        .build();
                    ip = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body().trim();
                }
            } catch (IOException | InterruptedException ignored) {
                // Keep the configured/listener address if public IP discovery fails.
            }
        }

        int port = config.session().remotePort().equalsIgnoreCase("auto")
            ? server.getPort()
            : Integer.parseInt(config.session().remotePort());

        sessionInfo = new SessionInfo();
        sessionInfo.setIp(ip);
        sessionInfo.setPort(port);
        refreshSessionInfo();
        createSession();
    }

    private void refreshSessionInfo() {
        Server server = getServer();
        String hostName = server.getSubMotd();
        if (hostName == null || hostName.isEmpty()) {
            hostName = sessionManager.getGamertag();
        }
        sessionInfo.setHostName(hostName);
        sessionInfo.setWorldName(server.getMotd());
        sessionInfo.setPlayers(server.getOnlinePlayers().size());
        sessionInfo.setMaxPlayers(server.getMaxPlayers());
    }

    private void createSession() {
        sessionManager.restartCallback(this::restart);
        try {
            if (!sessionManager.init(sessionInfo, config.friendSync())) {
                setEnabled(false);
                return;
            }
        } catch (SessionCreationException | SessionUpdateException e) {
            logger.error("Failed to create Xbox session!", e);
            return;
        }

        sessionManager.scheduledThread().scheduleWithFixedDelay(
            this::tick,
            config.session().updateInterval(),
            config.session().updateInterval(),
            TimeUnit.SECONDS
        );
    }

    private void tick() {
        refreshSessionInfo();
        try {
            sessionManager.updateSession(sessionInfo);
        } catch (SessionUpdateException e) {
            logger.error("Failed to update session information!", e);
        }
    }
}
