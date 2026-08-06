package com.rtm516.mcxboxbroadcast.core.nethernet;

import com.rtm516.mcxboxbroadcast.core.Constants;
import com.rtm516.mcxboxbroadcast.core.Logger;
import com.rtm516.mcxboxbroadcast.core.SessionInfo;
import com.rtm516.mcxboxbroadcast.core.SessionManagerCore;

import java.io.IOException;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;
import org.cloudburstmc.math.vector.Vector2f;
import org.cloudburstmc.math.vector.Vector3f;
import org.cloudburstmc.math.vector.Vector3i;
import org.cloudburstmc.nbt.NbtMap;
import org.cloudburstmc.protocol.bedrock.BedrockServerSession;
import org.cloudburstmc.protocol.bedrock.data.ChatRestrictionLevel;
import org.cloudburstmc.protocol.bedrock.data.Difficulty;
import org.cloudburstmc.protocol.bedrock.data.EduSharedUriResource;
import org.cloudburstmc.protocol.bedrock.data.EducationEditionOffer;
import org.cloudburstmc.protocol.bedrock.data.EditorWorldType;
import org.cloudburstmc.protocol.bedrock.data.GamePublishSetting;
import org.cloudburstmc.protocol.bedrock.data.GameRuleData;
import org.cloudburstmc.protocol.bedrock.data.GameType;
import org.cloudburstmc.protocol.bedrock.data.GeneratorType;
import org.cloudburstmc.protocol.bedrock.data.LevelSettings;
import org.cloudburstmc.protocol.bedrock.data.PacketCompressionAlgorithm;
import org.cloudburstmc.protocol.bedrock.data.PlayStatus;
import org.cloudburstmc.protocol.bedrock.data.PlayerPermissionLevel;
import org.cloudburstmc.protocol.bedrock.data.ServerAuthMovementMode;
import org.cloudburstmc.protocol.bedrock.data.SpawnBiomeType;
import org.cloudburstmc.protocol.bedrock.data.SpawnSettings;
import org.cloudburstmc.protocol.bedrock.data.SyncedPlayerMovementSettings;
import org.cloudburstmc.protocol.bedrock.data.payload.common.DimensionType;
import org.cloudburstmc.protocol.bedrock.data.payload.ServerTelemetryData;
import org.cloudburstmc.protocol.bedrock.data.payload.editor.ServerEditorConnectionPolicy;
import org.cloudburstmc.protocol.bedrock.data.payload.experiment.Experiments;
import org.cloudburstmc.protocol.bedrock.data.payload.pack.PackIdVersion;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacket;
import org.cloudburstmc.protocol.bedrock.packet.BedrockPacketHandler;
import org.cloudburstmc.protocol.bedrock.packet.ClientCacheStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.LoginPacket;
import org.cloudburstmc.protocol.bedrock.packet.NetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.PlayStatusPacket;
import org.cloudburstmc.protocol.bedrock.packet.RequestNetworkSettingsPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackClientResponsePacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePackStackPacket;
import org.cloudburstmc.protocol.bedrock.packet.ResourcePacksInfoPacket;
import org.cloudburstmc.protocol.bedrock.packet.StartGamePacket;
import org.cloudburstmc.protocol.bedrock.packet.TransferPacket;
import org.cloudburstmc.protocol.bedrock.util.ChainValidationResult;
import org.cloudburstmc.protocol.bedrock.util.EncryptionUtils;
import org.cloudburstmc.protocol.common.PacketSignal;
import org.cloudburstmc.protocol.common.util.OptionalBoolean;

public class RedirectPacketHandler implements BedrockPacketHandler {

    private final BedrockServerSession session;
    private final SessionInfo sessionInfo;
    private final SessionManagerCore sessionManager;

    private ChainValidationResult.IdentityData identityData;
    private boolean networkSettingsRequested = false;
    private final Logger logger;

    public RedirectPacketHandler(BedrockServerSession session, SessionInfo sessionInfo, SessionManagerCore sessionManager, Logger logger) {
        this.session = session;
        this.sessionInfo = sessionInfo;
        this.sessionManager = sessionManager;
        this.logger = logger;
    }

    private void disconnect(String message) {
        if (message == null) {
            session.disconnect();
        } else {
            session.disconnect(message);
        }
    }

    private void disconnect() {
        disconnect(null);
    }

    @Override
    public PacketSignal handlePacket(BedrockPacket packet) {
        BedrockPacketHandler.super.handlePacket(packet);
        return PacketSignal.HANDLED; 
    }

