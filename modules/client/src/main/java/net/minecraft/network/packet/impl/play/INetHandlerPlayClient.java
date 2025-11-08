package net.minecraft.network.packet.impl.play;

import net.minecraft.network.INetHandler;
import net.minecraft.network.packet.impl.play.server.*;

public interface INetHandlerPlayClient extends INetHandler {
    void handleSpawnObject(S0EPacketSpawnObject packet);

    void handleSpawnExperienceOrb(S11PacketSpawnExperienceOrb packet);

    void handleSpawnGlobalEntity(S2CPacketSpawnGlobalEntity packet);

    void handleSpawnMob(S0FPacketSpawnMob packet);

    void handleScoreboardObjective(S3BPacketScoreboardObjective packet);

    void handleSpawnPainting(S10PacketSpawnPainting packet);

    void handleSpawnPlayer(S0CPacketSpawnPlayer packet);

    void handleAnimation(S0BPacketAnimation packet);

    void handleStatistics(S37PacketStatistics packet);

    void handleBlockBreakAnim(S25PacketBlockBreakAnim packet);

    void handleSignEditorOpen(S36PacketSignEditorOpen packet);

    void handleUpdateTileEntity(S35PacketUpdateTileEntity packet);

    void handleBlockAction(S24PacketBlockAction packet);

    void handleBlockChange(S23PacketBlockChange packet);

    void handleChat(S02PacketChat packet);

    void handleTabComplete(S3APacketTabComplete packet);

    void handleMultiBlockChange(S22PacketMultiBlockChange packet);

    void handleMaps(S34PacketMaps packet);

    void handleConfirmTransaction(S32PacketConfirmTransaction packet);

    void handleCloseWindow(S2EPacketCloseWindow packet);

    void handleWindowItems(S30PacketWindowItems packet);

    void handleOpenWindow(S2DPacketOpenWindow packet);

    void handleWindowProperty(S31PacketWindowProperty packet);

    void handleSetSlot(S2FPacketSetSlot packet);

    void handleCustomPayload(S3FPacketCustomPayload packet);

    void handleDisconnect(S40PacketDisconnect packet);

    void handleUseBed(S0APacketUseBed packet);

    void handleEntityStatus(S19PacketEntityStatus packet);

    void handleEntityAttach(S1BPacketEntityAttach packet);

    void handleExplosion(S27PacketExplosion packet);

    void handleChangeGameState(S2BPacketChangeGameState packet);

    void handleKeepAlive(S00PacketKeepAlive packet);

    void handleChunkData(S21PacketChunkData packet);

    void handleMapChunkBulk(S26PacketMapChunkBulk packet);

    void handleEffect(S28PacketEffect packet);

    void handleJoinGame(S01PacketJoinGame packet);

    void handleEntityMovement(S14PacketEntity packet);

    void handlePlayerPosLook(S08PacketPlayerPosLook packet);

    void handleParticles(S2APacketParticles packet);

    void handlePlayerAbilities(S39PacketPlayerAbilities packet);

    void handlePlayerListItem(S38PacketPlayerListItem packet);

    void handleDestroyEntities(S13PacketDestroyEntities packet);

    void handleRemoveEntityEffect(S1EPacketRemoveEntityEffect packet);

    void handleRespawn(S07PacketRespawn packet);

    void handleEntityHeadLook(S19PacketEntityHeadLook packet);

    void handleHeldItemChange(S09PacketHeldItemChange packet);

    void handleDisplayScoreboard(S3DPacketDisplayScoreboard packet);

    void handleEntityMetadata(S1CPacketEntityMetadata packet);

    void handleEntityVelocity(S12PacketEntityVelocity packet);

    void handleEntityEquipment(S04PacketEntityEquipment packet);

    void handleSetExperience(S1FPacketSetExperience packet);

    void handleUpdateHealth(S06PacketUpdateHealth packet);

    void handleTeams(S3EPacketTeams packet);

    void handleUpdateScore(S3CPacketUpdateScore packet);

    void handleSpawnPosition(S05PacketSpawnPosition packet);

    void handleTimeUpdate(S03PacketTimeUpdate packet);

    void handleUpdateSign(S33PacketUpdateSign packet);

    void handleSoundEffect(S29PacketSoundEffect packet);

    void handleCollectItem(S0DPacketCollectItem packet);

    void handleEntityTeleport(S18PacketEntityTeleport packet);

    void handleEntityProperties(S20PacketEntityProperties packet);

    void handleEntityEffect(S1DPacketEntityEffect packet);

    void handleCombatEvent(S42PacketCombatEvent packet);

    void handleServerDifficulty(S41PacketServerDifficulty packet);

    void handleCamera(S43PacketCamera packet);

    void handleWorldBorder(S44PacketWorldBorder packet);

    void handleTitle(S45PacketTitle packet);

    void handleSetCompressionLevel(S46PacketSetCompressionLevel packet);

    void handlePlayerListHeaderFooter(S47PacketPlayerListHeaderFooter packet);

    void handleResourcePack(S48PacketResourcePackSend packet);

    void handleEntityNBT(S49PacketUpdateEntityNBT packet);
}
