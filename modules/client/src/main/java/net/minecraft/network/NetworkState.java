package net.minecraft.network;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketDirection;
import net.minecraft.network.packet.impl.handshake.client.C00Handshake;
import net.minecraft.network.packet.impl.login.client.*;
import net.minecraft.network.packet.impl.login.server.*;
import net.minecraft.network.packet.impl.play.client.*;
import net.minecraft.network.packet.impl.play.server.*;
import net.minecraft.network.packet.impl.status.client.*;
import net.minecraft.network.packet.impl.status.server.*;

import java.util.function.Supplier;

public enum NetworkState {
    HANDSHAKING,
    PLAY,
    STATUS,
    LOGIN;

    private static final NetworkState[] STATES_BY_ID = new NetworkState[values().length];
    private static final Reference2ObjectOpenHashMap<Class<? extends Packet<?>>, NetworkState> PACKET_TO_STATE = new Reference2ObjectOpenHashMap<>();

    private final int id;
    private final PacketRegistry[] registries = new PacketRegistry[PacketDirection.values().length];

    NetworkState() {
        this.id = this.ordinal() - 1;

        for (PacketDirection direction : PacketDirection.values()) {
            this.registries[direction.ordinal()] = new PacketRegistry();
        }
    }

    private <T extends Packet<?>> NetworkState reg(PacketDirection direction, Class<T> clazz, Supplier<T> constructor) {
        this.registries[direction.ordinal()].add(clazz, constructor);
        PACKET_TO_STATE.put(clazz, this);

        return this;
    }

    public static NetworkState getById(int stateId) {
        int index = stateId + 1;
        if (index < 0 || index >= STATES_BY_ID.length) {
            return null;
        }

        return STATES_BY_ID[index];
    }

    public static NetworkState getFromPacket(Packet<?> packet) {
        return PACKET_TO_STATE.get(packet.getClass());
    }

    public Integer getPacketId(PacketDirection direction, Packet<?> packet) {
        return this.registries[direction.ordinal()].getId(packet.getClass());
    }

    public Packet<?> getPacket(PacketDirection dir, int packetId) {
        return this.registries[dir.ordinal()].instantiate(packetId);
    }

    public int getId() {
        return this.id;
    }

    private static class PacketRegistry {
        private final Reference2IntOpenHashMap<Class<? extends Packet<?>>> classToId = new Reference2IntOpenHashMap<>();
        private final Int2ObjectOpenHashMap<Supplier<? extends Packet<?>>> idToSupplier = new Int2ObjectOpenHashMap<>();
        private int nextId = 0;

        public PacketRegistry() {
            classToId.defaultReturnValue(-1);
        }

        public <T extends Packet<?>> void add(Class<T> clazz, Supplier<T> constructor) {
            int packetId = nextId++;

            classToId.put(clazz, packetId);
            idToSupplier.put(packetId, constructor);
        }

        public Integer getId(Class<?> clazz) {
            int id = classToId.getInt(clazz);

            return id == -1 ? null : id;
        }

        public Packet<?> instantiate(int id) {
            Supplier<? extends Packet<?>> supplier = idToSupplier.get(id);

            return supplier != null ? supplier.get() : null;
        }

        public void optimize() {
            classToId.trim();
            idToSupplier.trim();
        }
    }