    @Override
    public PacketSignal handle(RequestNetworkSettingsPacket packet) {
        int clientProtocolVersion = packet.getClientNetworkVersion();
        int serverProtocolVersion = Constants.BEDROCK_CODEC.getProtocolVersion();

        // The client normally prevents you connecting to a server with a different protocol number
        // But just incase they bypass that we double check here
        if (clientProtocolVersion != serverProtocolVersion) {
            session.disconnect(clientProtocolVersion > serverProtocolVersion ? "disconnectionScreen.outdatedServer" : "disconnectionScreen.outdatedClient");
            return PacketSignal.HANDLED;
        }

        session.setCodec(Constants.BEDROCK_CODEC);

        NetworkSettingsPacket networkSettingsPacket = new NetworkSettingsPacket();
        networkSettingsPacket.setCompressionThreshold(0);
        networkSettingsPacket.setCompressionAlgorithm(PacketCompressionAlgorithm.ZLIB);

        session.sendPacketImmediately(networkSettingsPacket);
        session.setCompression(PacketCompressionAlgorithm.ZLIB);

        networkSettingsRequested = true;
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(LoginPacket packet) {
        if (!networkSettingsRequested) {
            PlayStatusPacket statusPacket = new PlayStatusPacket();
            statusPacket.setStatus(PlayStatus.LOGIN_FAILED_CLIENT_OLD);
            session.sendPacket(statusPacket);

            disconnect();
            return PacketSignal.HANDLED;
        }

        PlayStatusPacket status = new PlayStatusPacket();
        status.setStatus(PlayStatus.LOGIN_SUCCESS);
        session.sendPacket(status);

        ResourcePacksInfoPacket info = new ResourcePacksInfoPacket();
        PackIdVersion worldTemplate = new PackIdVersion();
        worldTemplate.setPackUUID(UUID.randomUUID());
        worldTemplate.setPackVersion("*");
        info.setWorldTemplateIdAndVersion(worldTemplate);
        info.setForceDisableVibrantVisuals(true);
        info.setResourcePackRequired(false);
        session.sendPacket(info);

        try {
            ChainValidationResult result = EncryptionUtils.validatePayload(packet);
            if (!result.signed()) {
                throw new IllegalArgumentException("Chain is not signed");
            }
            PublicKey identityPublicKey = result.identityClaims().parsedIdentityPublicKey();

            byte[] clientDataPayload = EncryptionUtils.verifyClientData(packet.getClientJwt(), identityPublicKey);
            if (clientDataPayload == null) {
                throw new IllegalStateException("Client data isn't signed by the given chain data");
            }

            identityData = result.identityClaims().extraData;
        } catch (AssertionError | Exception error) {
            logger.debug("Failed to validate login packet: " + logger.getStackTrace(error));
            disconnect("disconnect.loginFailed");
        }
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ClientCacheStatusPacket packet) {
        return PacketSignal.HANDLED;
    }

    @Override
    public PacketSignal handle(ResourcePackClientResponsePacket packet) {
        switch (packet.getResponse()) {
            case RESOURCE_PACK_STACK_FINISHED:
                sendStartGame();
                break;
            case DOWNLOADING_FINISHED:
                ResourcePackStackPacket stack = new ResourcePackStackPacket();
                Experiments experiments = new Experiments();
                experiments.setExperimentsEverToggled(false);
                stack.setExperiments(experiments);
                stack.setTexturePackRequired(false);
                stack.setBaseGameVersion("*");
                session.sendPacket(stack);
                break;
            default:
                disconnect("disconnectionScreen.resourcePack");
                break;
        }
        return PacketSignal.HANDLED;
    }

    @SuppressWarnings("deprecation")
    public void sendStartGame() {
        StartGamePacket startGamePacket = new StartGamePacket();
        startGamePacket.setEntityID(1);
        startGamePacket.setRuntimeID(1);
        startGamePacket.setGameType(GameType.CREATIVE);
        startGamePacket.setPosition(Vector3f.from(0, 64 + 2, 0));
        startGamePacket.setRotation(Vector2f.ONE);
        startGamePacket.setPlayerPropertyData(NbtMap.EMPTY);

        SpawnSettings spawnSettings = new SpawnSettings();
        spawnSettings.setType(SpawnBiomeType.DEFAULT);
        spawnSettings.setUserDefinedBiomeName("");
        spawnSettings.setDimension(DimensionType.from(2));

        LevelSettings levelSettings = new LevelSettings();
        levelSettings.setSeed(0L);
        levelSettings.setSpawnSettings(spawnSettings);
        levelSettings.setGeneratorType(GeneratorType.OVERWORLD);
        levelSettings.setOverrideForceExperimentalGameplay(OptionalBoolean.empty());
        levelSettings.setGameType(GameType.CREATIVE);
        levelSettings.setGameDifficulty(Difficulty.PEACEFUL);
        levelSettings.setDefaultSpawnBlockPosition(Vector3i.ZERO);
        levelSettings.setAchievementsDisabled(true);
        levelSettings.setEditorWorldType(EditorWorldType.NON_EDITOR);
        levelSettings.setCreatedInEditor(false);
        levelSettings.setExportedFromEditor(false);
        levelSettings.setDayCycleStopTime(-1);
        levelSettings.setEducationEditionOffer(EducationEditionOffer.NONE);
        levelSettings.setEducationFeaturesEnabled(false);
        levelSettings.setEducationProductID("");
        levelSettings.setEduSharedUriResource(EduSharedUriResource.EMPTY);
        levelSettings.setRainLevel(0);
        levelSettings.setLightningLevel(0);
        levelSettings.setMultiplayerGameIntent(true);
        levelSettings.setLanBroadcastIntent(true);
        levelSettings.getRuleData().getRulesList().add(new GameRuleData<>("showcoordinates", false));
        levelSettings.setExperiments(new Experiments());
        levelSettings.setWereAnyExperimentsEverToggled(false);
        levelSettings.setPlatformBroadcastSetting(GamePublishSetting.PUBLIC);
        levelSettings.setXboxLiveBroadcastSetting(GamePublishSetting.PUBLIC);
        levelSettings.setCommandsEnabled(true);
        levelSettings.setChatRestrictionLevel(ChatRestrictionLevel.NONE);
        levelSettings.setTexturePacksRequired(false);
        levelSettings.setHasBonusChestEnabled(false);
        levelSettings.setStartWithMapEnabled(false);
        levelSettings.setTrustingPlayers(true);
        levelSettings.setPlayerPermissions(PlayerPermissionLevel.VISITOR);
        levelSettings.setServerChunkTickRange(4);
        levelSettings.setHasLockedBehaviorPack(false);
        levelSettings.setHasLockedResourcePack(false);
        levelSettings.setFromLockedTemplate(false);
        levelSettings.setUseMsaGamertagsOnly(false);
        levelSettings.setFromWorldTemplate(false);
        levelSettings.setWorldTemplateOptionLocked(false);
        levelSettings.setBaseGameVersion("*");
        levelSettings.setServerId("");
        levelSettings.setWorldId("");
        levelSettings.setScenarioId("");
        levelSettings.setOwnerId("");
        levelSettings.setServerEditorConnectionPolicy(ServerEditorConnectionPolicy.MATCH_WORLD_TYPE);
        startGamePacket.setSettings(levelSettings);

        startGamePacket.setLevelID("");
        startGamePacket.setLevelName("MCXboxBroadcast");
        startGamePacket.setTemplateContentIdentity("");
        startGamePacket.setWorldTemplateID(new UUID(0, 0));
        startGamePacket.setLevelCurrentTime(0);
        startGamePacket.setEnchantmentSeed(0);
        startGamePacket.setMultiplayerCorrelationId("");
        startGamePacket.setServerVersion("*");

        startGamePacket.setMovementSettings(new SyncedPlayerMovementSettings(
            ServerAuthMovementMode.SERVER_AUTHORITATIVE_V3,
            0,
            false
        ));

        ServerTelemetryData telemetryData = new ServerTelemetryData();
        telemetryData.setServerId("");
        telemetryData.setScenarioId("");
        telemetryData.setWorldId("");
        telemetryData.setOwnerId("");
        startGamePacket.setServerTelemetryData(telemetryData);

        session.sendPacket(startGamePacket);

        TransferPacket transferPacket = new TransferPacket();
        transferPacket.setServerAddress(sessionInfo.getIp());
        transferPacket.setServerPort(sessionInfo.getPort());
        session.sendPacket(transferPacket);

        try {
            if (identityData != null) {
                sessionManager.logger().info("Transferred bedrock client " + identityData.displayName + " (" + identityData.xuid + ") to target server.");
                sessionManager.storageManager().playerHistory().lastSeen(identityData.xuid, Instant.now());
            }
        } catch (IOException ignored) { }
    }
}
