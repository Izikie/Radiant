package net.minecraft.network;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import net.minecraft.network.handshake.client.C00Handshake;
import net.minecraft.network.login.client.C00PacketLoginStart;
import net.minecraft.network.login.client.C01PacketEncryptionResponse;
import net.minecraft.network.login.server.S00PacketDisconnect;
import net.minecraft.network.login.server.S01PacketEncryptionRequest;
import net.minecraft.network.login.server.S02PacketLoginSuccess;
import net.minecraft.network.login.server.S03PacketEnableCompression;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.network.status.client.C00PacketServerQuery;
import net.minecraft.network.status.client.C01PacketPing;
import net.minecraft.network.status.server.S00PacketServerInfo;
import net.minecraft.network.status.server.S01PacketPong;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public enum NetworkState {
    HANDSHAKING(-1) {
        {
            this.registerPacket(PacketDirection.OUTGOING, C00Handshake.class);
        }
    },
    PLAY(0) {
        {
            this.registerPacket(PacketDirection.INCOMING, S00PacketKeepAlive.class);
            this.registerPacket(PacketDirection.INCOMING, S01PacketJoinGame.class);
            this.registerPacket(PacketDirection.INCOMING, S02PacketChat.class);
            this.registerPacket(PacketDirection.INCOMING, S03PacketTimeUpdate.class);
            this.registerPacket(PacketDirection.INCOMING, S04PacketEntityEquipment.class);
            this.registerPacket(PacketDirection.INCOMING, S05PacketSpawnPosition.class);
            this.registerPacket(PacketDirection.INCOMING, S06PacketUpdateHealth.class);
            this.registerPacket(PacketDirection.INCOMING, S07PacketRespawn.class);
            this.registerPacket(PacketDirection.INCOMING, S08PacketPlayerPosLook.class);
            this.registerPacket(PacketDirection.INCOMING, S09PacketHeldItemChange.class);
            this.registerPacket(PacketDirection.INCOMING, S0APacketUseBed.class);
            this.registerPacket(PacketDirection.INCOMING, S0BPacketAnimation.class);
            this.registerPacket(PacketDirection.INCOMING, S0CPacketSpawnPlayer.class);
            this.registerPacket(PacketDirection.INCOMING, S0DPacketCollectItem.class);
            this.registerPacket(PacketDirection.INCOMING, S0EPacketSpawnObject.class);
            this.registerPacket(PacketDirection.INCOMING, S0FPacketSpawnMob.class);
            this.registerPacket(PacketDirection.INCOMING, S10PacketSpawnPainting.class);
            this.registerPacket(PacketDirection.INCOMING, S11PacketSpawnExperienceOrb.class);
            this.registerPacket(PacketDirection.INCOMING, S12PacketEntityVelocity.class);
            this.registerPacket(PacketDirection.INCOMING, S13PacketDestroyEntities.class);
            this.registerPacket(PacketDirection.INCOMING, S14PacketEntity.class);
            this.registerPacket(PacketDirection.INCOMING, S14PacketEntity.S15PacketEntityRelMove.class);
            this.registerPacket(PacketDirection.INCOMING, S14PacketEntity.S16PacketEntityLook.class);
            this.registerPacket(PacketDirection.INCOMING, S14PacketEntity.S17PacketEntityLookMove.class);
            this.registerPacket(PacketDirection.INCOMING, S18PacketEntityTeleport.class);
            this.registerPacket(PacketDirection.INCOMING, S19PacketEntityHeadLook.class);
            this.registerPacket(PacketDirection.INCOMING, S19PacketEntityStatus.class);
            this.registerPacket(PacketDirection.INCOMING, S1BPacketEntityAttach.class);
            this.registerPacket(PacketDirection.INCOMING, S1CPacketEntityMetadata.class);
            this.registerPacket(PacketDirection.INCOMING, S1DPacketEntityEffect.class);
            this.registerPacket(PacketDirection.INCOMING, S1EPacketRemoveEntityEffect.class);
            this.registerPacket(PacketDirection.INCOMING, S1FPacketSetExperience.class);
            this.registerPacket(PacketDirection.INCOMING, S20PacketEntityProperties.class);
            this.registerPacket(PacketDirection.INCOMING, S21PacketChunkData.class);
            this.registerPacket(PacketDirection.INCOMING, S22PacketMultiBlockChange.class);
            this.registerPacket(PacketDirection.INCOMING, S23PacketBlockChange.class);
            this.registerPacket(PacketDirection.INCOMING, S24PacketBlockAction.class);
            this.registerPacket(PacketDirection.INCOMING, S25PacketBlockBreakAnim.class);
            this.registerPacket(PacketDirection.INCOMING, S26PacketMapChunkBulk.class);
            this.registerPacket(PacketDirection.INCOMING, S27PacketExplosion.class);
            this.registerPacket(PacketDirection.INCOMING, S28PacketEffect.class);
            this.registerPacket(PacketDirection.INCOMING, S29PacketSoundEffect.class);
            this.registerPacket(PacketDirection.INCOMING, S2APacketParticles.class);
            this.registerPacket(PacketDirection.INCOMING, S2BPacketChangeGameState.class);
            this.registerPacket(PacketDirection.INCOMING, S2CPacketSpawnGlobalEntity.class);
            this.registerPacket(PacketDirection.INCOMING, S2DPacketOpenWindow.class);
            this.registerPacket(PacketDirection.INCOMING, S2EPacketCloseWindow.class);
            this.registerPacket(PacketDirection.INCOMING, S2FPacketSetSlot.class);
            this.registerPacket(PacketDirection.INCOMING, S30PacketWindowItems.class);
            this.registerPacket(PacketDirection.INCOMING, S31PacketWindowProperty.class);
            this.registerPacket(PacketDirection.INCOMING, S32PacketConfirmTransaction.class);
            this.registerPacket(PacketDirection.INCOMING, S33PacketUpdateSign.class);
            this.registerPacket(PacketDirection.INCOMING, S34PacketMaps.class);
            this.registerPacket(PacketDirection.INCOMING, S35PacketUpdateTileEntity.class);
            this.registerPacket(PacketDirection.INCOMING, S36PacketSignEditorOpen.class);
            this.registerPacket(PacketDirection.INCOMING, S37PacketStatistics.class);
            this.registerPacket(PacketDirection.INCOMING, S38PacketPlayerListItem.class);
            this.registerPacket(PacketDirection.INCOMING, S39PacketPlayerAbilities.class);
            this.registerPacket(PacketDirection.INCOMING, S3APacketTabComplete.class);
            this.registerPacket(PacketDirection.INCOMING, S3BPacketScoreboardObjective.class);
            this.registerPacket(PacketDirection.INCOMING, S3CPacketUpdateScore.class);
            this.registerPacket(PacketDirection.INCOMING, S3DPacketDisplayScoreboard.class);
            this.registerPacket(PacketDirection.INCOMING, S3EPacketTeams.class);
            this.registerPacket(PacketDirection.INCOMING, S3FPacketCustomPayload.class);
            this.registerPacket(PacketDirection.INCOMING, S40PacketDisconnect.class);
            this.registerPacket(PacketDirection.INCOMING, S41PacketServerDifficulty.class);
            this.registerPacket(PacketDirection.INCOMING, S42PacketCombatEvent.class);
            this.registerPacket(PacketDirection.INCOMING, S43PacketCamera.class);
            this.registerPacket(PacketDirection.INCOMING, S44PacketWorldBorder.class);
            this.registerPacket(PacketDirection.INCOMING, S45PacketTitle.class);
            this.registerPacket(PacketDirection.INCOMING, S46PacketSetCompressionLevel.class);
            this.registerPacket(PacketDirection.INCOMING, S47PacketPlayerListHeaderFooter.class);
            this.registerPacket(PacketDirection.INCOMING, S48PacketResourcePackSend.class);
            this.registerPacket(PacketDirection.INCOMING, S49PacketUpdateEntityNBT.class);
            this.registerPacket(PacketDirection.OUTGOING, C00PacketKeepAlive.class);
            this.registerPacket(PacketDirection.OUTGOING, C01PacketChatMessage.class);
            this.registerPacket(PacketDirection.OUTGOING, C02PacketUseEntity.class);
            this.registerPacket(PacketDirection.OUTGOING, C03PacketPlayer.class);
            this.registerPacket(PacketDirection.OUTGOING, C03PacketPlayer.C04PacketPlayerPosition.class);
            this.registerPacket(PacketDirection.OUTGOING, C03PacketPlayer.C05PacketPlayerLook.class);
            this.registerPacket(PacketDirection.OUTGOING, C03PacketPlayer.C06PacketPlayerPosLook.class);
            this.registerPacket(PacketDirection.OUTGOING, C07PacketPlayerDigging.class);
            this.registerPacket(PacketDirection.OUTGOING, C08PacketPlayerBlockPlacement.class);
            this.registerPacket(PacketDirection.OUTGOING, C09PacketHeldItemChange.class);
            this.registerPacket(PacketDirection.OUTGOING, C0APacketAnimation.class);
            this.registerPacket(PacketDirection.OUTGOING, C0BPacketEntityAction.class);
            this.registerPacket(PacketDirection.OUTGOING, C0CPacketInput.class);
            this.registerPacket(PacketDirection.OUTGOING, C0DPacketCloseWindow.class);
            this.registerPacket(PacketDirection.OUTGOING, C0EPacketClickWindow.class);
            this.registerPacket(PacketDirection.OUTGOING, C0FPacketConfirmTransaction.class);
            this.registerPacket(PacketDirection.OUTGOING, C10PacketCreativeInventoryAction.class);
            this.registerPacket(PacketDirection.OUTGOING, C11PacketEnchantItem.class);
            this.registerPacket(PacketDirection.OUTGOING, C12PacketUpdateSign.class);
            this.registerPacket(PacketDirection.OUTGOING, C13PacketPlayerAbilities.class);
            this.registerPacket(PacketDirection.OUTGOING, C14PacketTabComplete.class);
            this.registerPacket(PacketDirection.OUTGOING, C15PacketClientSettings.class);
            this.registerPacket(PacketDirection.OUTGOING, C16PacketClientStatus.class);
            this.registerPacket(PacketDirection.OUTGOING, C17PacketCustomPayload.class);
            this.registerPacket(PacketDirection.OUTGOING, C18PacketSpectate.class);
            this.registerPacket(PacketDirection.OUTGOING, C19PacketResourcePackStatus.class);
        }
    },
    STATUS(1) {
        {
            this.registerPacket(PacketDirection.OUTGOING, C00PacketServerQuery.class);
            this.registerPacket(PacketDirection.INCOMING, S00PacketServerInfo.class);
            this.registerPacket(PacketDirection.OUTGOING, C01PacketPing.class);
            this.registerPacket(PacketDirection.INCOMING, S01PacketPong.class);
        }
    },
    LOGIN(2) {
        {
            this.registerPacket(PacketDirection.INCOMING, S00PacketDisconnect.class);
            this.registerPacket(PacketDirection.INCOMING, S01PacketEncryptionRequest.class);
            this.registerPacket(PacketDirection.INCOMING, S02PacketLoginSuccess.class);
            this.registerPacket(PacketDirection.INCOMING, S03PacketEnableCompression.class);
            this.registerPacket(PacketDirection.OUTGOING, C00PacketLoginStart.class);
            this.registerPacket(PacketDirection.OUTGOING, C01PacketEncryptionResponse.class);
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(NetworkState.class);

    private static final int STATE_ID_MIN = -1;
    private static final int STATE_ID_MAX = 2;
    private static final NetworkState[] STATES_BY_ID = new NetworkState[STATE_ID_MAX - STATE_ID_MIN + 1];
    private static final Map<Class<? extends Packet<?>>, NetworkState> STATES_BY_CLASS = new HashMap<>();
    private final int id;
    private final Map<PacketDirection, BiMap<Integer, Class<? extends Packet<?>>>> directionMaps;

    NetworkState(int protocolId) {
        this.directionMaps = new EnumMap<>(PacketDirection.class);
        this.id = protocolId;
    }

    protected void registerPacket(PacketDirection direction, Class<? extends Packet<?>> packetClass) {
        BiMap<Integer, Class<? extends Packet<?>>> bimap = this.directionMaps.computeIfAbsent(direction, k -> HashBiMap.create());

        if (bimap.containsValue(packetClass)) {
            String s = direction + " packet " + packetClass + " is already known to ID " + bimap.inverse().get(packetClass);
            LOGGER.error(s);
            throw new IllegalArgumentException(s);
        } else {
            bimap.put(bimap.size(), packetClass);
        }
    }

    public Integer getPacketId(PacketDirection direction, Packet<?> packet) {
        return this.directionMaps.get(direction).inverse().get(packet.getClass());
    }

    public Packet<?> getPacket(PacketDirection direction, int packetId) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<? extends Packet<?>> oclass = this.directionMaps.get(direction).get(packetId);
        return oclass == null ? null : oclass.getDeclaredConstructor().newInstance();
    }

    public Class<? extends Packet<?>> getPacketClass(PacketDirection direction, int packetId) {
        return this.directionMaps.get(direction).get(packetId);
    }

    public int getId() {
        return this.id;
    }

    public static NetworkState getById(int stateId) {
        return stateId >= STATE_ID_MIN && stateId <= STATE_ID_MAX ? STATES_BY_ID[stateId - STATE_ID_MIN] : null;
    }

    public static NetworkState getFromPacket(Packet<?> packet) {
        return STATES_BY_CLASS.get(packet.getClass());
    }

    static {
        for (NetworkState connectionState : values()) {
            int stateID = connectionState.getId();

            if (stateID < STATE_ID_MIN || stateID > STATE_ID_MAX) {
                throw new Error("Invalid protocol ID " + stateID);
            }

            STATES_BY_ID[stateID - STATE_ID_MIN] = connectionState;

            for (PacketDirection direction : connectionState.directionMaps.keySet()) {
                for (Class<? extends Packet<?>> oclass : (connectionState.directionMaps.get(direction)).values()) {
                    if (STATES_BY_CLASS.containsKey(oclass) && STATES_BY_CLASS.get(oclass) != connectionState) {
                        throw new Error("Packet " + oclass + " is already assigned to protocol " + STATES_BY_CLASS.get(oclass) + " - can't reassign to " + connectionState);
                    }

                    try {
                        oclass.getConstructor().newInstance();
                    } catch (Throwable ignore) {
                        throw new Error("Packet " + oclass + " fails instantiation checks! " + oclass);
                    }

                    STATES_BY_CLASS.put(oclass, connectionState);
                }
            }
        }
    }
}