    static {
        for (NetworkState state : values()) {
            STATES_BY_ID[state.id + 1] = state;
        }

        HANDSHAKING.reg(PacketDirection.OUTGOING, C00Handshake.class, C00Handshake::new);

        PLAY.reg(PacketDirection.INCOMING, S00PacketKeepAlive.class, S00PacketKeepAlive::new)
                .reg(PacketDirection.INCOMING, S01PacketJoinGame.class, S01PacketJoinGame::new)
                .reg(PacketDirection.INCOMING, S02PacketChat.class, S02PacketChat::new)
                .reg(PacketDirection.INCOMING, S03PacketTimeUpdate.class, S03PacketTimeUpdate::new)
                .reg(PacketDirection.INCOMING, S04PacketEntityEquipment.class, S04PacketEntityEquipment::new)
                .reg(PacketDirection.INCOMING, S05PacketSpawnPosition.class, S05PacketSpawnPosition::new)
                .reg(PacketDirection.INCOMING, S06PacketUpdateHealth.class, S06PacketUpdateHealth::new)
                .reg(PacketDirection.INCOMING, S07PacketRespawn.class, S07PacketRespawn::new)
                .reg(PacketDirection.INCOMING, S08PacketPlayerPosLook.class, S08PacketPlayerPosLook::new)
                .reg(PacketDirection.INCOMING, S09PacketHeldItemChange.class, S09PacketHeldItemChange::new)
                .reg(PacketDirection.INCOMING, S0APacketUseBed.class, S0APacketUseBed::new)
                .reg(PacketDirection.INCOMING, S0BPacketAnimation.class, S0BPacketAnimation::new)
                .reg(PacketDirection.INCOMING, S0CPacketSpawnPlayer.class, S0CPacketSpawnPlayer::new)
                .reg(PacketDirection.INCOMING, S0DPacketCollectItem.class, S0DPacketCollectItem::new)
                .reg(PacketDirection.INCOMING, S0EPacketSpawnObject.class, S0EPacketSpawnObject::new)
                .reg(PacketDirection.INCOMING, S0FPacketSpawnMob.class, S0FPacketSpawnMob::new)
                .reg(PacketDirection.INCOMING, S10PacketSpawnPainting.class, S10PacketSpawnPainting::new)
                .reg(PacketDirection.INCOMING, S11PacketSpawnExperienceOrb.class, S11PacketSpawnExperienceOrb::new)
                .reg(PacketDirection.INCOMING, S12PacketEntityVelocity.class, S12PacketEntityVelocity::new)
                .reg(PacketDirection.INCOMING, S13PacketDestroyEntities.class, S13PacketDestroyEntities::new)
                .reg(PacketDirection.INCOMING, S14PacketEntity.class, S14PacketEntity::new)
                .reg(PacketDirection.INCOMING, S14PacketEntity.S15PacketEntityRelMove.class, S14PacketEntity.S15PacketEntityRelMove::new)
                .reg(PacketDirection.INCOMING, S14PacketEntity.S16PacketEntityLook.class, S14PacketEntity.S16PacketEntityLook::new)
                .reg(PacketDirection.INCOMING, S14PacketEntity.S17PacketEntityLookMove.class, S14PacketEntity.S17PacketEntityLookMove::new)
                .reg(PacketDirection.INCOMING, S18PacketEntityTeleport.class, S18PacketEntityTeleport::new)
                .reg(PacketDirection.INCOMING, S19PacketEntityHeadLook.class, S19PacketEntityHeadLook::new)
                .reg(PacketDirection.INCOMING, S19PacketEntityStatus.class, S19PacketEntityStatus::new)
                .reg(PacketDirection.INCOMING, S1BPacketEntityAttach.class, S1BPacketEntityAttach::new)
                .reg(PacketDirection.INCOMING, S1CPacketEntityMetadata.class, S1CPacketEntityMetadata::new)
                .reg(PacketDirection.INCOMING, S1DPacketEntityEffect.class, S1DPacketEntityEffect::new)
                .reg(PacketDirection.INCOMING, S1EPacketRemoveEntityEffect.class, S1EPacketRemoveEntityEffect::new)
                .reg(PacketDirection.INCOMING, S1FPacketSetExperience.class, S1FPacketSetExperience::new)
                .reg(PacketDirection.INCOMING, S20PacketEntityProperties.class, S20PacketEntityProperties::new)
                .reg(PacketDirection.INCOMING, S21PacketChunkData.class, S21PacketChunkData::new)
                .reg(PacketDirection.INCOMING, S22PacketMultiBlockChange.class, S22PacketMultiBlockChange::new)
                .reg(PacketDirection.INCOMING, S23PacketBlockChange.class, S23PacketBlockChange::new)
                .reg(PacketDirection.INCOMING, S24PacketBlockAction.class, S24PacketBlockAction::new)
                .reg(PacketDirection.INCOMING, S25PacketBlockBreakAnim.class, S25PacketBlockBreakAnim::new)
                .reg(PacketDirection.INCOMING, S26PacketMapChunkBulk.class, S26PacketMapChunkBulk::new)
                .reg(PacketDirection.INCOMING, S27PacketExplosion.class, S27PacketExplosion::new)
                .reg(PacketDirection.INCOMING, S28PacketEffect.class, S28PacketEffect::new)
                .reg(PacketDirection.INCOMING, S29PacketSoundEffect.class, S29PacketSoundEffect::new)
                .reg(PacketDirection.INCOMING, S2APacketParticles.class, S2APacketParticles::new)
                .reg(PacketDirection.INCOMING, S2BPacketChangeGameState.class, S2BPacketChangeGameState::new)
                .reg(PacketDirection.INCOMING, S2CPacketSpawnGlobalEntity.class, S2CPacketSpawnGlobalEntity::new)
                .reg(PacketDirection.INCOMING, S2DPacketOpenWindow.class, S2DPacketOpenWindow::new)
                .reg(PacketDirection.INCOMING, S2EPacketCloseWindow.class, S2EPacketCloseWindow::new)
                .reg(PacketDirection.INCOMING, S2FPacketSetSlot.class, S2FPacketSetSlot::new)
                .reg(PacketDirection.INCOMING, S30PacketWindowItems.class, S30PacketWindowItems::new)
                .reg(PacketDirection.INCOMING, S31PacketWindowProperty.class, S31PacketWindowProperty::new)
                .reg(PacketDirection.INCOMING, S32PacketConfirmTransaction.class, S32PacketConfirmTransaction::new)
                .reg(PacketDirection.INCOMING, S33PacketUpdateSign.class, S33PacketUpdateSign::new)
                .reg(PacketDirection.INCOMING, S34PacketMaps.class, S34PacketMaps::new)
                .reg(PacketDirection.INCOMING, S35PacketUpdateTileEntity.class, S35PacketUpdateTileEntity::new)
                .reg(PacketDirection.INCOMING, S36PacketSignEditorOpen.class, S36PacketSignEditorOpen::new)
                .reg(PacketDirection.INCOMING, S37PacketStatistics.class, S37PacketStatistics::new)
                .reg(PacketDirection.INCOMING, S38PacketPlayerListItem.class, S38PacketPlayerListItem::new)
                .reg(PacketDirection.INCOMING, S39PacketPlayerAbilities.class, S39PacketPlayerAbilities::new)
                .reg(PacketDirection.INCOMING, S3APacketTabComplete.class, S3APacketTabComplete::new)
                .reg(PacketDirection.INCOMING, S3BPacketScoreboardObjective.class, S3BPacketScoreboardObjective::new)
                .reg(PacketDirection.INCOMING, S3CPacketUpdateScore.class, S3CPacketUpdateScore::new)
                .reg(PacketDirection.INCOMING, S3DPacketDisplayScoreboard.class, S3DPacketDisplayScoreboard::new)
                .reg(PacketDirection.INCOMING, S3EPacketTeams.class, S3EPacketTeams::new)
                .reg(PacketDirection.INCOMING, S3FPacketCustomPayload.class, S3FPacketCustomPayload::new)
                .reg(PacketDirection.INCOMING, S40PacketDisconnect.class, S40PacketDisconnect::new)
                .reg(PacketDirection.INCOMING, S41PacketServerDifficulty.class, S41PacketServerDifficulty::new)
                .reg(PacketDirection.INCOMING, S42PacketCombatEvent.class, S42PacketCombatEvent::new)
                .reg(PacketDirection.INCOMING, S43PacketCamera.class, S43PacketCamera::new)
                .reg(PacketDirection.INCOMING, S44PacketWorldBorder.class, S44PacketWorldBorder::new)
                .reg(PacketDirection.INCOMING, S45PacketTitle.class, S45PacketTitle::new)
                .reg(PacketDirection.INCOMING, S46PacketSetCompressionLevel.class, S46PacketSetCompressionLevel::new)
                .reg(PacketDirection.INCOMING, S47PacketPlayerListHeaderFooter.class, S47PacketPlayerListHeaderFooter::new)
                .reg(PacketDirection.INCOMING, S48PacketResourcePackSend.class, S48PacketResourcePackSend::new)
                .reg(PacketDirection.INCOMING, S49PacketUpdateEntityNBT.class, S49PacketUpdateEntityNBT::new)
                .reg(PacketDirection.OUTGOING, C00PacketKeepAlive.class, C00PacketKeepAlive::new)
                .reg(PacketDirection.OUTGOING, C01PacketChatMessage.class, C01PacketChatMessage::new)
                .reg(PacketDirection.OUTGOING, C02PacketUseEntity.class, C02PacketUseEntity::new)
                .reg(PacketDirection.OUTGOING, C03PacketPlayer.class, C03PacketPlayer::new)
                .reg(PacketDirection.OUTGOING, C03PacketPlayer.C04PacketPlayerPosition.class, C03PacketPlayer.C04PacketPlayerPosition::new)
                .reg(PacketDirection.OUTGOING, C03PacketPlayer.C05PacketPlayerLook.class, C03PacketPlayer.C05PacketPlayerLook::new)
                .reg(PacketDirection.OUTGOING, C03PacketPlayer.C06PacketPlayerPosLook.class, C03PacketPlayer.C06PacketPlayerPosLook::new)
                .reg(PacketDirection.OUTGOING, C07PacketPlayerDigging.class, C07PacketPlayerDigging::new)
                .reg(PacketDirection.OUTGOING, C08PacketPlayerBlockPlacement.class, C08PacketPlayerBlockPlacement::new)
                .reg(PacketDirection.OUTGOING, C09PacketHeldItemChange.class, C09PacketHeldItemChange::new)
                .reg(PacketDirection.OUTGOING, C0APacketAnimation.class, C0APacketAnimation::new)
                .reg(PacketDirection.OUTGOING, C0BPacketEntityAction.class, C0BPacketEntityAction::new)
                .reg(PacketDirection.OUTGOING, C0CPacketInput.class, C0CPacketInput::new)
                .reg(PacketDirection.OUTGOING, C0DPacketCloseWindow.class, C0DPacketCloseWindow::new)
                .reg(PacketDirection.OUTGOING, C0EPacketClickWindow.class, C0EPacketClickWindow::new)
                .reg(PacketDirection.OUTGOING, C0FPacketConfirmTransaction.class, C0FPacketConfirmTransaction::new)
                .reg(PacketDirection.OUTGOING, C10PacketCreativeInventoryAction.class, C10PacketCreativeInventoryAction::new)
                .reg(PacketDirection.OUTGOING, C11PacketEnchantItem.class, C11PacketEnchantItem::new)
                .reg(PacketDirection.OUTGOING, C12PacketUpdateSign.class, C12PacketUpdateSign::new)
                .reg(PacketDirection.OUTGOING, C13PacketPlayerAbilities.class, C13PacketPlayerAbilities::new)
                .reg(PacketDirection.OUTGOING, C14PacketTabComplete.class, C14PacketTabComplete::new)
                .reg(PacketDirection.OUTGOING, C15PacketClientSettings.class, C15PacketClientSettings::new)
                .reg(PacketDirection.OUTGOING, C16PacketClientStatus.class, C16PacketClientStatus::new)
                .reg(PacketDirection.OUTGOING, C17PacketCustomPayload.class, C17PacketCustomPayload::new)
                .reg(PacketDirection.OUTGOING, C18PacketSpectate.class, C18PacketSpectate::new)
                .reg(PacketDirection.OUTGOING, C19PacketResourcePackStatus.class, C19PacketResourcePackStatus::new);

        STATUS.reg(PacketDirection.OUTGOING, C00PacketServerQuery.class, C00PacketServerQuery::new)
                .reg(PacketDirection.INCOMING, S00PacketServerInfo.class, S00PacketServerInfo::new)
                .reg(PacketDirection.OUTGOING, C01PacketPing.class, C01PacketPing::new)
                .reg(PacketDirection.INCOMING, S01PacketPong.class, S01PacketPong::new);

        LOGIN.reg(PacketDirection.INCOMING, S00PacketDisconnect.class, S00PacketDisconnect::new)
                .reg(PacketDirection.INCOMING, S01PacketEncryptionRequest.class, S01PacketEncryptionRequest::new)
                .reg(PacketDirection.INCOMING, S02PacketLoginSuccess.class, S02PacketLoginSuccess::new)
                .reg(PacketDirection.INCOMING, S03PacketEnableCompression.class, S03PacketEnableCompression::new)
                .reg(PacketDirection.OUTGOING, C00PacketLoginStart.class, C00PacketLoginStart::new)
                .reg(PacketDirection.OUTGOING, C01PacketEncryptionResponse.class, C01PacketEncryptionResponse::new);

        PACKET_TO_STATE.trim();
        for (NetworkState state : values()) {
            for (PacketRegistry registry : state.registries) {
                registry.optimize();
            }
        }
    }
}
