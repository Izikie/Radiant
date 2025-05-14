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
import org.apache.logging.log4j.LogManager;

import java.lang.reflect.InvocationTargetException;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public enum NetworkState {
    HANDSHAKING(-1) {
        {
            this.registerPacket(PacketDirection.SERVERBOUND, C00Handshake.class);
        }
    },
    PLAY(0) {
        {
            this.registerPacket(PacketDirection.CLIENTBOUND, S00PacketKeepAlive.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S01PacketJoinGame.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S02PacketChat.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S03PacketTimeUpdate.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S04PacketEntityEquipment.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S05PacketSpawnPosition.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S06PacketUpdateHealth.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S07PacketRespawn.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S08PacketPlayerPosLook.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S09PacketHeldItemChange.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S0APacketUseBed.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S0BPacketAnimation.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S0CPacketSpawnPlayer.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S0DPacketCollectItem.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S0EPacketSpawnObject.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S0FPacketSpawnMob.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S10PacketSpawnPainting.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S11PacketSpawnExperienceOrb.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S12PacketEntityVelocity.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S13PacketDestroyEntities.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S14PacketEntity.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S14PacketEntity.S15PacketEntityRelMove.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S14PacketEntity.S16PacketEntityLook.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S14PacketEntity.S17PacketEntityLookMove.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S18PacketEntityTeleport.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S19PacketEntityHeadLook.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S19PacketEntityStatus.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S1BPacketEntityAttach.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S1CPacketEntityMetadata.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S1DPacketEntityEffect.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S1EPacketRemoveEntityEffect.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S1FPacketSetExperience.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S20PacketEntityProperties.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S21PacketChunkData.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S22PacketMultiBlockChange.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S23PacketBlockChange.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S24PacketBlockAction.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S25PacketBlockBreakAnim.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S26PacketMapChunkBulk.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S27PacketExplosion.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S28PacketEffect.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S29PacketSoundEffect.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S2APacketParticles.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S2BPacketChangeGameState.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S2CPacketSpawnGlobalEntity.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S2DPacketOpenWindow.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S2EPacketCloseWindow.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S2FPacketSetSlot.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S30PacketWindowItems.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S31PacketWindowProperty.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S32PacketConfirmTransaction.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S33PacketUpdateSign.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S34PacketMaps.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S35PacketUpdateTileEntity.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S36PacketSignEditorOpen.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S37PacketStatistics.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S38PacketPlayerListItem.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S39PacketPlayerAbilities.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S3APacketTabComplete.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S3BPacketScoreboardObjective.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S3CPacketUpdateScore.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S3DPacketDisplayScoreboard.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S3EPacketTeams.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S3FPacketCustomPayload.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S40PacketDisconnect.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S41PacketServerDifficulty.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S42PacketCombatEvent.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S43PacketCamera.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S44PacketWorldBorder.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S45PacketTitle.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S46PacketSetCompressionLevel.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S47PacketPlayerListHeaderFooter.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S48PacketResourcePackSend.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S49PacketUpdateEntityNBT.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C00PacketKeepAlive.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C01PacketChatMessage.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C02PacketUseEntity.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C03PacketPlayer.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C03PacketPlayer.C04PacketPlayerPosition.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C03PacketPlayer.C05PacketPlayerLook.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C03PacketPlayer.C06PacketPlayerPosLook.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C07PacketPlayerDigging.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C08PacketPlayerBlockPlacement.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C09PacketHeldItemChange.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C0APacketAnimation.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C0BPacketEntityAction.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C0CPacketInput.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C0DPacketCloseWindow.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C0EPacketClickWindow.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C0FPacketConfirmTransaction.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C10PacketCreativeInventoryAction.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C11PacketEnchantItem.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C12PacketUpdateSign.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C13PacketPlayerAbilities.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C14PacketTabComplete.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C15PacketClientSettings.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C16PacketClientStatus.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C17PacketCustomPayload.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C18PacketSpectate.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C19PacketResourcePackStatus.class);
        }
    },
    STATUS(1) {
        {
            this.registerPacket(PacketDirection.SERVERBOUND, C00PacketServerQuery.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S00PacketServerInfo.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C01PacketPing.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S01PacketPong.class);
        }
    },
    LOGIN(2) {
        {
            this.registerPacket(PacketDirection.CLIENTBOUND, S00PacketDisconnect.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S01PacketEncryptionRequest.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S02PacketLoginSuccess.class);
            this.registerPacket(PacketDirection.CLIENTBOUND, S03PacketEnableCompression.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C00PacketLoginStart.class);
            this.registerPacket(PacketDirection.SERVERBOUND, C01PacketEncryptionResponse.class);
        }
    };

    private static final int field_181136_e = -1;
    private static final int field_181137_f = 2;
    private static final NetworkState[] STATES_BY_ID = new NetworkState[field_181137_f - field_181136_e + 1];
    private static final Map<Class<? extends Packet>, NetworkState> STATES_BY_CLASS = new HashMap<>();
    private final int id;
    private final Map<PacketDirection, BiMap<Integer, Class<? extends Packet>>> directionMaps;

    NetworkState(int protocolId) {
        this.directionMaps = new EnumMap<>(PacketDirection.class);
        this.id = protocolId;
    }

    protected NetworkState registerPacket(PacketDirection direction, Class<? extends Packet> packetClass) {
        BiMap<Integer, Class<? extends Packet>> bimap = this.directionMaps.computeIfAbsent(direction, k -> HashBiMap.create());

        if (bimap.containsValue(packetClass)) {
            String s = direction + " packet " + packetClass + " is already known to ID " + bimap.inverse().get(packetClass);
            LogManager.getLogger().fatal(s);
            throw new IllegalArgumentException(s);
        } else {
            bimap.put(bimap.size(), packetClass);
            return this;
        }
    }

    public Integer getPacketId(PacketDirection direction, Packet packetIn) {
        return (Integer) ((BiMap) this.directionMaps.get(direction)).inverse().get(packetIn.getClass());
    }

    public Packet getPacket(PacketDirection direction, int packetId) throws InstantiationException, IllegalAccessException, NoSuchMethodException, InvocationTargetException {
        Class<? extends Packet> oclass = (Class) ((BiMap) this.directionMaps.get(direction)).get(packetId);
        return oclass == null ? null : oclass.getDeclaredConstructor().newInstance();
    }

    public int getId() {
        return this.id;
    }

    public static NetworkState getById(int stateId) {
        return stateId >= field_181136_e && stateId <= field_181137_f ? STATES_BY_ID[stateId - field_181136_e] : null;
    }

    public static NetworkState getFromPacket(Packet packetIn) {
        return STATES_BY_CLASS.get(packetIn.getClass());
    }

    static {
        for (NetworkState enumconnectionstate : values()) {
            int i = enumconnectionstate.getId();

            if (i < field_181136_e || i > field_181137_f) {
                throw new Error("Invalid protocol ID " + i);
            }

            STATES_BY_ID[i - field_181136_e] = enumconnectionstate;

            for (PacketDirection enumpacketdirection : enumconnectionstate.directionMaps.keySet()) {
                for (Class<? extends Packet> oclass : (enumconnectionstate.directionMaps.get(enumpacketdirection)).values()) {
                    if (STATES_BY_CLASS.containsKey(oclass) && STATES_BY_CLASS.get(oclass) != enumconnectionstate) {
                        throw new Error("Packet " + oclass + " is already assigned to protocol " + STATES_BY_CLASS.get(oclass) + " - can't reassign to " + enumconnectionstate);
                    }

                    try {
                        oclass.getConstructor().newInstance();
                    } catch (Throwable var10) {
                        throw new Error("Packet " + oclass + " fails instantiation checks! " + oclass);
                    }

                    STATES_BY_CLASS.put(oclass, enumconnectionstate);
                }
            }
        }
    }
}
