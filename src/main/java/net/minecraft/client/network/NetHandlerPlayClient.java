package net.minecraft.client.network;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.mojang.authlib.GameProfile;
import io.netty.buffer.Unpooled;
import net.minecraft.block.Block;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityOtherPlayerMP;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.inventory.GuiContainerCreative;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.ServerList;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EntityPickupFX;
import net.minecraft.client.player.inventory.ContainerLocalMenu;
import net.minecraft.client.player.inventory.LocalBlockIntercommunication;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.ai.attributes.BaseAttributeMap;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.RangedAttribute;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.item.*;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.*;
import net.minecraft.init.Items;
import net.minecraft.inventory.AnimalChest;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.InventoryBasic;
import net.minecraft.item.Item;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.Packet;
import net.minecraft.network.PacketBuffer;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.INetHandlerPlayClient;
import net.minecraft.network.play.client.*;
import net.minecraft.network.play.server.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.scoreboard.*;
import net.minecraft.stats.Achievement;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatBase;
import net.minecraft.tileentity.*;
import net.minecraft.util.*;
import net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.Explosion;
import net.minecraft.world.WorldProviderSurface;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.storage.MapData;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.Map.Entry;

public class NetHandlerPlayClient implements INetHandlerPlayClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetHandlerPlayClient.class);
    private final NetworkManager netManager;
    private final GameProfile profile;
    private final GuiScreen guiScreenServer;
    private Minecraft gameController;
    private WorldClient clientWorldController;
    private boolean doneLoadingTerrain;
    private final Map<UUID, NetworkPlayerInfo> playerInfoMap = new HashMap<>();
    public int currentServerMaxPlayers = 20;
    private boolean field_147308_k = false;
    private final Random avRandomizer = new Random();

    public NetHandlerPlayClient(Minecraft minecraft, GuiScreen guiScreen, NetworkManager netManager, GameProfile gameProfile) {
        this.gameController = minecraft;
        this.guiScreenServer = guiScreen;
        this.netManager = netManager;
        this.profile = gameProfile;
    }

    public void cleanup() {
        this.clientWorldController = null;
    }

    public void handleJoinGame(S01PacketJoinGame packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.playerController = new PlayerControllerMP(this.gameController, this);
        this.clientWorldController = new WorldClient(this, new WorldSettings(0L, packet.getGameType(), false, packet.isHardcoreMode(), packet.getWorldType()), packet.getDimension(), packet.getDifficulty());
        this.gameController.gameSettings.difficulty = packet.getDifficulty();
        this.gameController.loadWorld(this.clientWorldController);
        this.gameController.player.dimension = packet.getDimension();
        this.gameController.displayGuiScreen(new GuiDownloadTerrain(this));
        this.gameController.player.setEntityId(packet.getEntityId());
        this.currentServerMaxPlayers = packet.getMaxPlayers();
        this.gameController.player.setReducedDebug(packet.isReducedDebugInfo());
        this.gameController.playerController.setGameType(packet.getGameType());
        this.gameController.gameSettings.sendSettingsToServer();
        this.netManager.sendPacket(new C17PacketCustomPayload("MC|Brand", (new PacketBuffer(Unpooled.buffer())).writeString(ClientBrandRetriever.getClientModName())));
    }

    public void handleSpawnObject(S0EPacketSpawnObject packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        double x = packet.getX() / 32.0D;
        double y = packet.getY() / 32.0D;
        double z = packet.getZ() / 32.0D;
        Entity entity = null;

        switch (packet.getType()) {
            case 1 -> entity = new EntityBoat(this.clientWorldController, x, y, z);
            case 2 -> entity = new EntityItem(this.clientWorldController, x, y, z);
            case 10 ->
                    entity = EntityMinecart.getMinecart(this.clientWorldController, x, y, z, EntityMinecart.MinecartType.byNetworkID(packet.getExtraData()));
            case 50 -> entity = new EntityTNTPrimed(this.clientWorldController, x, y, z, null);
            case 51 -> entity = new EntityEnderCrystal(this.clientWorldController, x, y, z);
            case 60 -> entity = new EntityArrow(this.clientWorldController, x, y, z);
            case 61 -> entity = new EntitySnowball(this.clientWorldController, x, y, z);
            case 62 -> entity = new EntityEgg(this.clientWorldController, x, y, z);
            case 63 ->
                    entity = new EntityLargeFireball(this.clientWorldController, x, y, z, packet.getSpeedX() / 8000.0D, packet.getSpeedY() / 8000.0D, packet.getSpeedZ() / 8000.0D);
            case 64 ->
                    entity = new EntitySmallFireball(this.clientWorldController, x, y, z, packet.getSpeedX() / 8000.0D, packet.getSpeedY() / 8000.0D, packet.getSpeedZ() / 8000.0D);
            case 65 -> entity = new EntityEnderPearl(this.clientWorldController, x, y, z);
            case 66 ->
                    entity = new EntityWitherSkull(this.clientWorldController, x, y, z, packet.getSpeedX() / 8000.0D, packet.getSpeedY() / 8000.0D, packet.getSpeedZ() / 8000.0D);
            case 70 ->
                    entity = new EntityFallingBlock(this.clientWorldController, x, y, z, Block.getStateById(packet.getExtraData() & 65535));
            case 71 ->
                    entity = new EntityItemFrame(this.clientWorldController, new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)), Direction.getHorizontal(packet.getExtraData()));
            case 72 -> entity = new EntityEnderEye(this.clientWorldController, x, y, z);
            case 73 -> entity = new EntityPotion(this.clientWorldController, x, y, z, packet.getExtraData());
            case 75 -> entity = new EntityExpBottle(this.clientWorldController, x, y, z);
            case 76 -> entity = new EntityFireworkRocket(this.clientWorldController, x, y, z, null);
            case 77 ->
                    entity = new EntityLeashKnot(this.clientWorldController, new BlockPos(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z)));
            case 78 -> entity = new EntityArmorStand(this.clientWorldController, x, y, z);
            case 90 -> {
                Entity entity1 = this.clientWorldController.getEntityByID(packet.getExtraData());

                if (entity1 instanceof EntityPlayer entityPlayer) {
                    entity = new EntityFishHook(this.clientWorldController, x, y, z, entityPlayer);
                }
            }
        }

        if (entity != null) {
            entity.serverPosX = packet.getX();
            entity.serverPosY = packet.getY();
            entity.serverPosZ = packet.getZ();
            entity.rotationPitch = (packet.getPitch() * 360) / 256.0F;
            entity.rotationYaw = (packet.getYaw() * 360) / 256.0F;
            Entity[] aentity = entity.getParts();

            if (aentity != null) {
                int i = packet.getEntityID() - entity.getEntityId();

                for (Entity value : aentity) {
                    value.setEntityId(value.getEntityId() + i);
                }
            }

            entity.setEntityId(packet.getEntityID());
            this.clientWorldController.addEntityToWorld(packet.getEntityID(), entity);

            if (packet.getExtraData() > 0) {
                if (packet.getType() == 60) {
                    Entity entity2 = this.clientWorldController.getEntityByID(packet.getExtraData());

                    if (entity2 instanceof EntityLivingBase && entity instanceof EntityArrow entityArrow) {
                        entityArrow.shootingEntity = entity2;
                    }
                }

                entity.setVelocity(packet.getSpeedX() / 8000.0D, packet.getSpeedY() / 8000.0D, packet.getSpeedZ() / 8000.0D);
            }
        }
    }

    public void handleSpawnExperienceOrb(S11PacketSpawnExperienceOrb packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = new EntityXPOrb(this.clientWorldController, packet.getX() / 32.0D, packet.getY() / 32.0D, packet.getZ() / 32.0D, packet.getXPValue());
        entity.serverPosX = packet.getX();
        entity.serverPosY = packet.getY();
        entity.serverPosZ = packet.getZ();
        entity.rotationYaw = 0.0F;
        entity.rotationPitch = 0.0F;
        entity.setEntityId(packet.getEntityID());
        this.clientWorldController.addEntityToWorld(packet.getEntityID(), entity);
    }

    public void handleSpawnGlobalEntity(S2CPacketSpawnGlobalEntity packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        double x = packet.func_149051_d() / 32.0D;
        double y = packet.func_149050_e() / 32.0D;
        double z = packet.func_149049_f() / 32.0D;
        Entity entity = null;

        if (packet.func_149053_g() == 1) {
            entity = new EntityLightningBolt(this.clientWorldController, x, y, z);
        }

        if (entity != null) {
            entity.serverPosX = packet.func_149051_d();
            entity.serverPosY = packet.func_149050_e();
            entity.serverPosZ = packet.func_149049_f();
            entity.rotationYaw = 0.0F;
            entity.rotationPitch = 0.0F;
            entity.setEntityId(packet.func_149052_c());
            this.clientWorldController.addWeatherEffect(entity);
        }
    }

    public void handleSpawnPainting(S10PacketSpawnPainting packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPainting painting = new EntityPainting(this.clientWorldController, packet.getPosition(), packet.getFacing(), packet.getTitle());
        this.clientWorldController.addEntityToWorld(packet.getEntityID(), painting);
    }

    public void handleEntityVelocity(S12PacketEntityVelocity packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityID());

        if (entity != null) {
            entity.setVelocity(packet.getMotionX() / 8000.0D, packet.getMotionY() / 8000.0D, packet.getMotionZ() / 8000.0D);
        }
    }

    public void handleEntityMetadata(S1CPacketEntityMetadata packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityId());

        if (entity != null && packet.func_149376_c() != null) {
            entity.getDataWatcher().updateWatchedObjectsFromList(packet.func_149376_c());
        }
    }

    public void handleSpawnPlayer(S0CPacketSpawnPlayer packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        double x = packet.getX() / 32.0D;
        double y = packet.getY() / 32.0D;
        double z = packet.getZ() / 32.0D;
        float yaw = (packet.getYaw() * 360) / 256.0F;
        float pitch = (packet.getPitch() * 360) / 256.0F;
        EntityOtherPlayerMP playerMP = new EntityOtherPlayerMP(this.gameController.world, this.getPlayerInfo(packet.getPlayer()).getGameProfile());
        playerMP.prevPosX = playerMP.lastTickPosX = (playerMP.serverPosX = packet.getX());
        playerMP.prevPosY = playerMP.lastTickPosY = (playerMP.serverPosY = packet.getY());
        playerMP.prevPosZ = playerMP.lastTickPosZ = (playerMP.serverPosZ = packet.getZ());
        int i = packet.getCurrentItemID();

        if (i == 0) {
            playerMP.inventory.mainInventory[playerMP.inventory.currentItem] = null;
        } else {
            playerMP.inventory.mainInventory[playerMP.inventory.currentItem] = new ItemStack(Item.getItemById(i), 1, 0);
        }

        playerMP.setPositionAndRotation(x, y, z, yaw, pitch);
        this.clientWorldController.addEntityToWorld(packet.getEntityID(), playerMP);
        List<DataWatcher.WatchableObject> list = packet.getMetaData();

        if (list != null) {
            playerMP.getDataWatcher().updateWatchedObjectsFromList(list);
        }
    }

    public void handleEntityTeleport(S18PacketEntityTeleport packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityId());

        if (entity != null) {
            entity.serverPosX = packet.getX();
            entity.serverPosY = packet.getY();
            entity.serverPosZ = packet.getZ();
            double d0 = entity.serverPosX / 32.0D;
            double d1 = entity.serverPosY / 32.0D;
            double d2 = entity.serverPosZ / 32.0D;
            float f = (packet.getYaw() * 360) / 256.0F;
            float f1 = (packet.getPitch() * 360) / 256.0F;

            if (Math.abs(entity.posX - d0) < 0.03125D && Math.abs(entity.posY - d1) < 0.015625D && Math.abs(entity.posZ - d2) < 0.03125D) {
                entity.setPositionAndRotation2(entity.posX, entity.posY, entity.posZ, f, f1, 3, true);
            } else {
                entity.setPositionAndRotation2(d0, d1, d2, f, f1, 3, true);
            }

            entity.onGround = packet.getOnGround();
        }
    }

    public void handleHeldItemChange(S09PacketHeldItemChange packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if (packet.getHeldItemHotbarIndex() >= 0 && packet.getHeldItemHotbarIndex() < InventoryPlayer.getHotbarSize()) {
            this.gameController.player.inventory.currentItem = packet.getHeldItemHotbarIndex();
        }
    }

    public void handleEntityMovement(S14PacketEntity packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = packet.getEntity(this.clientWorldController);

        if (entity != null) {
            entity.serverPosX += packet.func_149062_c();
            entity.serverPosY += packet.func_149061_d();
            entity.serverPosZ += packet.func_149064_e();
            double d0 = entity.serverPosX / 32.0D;
            double d1 = entity.serverPosY / 32.0D;
            double d2 = entity.serverPosZ / 32.0D;
            float f = packet.func_149060_h() ? (packet.func_149066_f() * 360) / 256.0F : entity.rotationYaw;
            float f1 = packet.func_149060_h() ? (packet.func_149063_g() * 360) / 256.0F : entity.rotationPitch;
            entity.setPositionAndRotation2(d0, d1, d2, f, f1, 3, false);
            entity.onGround = packet.getOnGround();
        }
    }

    public void handleEntityHeadLook(S19PacketEntityHeadLook packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = packet.getEntity(this.clientWorldController);

        if (entity != null) {
            float yaw = (packet.getYaw() * 360) / 256.0F;
            entity.setRotationYawHead(yaw);
        }
    }

    public void handleDestroyEntities(S13PacketDestroyEntities packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        for (int i = 0; i < packet.getEntityIDs().length; ++i) {
            this.clientWorldController.removeEntityFromWorld(packet.getEntityIDs()[i]);
        }
    }

    public void handlePlayerPosLook(S08PacketPlayerPosLook packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPlayer entityplayer = this.gameController.player;
        double x = packet.getX();
        double y = packet.getY();
        double z = packet.getZ();
        float yaw = packet.getYaw();
        float pitch = packet.getPitch();

        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.Flag.X)) {
            x += entityplayer.posX;
        } else {
            entityplayer.motionX = 0.0D;
        }

        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.Flag.Y)) {
            y += entityplayer.posY;
        } else {
            entityplayer.motionY = 0.0D;
        }

        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.Flag.Z)) {
            z += entityplayer.posZ;
        } else {
            entityplayer.motionZ = 0.0D;
        }

        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.Flag.X_ROT)) {
            pitch += entityplayer.rotationPitch;
        }

        if (packet.func_179834_f().contains(S08PacketPlayerPosLook.Flag.Y_ROT)) {
            yaw += entityplayer.rotationYaw;
        }

        entityplayer.setPositionAndRotation(x, y, z, yaw, pitch);
        this.netManager.sendPacket(new C03PacketPlayer.C06PacketPlayerPosLook(entityplayer.posX, entityplayer.getEntityBoundingBox().minY, entityplayer.posZ, entityplayer.rotationYaw, entityplayer.rotationPitch, false));

        if (!this.doneLoadingTerrain) {
            this.gameController.player.prevPosX = this.gameController.player.posX;
            this.gameController.player.prevPosY = this.gameController.player.posY;
            this.gameController.player.prevPosZ = this.gameController.player.posZ;
            this.doneLoadingTerrain = true;
            this.gameController.displayGuiScreen(null);
        }
    }

    public void handleMultiBlockChange(S22PacketMultiBlockChange packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        for (S22PacketMultiBlockChange.BlockUpdateData updateData : packet.getChangedBlocks()) {
            this.clientWorldController.invalidateRegionAndSetBlock(updateData.getPos(), updateData.getBlockState());
        }
    }

    public void handleChunkData(S21PacketChunkData packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if (packet.func_149274_i()) {
            if (packet.getExtractedSize() == 0) {
                this.clientWorldController.doPreChunk(packet.getChunkX(), packet.getChunkZ(), false);
                return;
            }

            this.clientWorldController.doPreChunk(packet.getChunkX(), packet.getChunkZ(), true);
        }

        this.clientWorldController.invalidateBlockReceiveRegion(packet.getChunkX() << 4, 0, packet.getChunkZ() << 4, (packet.getChunkX() << 4) + 15, 256, (packet.getChunkZ() << 4) + 15);
        Chunk chunk = this.clientWorldController.getChunkFromChunkCoords(packet.getChunkX(), packet.getChunkZ());
        chunk.fillChunk(packet.getExtractedDataBytes(), packet.getExtractedSize(), packet.func_149274_i());
        this.clientWorldController.markBlockRangeForRenderUpdate(packet.getChunkX() << 4, 0, packet.getChunkZ() << 4, (packet.getChunkX() << 4) + 15, 256, (packet.getChunkZ() << 4) + 15);

        if (!packet.func_149274_i() || !(this.clientWorldController.provider instanceof WorldProviderSurface)) {
            chunk.resetRelightChecks();
        }
    }

    public void handleBlockChange(S23PacketBlockChange packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.clientWorldController.invalidateRegionAndSetBlock(packet.getBlockPosition(), packet.getBlockState());
    }

    public void handleDisconnect(S40PacketDisconnect packet) {
        this.netManager.closeChannel(packet.getReason());
    }

    public void onDisconnect(IChatComponent reason) {
        this.gameController.loadWorld(null);

        this.gameController.displayGuiScreen(new GuiDisconnected(Objects.requireNonNullElseGet(this.guiScreenServer, () -> new GuiMultiplayer(new GuiMainMenu())), "disconnect.lost", reason));
    }

    public void addToSendQueue(Packet<?> packet) {
        this.netManager.sendPacket(packet);
    }

    public void handleCollectItem(S0DPacketCollectItem packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getCollectedItemEntityID());
        EntityLivingBase entitylivingbase = (EntityLivingBase) this.clientWorldController.getEntityByID(packet.getEntityID());

        if (entitylivingbase == null) {
            entitylivingbase = this.gameController.player;
        }

        if (entity != null) {
            if (entity instanceof EntityXPOrb) {
                this.clientWorldController.playSoundAtEntity(entity, "random.orb", 0.2F, ((this.avRandomizer.nextFloat() - this.avRandomizer.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            } else {
                this.clientWorldController.playSoundAtEntity(entity, "random.pop", 0.2F, ((this.avRandomizer.nextFloat() - this.avRandomizer.nextFloat()) * 0.7F + 1.0F) * 2.0F);
            }

            this.gameController.effectRenderer.addEffect(new EntityPickupFX(this.clientWorldController, entity, entitylivingbase, 0.5F));
            this.clientWorldController.removeEntityFromWorld(packet.getCollectedItemEntityID());
        }
    }

    public void handleChat(S02PacketChat packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if (packet.getType() == 2) {
            this.gameController.ingameGUI.setRecordPlaying(packet.getChatComponent(), false);
        } else {
            this.gameController.ingameGUI.getChatGUI().printChatMessage(packet.getChatComponent());
        }
    }

    public void handleAnimation(S0BPacketAnimation packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityID());

        if (entity != null) {
            if (packet.getAnimationType() == 0) {
                EntityLivingBase entitylivingbase = (EntityLivingBase) entity;
                entitylivingbase.swingItem();
            } else if (packet.getAnimationType() == 1) {
                entity.performHurtAnimation();
            } else if (packet.getAnimationType() == 2) {
                EntityPlayer entityplayer = (EntityPlayer) entity;
                entityplayer.wakeUpPlayer(false, false, false);
            } else if (packet.getAnimationType() == 4) {
                this.gameController.effectRenderer.emitParticleAtEntity(entity, ParticleTypes.CRIT);
            } else if (packet.getAnimationType() == 5) {
                this.gameController.effectRenderer.emitParticleAtEntity(entity, ParticleTypes.CRIT_MAGIC);
            }
        }
    }

    public void handleUseBed(S0APacketUseBed packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        packet.getPlayer(this.clientWorldController).trySleep(packet.getPosition());
    }

    public void handleSpawnMob(S0FPacketSpawnMob packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        double x = packet.getX() / 32.0D;
        double y = packet.getY() / 32.0D;
        double z = packet.getZ() / 32.0D;
        float yaw = (packet.getYaw() * 360) / 256.0F;
        float pitch = (packet.getPitch() * 360) / 256.0F;
        EntityLivingBase entitylivingbase = (EntityLivingBase) EntityList.createEntityByID(packet.getEntityType(), this.gameController.world);
        entitylivingbase.serverPosX = packet.getX();
        entitylivingbase.serverPosY = packet.getY();
        entitylivingbase.serverPosZ = packet.getZ();
        entitylivingbase.renderYawOffset = entitylivingbase.rotationYawHead = (packet.getHeadPitch() * 360) / 256.0F;
        Entity[] aentity = entitylivingbase.getParts();

        if (aentity != null) {
            int i = packet.getEntityID() - entitylivingbase.getEntityId();

            for (Entity entity : aentity) {
                entity.setEntityId(entity.getEntityId() + i);
            }
        }

        entitylivingbase.setEntityId(packet.getEntityID());
        entitylivingbase.setPositionAndRotation(x, y, z, yaw, pitch);
        entitylivingbase.motionX = (packet.getVelocityX() / 8000.0F);
        entitylivingbase.motionY = (packet.getVelocityY() / 8000.0F);
        entitylivingbase.motionZ = (packet.getVelocityZ() / 8000.0F);
        this.clientWorldController.addEntityToWorld(packet.getEntityID(), entitylivingbase);
        List<DataWatcher.WatchableObject> list = packet.func_149027_c();

        if (list != null) {
            entitylivingbase.getDataWatcher().updateWatchedObjectsFromList(list);
        }
    }

    public void handleTimeUpdate(S03PacketTimeUpdate packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.world.setTotalWorldTime(packet.getTotalWorldTime());
        this.gameController.world.setWorldTime(packet.getWorldTime());
    }

    public void handleSpawnPosition(S05PacketSpawnPosition packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.player.setSpawnPoint(packet.getSpawnPos(), true);
        this.gameController.world.getWorldInfo().setSpawn(packet.getSpawnPos());
    }

    public void handleEntityAttach(S1BPacketEntityAttach packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityId());
        Entity vehicle = this.clientWorldController.getEntityByID(packet.getVehicleEntityId());

        if (packet.getLeash() == 0) {
            boolean flag = false;

            if (packet.getEntityId() == this.gameController.player.getEntityId()) {
                entity = this.gameController.player;

                if (vehicle instanceof EntityBoat entityBoat) {
                    entityBoat.setIsBoatEmpty(false);
                }

                flag = entity.ridingEntity == null && vehicle != null;
            } else if (vehicle instanceof EntityBoat entityBoat) {
                entityBoat.setIsBoatEmpty(true);
            }

            if (entity == null) {
                return;
            }

            entity.mountEntity(vehicle);

            if (flag) {
                GameSettings gamesettings = this.gameController.gameSettings;
                this.gameController.ingameGUI.setRecordPlaying(
                        I18n.format("mount.onboard", GameSettings.getKeyDisplayString(gamesettings.keyBindSneak.getKeyCode())),
                        false
                );
            }
        } else if (packet.getLeash() == 1 && entity instanceof EntityLiving entityLiving) {
            if (vehicle != null) {
                entityLiving.setLeashedToEntity(vehicle, false);
            } else {
                entityLiving.clearLeashed(false, false);
            }
        }
    }

    public void handleEntityStatus(S19PacketEntityStatus packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = packet.getEntity(this.clientWorldController);

        if (entity != null) {
            entity.handleStatusUpdate(packet.getOpCode());
        }
    }

    public void handleUpdateHealth(S06PacketUpdateHealth packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.player.setPlayerSPHealth(packet.getHealth());
        this.gameController.player.getFoodStats().setFoodLevel(packet.getFoodLevel());
        this.gameController.player.getFoodStats().setFoodSaturationLevel(packet.getSaturationLevel());
    }

    public void handleSetExperience(S1FPacketSetExperience packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.player.setXPStats(packet.func_149397_c(), packet.getTotalExperience(), packet.getLevel());
    }

    public void handleRespawn(S07PacketRespawn packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if (packet.getDimensionID() != this.gameController.player.dimension) {
            this.doneLoadingTerrain = false;
            Scoreboard scoreboard = this.clientWorldController.getScoreboard();
            this.clientWorldController = new WorldClient(this, new WorldSettings(0L, packet.getGameType(), false, this.gameController.world.getWorldInfo().isHardcoreModeEnabled(), packet.getWorldType()), packet.getDimensionID(), packet.getDifficulty());
            this.clientWorldController.setWorldScoreboard(scoreboard);
            this.gameController.loadWorld(this.clientWorldController);
            this.gameController.player.dimension = packet.getDimensionID();
            this.gameController.displayGuiScreen(new GuiDownloadTerrain(this));
        }

        this.gameController.setDimensionAndSpawnPlayer(packet.getDimensionID());
        this.gameController.playerController.setGameType(packet.getGameType());
    }

    public void handleExplosion(S27PacketExplosion packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Explosion explosion = new Explosion(this.gameController.world, null, packet.getX(), packet.getY(), packet.getZ(), packet.getStrength(), packet.getAffectedBlockPositions());
        explosion.doExplosionB(true);
        this.gameController.player.motionX += packet.func_149149_c();
        this.gameController.player.motionY += packet.func_149144_d();
        this.gameController.player.motionZ += packet.func_149147_e();
    }

    public void handleOpenWindow(S2DPacketOpenWindow packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPlayerSP entityplayersp = this.gameController.player;

        if ("minecraft:container".equals(packet.getGuiId())) {
            entityplayersp.displayGUIChest(new InventoryBasic(packet.getWindowTitle(), packet.getSlotCount()));
            entityplayersp.openContainer.windowId = packet.getWindowId();
        } else if ("minecraft:villager".equals(packet.getGuiId())) {
            entityplayersp.displayVillagerTradeGui(new NpcMerchant(entityplayersp, packet.getWindowTitle()));
            entityplayersp.openContainer.windowId = packet.getWindowId();
        } else if ("EntityHorse".equals(packet.getGuiId())) {
            Entity entity = this.clientWorldController.getEntityByID(packet.getEntityId());

            if (entity instanceof EntityHorse entityHorse) {
                entityplayersp.displayGUIHorse(entityHorse, new AnimalChest(packet.getWindowTitle(), packet.getSlotCount()));
                entityplayersp.openContainer.windowId = packet.getWindowId();
            }
        } else if (!packet.hasSlots()) {
            entityplayersp.displayGui(new LocalBlockIntercommunication(packet.getGuiId(), packet.getWindowTitle()));
            entityplayersp.openContainer.windowId = packet.getWindowId();
        } else {
            ContainerLocalMenu containerlocalmenu = new ContainerLocalMenu(packet.getGuiId(), packet.getWindowTitle(), packet.getSlotCount());
            entityplayersp.displayGUIChest(containerlocalmenu);
            entityplayersp.openContainer.windowId = packet.getWindowId();
        }
    }

    public void handleSetSlot(S2FPacketSetSlot packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPlayer entityplayer = this.gameController.player;

        if (packet.func_149175_c() == -1) {
            entityplayer.inventory.setItemStack(packet.func_149174_e());
        } else {
            boolean flag = false;

            if (this.gameController.currentScreen instanceof GuiContainerCreative guicontainercreative) {
                flag = guicontainercreative.getSelectedTabIndex() != CreativeTabs.TAB_INVENTORY.getTabIndex();
            }

            if (packet.func_149175_c() == 0 && packet.func_149173_d() >= 36 && packet.func_149173_d() < 45) {
                ItemStack itemstack = entityplayer.inventoryContainer.getSlot(packet.func_149173_d()).getStack();

                if (packet.func_149174_e() != null && (itemstack == null || itemstack.stackSize < packet.func_149174_e().stackSize)) {
                    packet.func_149174_e().animationsToGo = 5;
                }

                entityplayer.inventoryContainer.putStackInSlot(packet.func_149173_d(), packet.func_149174_e());
            } else if (packet.func_149175_c() == entityplayer.openContainer.windowId && (packet.func_149175_c() != 0 || !flag)) {
                entityplayer.openContainer.putStackInSlot(packet.func_149173_d(), packet.func_149174_e());
            }
        }
    }

    public void handleConfirmTransaction(S32PacketConfirmTransaction packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Container container = null;
        EntityPlayer entityplayer = this.gameController.player;

        if (packet.getWindowId() == 0) {
            container = entityplayer.inventoryContainer;
        } else if (packet.getWindowId() == entityplayer.openContainer.windowId) {
            container = entityplayer.openContainer;
        }

        if (container != null && !packet.func_148888_e()) {
            this.addToSendQueue(new C0FPacketConfirmTransaction(packet.getWindowId(), packet.getActionNumber(), true));
        }
    }

    public void handleWindowItems(S30PacketWindowItems packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPlayer entityplayer = this.gameController.player;

        if (packet.func_148911_c() == 0) {
            entityplayer.inventoryContainer.putStacksInSlots(packet.getItemStacks());
        } else if (packet.func_148911_c() == entityplayer.openContainer.windowId) {
            entityplayer.openContainer.putStacksInSlots(packet.getItemStacks());
        }
    }

    public void handleSignEditorOpen(S36PacketSignEditorOpen packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        TileEntity tileentity = this.clientWorldController.getTileEntity(packet.getSignPosition());

        if (!(tileentity instanceof TileEntitySign)) {
            tileentity = new TileEntitySign();
            tileentity.setWorldObj(this.clientWorldController);
            tileentity.setPos(packet.getSignPosition());
        }

        this.gameController.player.openEditSign((TileEntitySign) tileentity);
    }

    public void handleUpdateSign(S33PacketUpdateSign packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        boolean flag = false;

        if (this.gameController.world.isBlockLoaded(packet.getPos())) {
            TileEntity tileentity = this.gameController.world.getTileEntity(packet.getPos());

            if (tileentity instanceof TileEntitySign tileentitysign) {

                if (tileentitysign.getIsEditable()) {
                    System.arraycopy(packet.getLines(), 0, tileentitysign.signText, 0, 4);
                    tileentitysign.markDirty();
                }

                flag = true;
            }
        }

        if (!flag && this.gameController.player != null) {
            this.gameController.player.addChatMessage(new ChatComponentText("Unable to locate sign at " + packet.getPos().getX() + ", " + packet.getPos().getY() + ", " + packet.getPos().getZ()));
        }
    }

    public void handleUpdateTileEntity(S35PacketUpdateTileEntity packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if (this.gameController.world.isBlockLoaded(packet.getPos())) {
            TileEntity tileentity = this.gameController.world.getTileEntity(packet.getPos());
            int i = packet.getTileEntityType();

            if (i == 1 && tileentity instanceof TileEntityMobSpawner || i == 2 && tileentity instanceof TileEntityCommandBlock || i == 3 && tileentity instanceof TileEntityBeacon || i == 4 && tileentity instanceof TileEntitySkull || i == 5 && tileentity instanceof TileEntityFlowerPot || i == 6 && tileentity instanceof TileEntityBanner) {
                tileentity.readFromNBT(packet.getNbtCompound());
            }
        }
    }

    public void handleWindowProperty(S31PacketWindowProperty packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPlayer entityplayer = this.gameController.player;

        if (entityplayer.openContainer != null && entityplayer.openContainer.windowId == packet.getWindowId()) {
            entityplayer.openContainer.updateProgressBar(packet.getVarIndex(), packet.getVarValue());
        }
    }

    public void handleEntityEquipment(S04PacketEntityEquipment packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityID());

        if (entity != null) {
            entity.setCurrentItemOrArmor(packet.getEquipmentSlot(), packet.getItemStack());
        }
    }

    public void handleCloseWindow(S2EPacketCloseWindow packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.player.closeScreenAndDropStack();
    }

    public void handleBlockAction(S24PacketBlockAction packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.world.addBlockEvent(packet.getBlockPosition(), packet.getBlockType(), packet.getData1(), packet.getData2());
    }

    public void handleBlockBreakAnim(S25PacketBlockBreakAnim packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.world.sendBlockBreakProgress(packet.getBreakerId(), packet.getPosition(), packet.getProgress());
    }

    public void handleMapChunkBulk(S26PacketMapChunkBulk packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        for (int i = 0; i < packet.getChunkCount(); ++i) {
            int j = packet.getChunkX(i);
            int k = packet.getChunkZ(i);
            this.clientWorldController.doPreChunk(j, k, true);
            this.clientWorldController.invalidateBlockReceiveRegion(j << 4, 0, k << 4, (j << 4) + 15, 256, (k << 4) + 15);
            Chunk chunk = this.clientWorldController.getChunkFromChunkCoords(j, k);
            chunk.fillChunk(packet.getChunkBytes(i), packet.getChunkSize(i), true);
            this.clientWorldController.markBlockRangeForRenderUpdate(j << 4, 0, k << 4, (j << 4) + 15, 256, (k << 4) + 15);

            if (!(this.clientWorldController.provider instanceof WorldProviderSurface)) {
                chunk.resetRelightChecks();
            }
        }
    }

    public void handleChangeGameState(S2BPacketChangeGameState packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPlayer entityplayer = this.gameController.player;
        int i = packet.getGameState();
        float f = packet.func_149137_d();
        int j = MathHelper.floor(f + 0.5F);

        if (i >= 0 && i < S2BPacketChangeGameState.MESSAGE_NAMES.length && S2BPacketChangeGameState.MESSAGE_NAMES[i] != null) {
            entityplayer.addChatComponentMessage(new ChatComponentTranslation(S2BPacketChangeGameState.MESSAGE_NAMES[i]));
        }

        switch (i) {
            case 1 -> {
                this.clientWorldController.getWorldInfo().setRaining(true);
                this.clientWorldController.setRainStrength(0.0F);
            }
            case 2 -> {
                this.clientWorldController.getWorldInfo().setRaining(false);
                this.clientWorldController.setRainStrength(1.0F);
            }
            case 3 -> this.gameController.playerController.setGameType(WorldSettings.GameType.getByID(j));
            case 4 -> this.gameController.displayGuiScreen(new GuiWinGame()); // TODO: Only allow once per session
            // BUGFIX: Action 5, Shows Demo Screen
            case 6 ->
                    this.clientWorldController.playSound(entityplayer.posX, entityplayer.posY + entityplayer.getEyeHeight(), entityplayer.posZ, "random.successful_hit", 0.18F, 0.45F, false);
            case 7 -> // BUGFIX: HIGH VALUE -> LAG/CRASH | LOW VALUE -> WORLD COLOR CHANGES
                    this.clientWorldController.setRainStrength(Math.clamp(f, -2.0F, 2F)); // Allow leniency for servers to use
            case 8 -> this.clientWorldController.setThunderStrength(f);
            case 10 -> {
                this.clientWorldController.spawnParticle(ParticleTypes.MOB_APPEARANCE, entityplayer.posX, entityplayer.posY, entityplayer.posZ, 0.0D, 0.0D, 0.0D);
                this.clientWorldController.playSound(entityplayer.posX, entityplayer.posY, entityplayer.posZ, "mob.guardian.curse", 1.0F, 1.0F, false);
            }
        }
    }

    public void handleMaps(S34PacketMaps packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        MapData mapdata = ItemMap.loadMapData(packet.getMapId(), this.gameController.world);
        packet.setMapdataTo(mapdata);
        this.gameController.entityRenderer.getMapItemRenderer().updateMapTexture(mapdata);
    }

    public void handleEffect(S28PacketEffect packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if (packet.isSoundServerwide()) {
            this.gameController.world.playBroadcastSound(packet.getSoundType(), packet.getSoundPos(), packet.getSoundData());
        } else {
            this.gameController.world.playAuxSFX(packet.getSoundType(), packet.getSoundPos(), packet.getSoundData());
        }
    }

    public void handleStatistics(S37PacketStatistics packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        boolean flag = false;

        for (Entry<StatBase, Integer> entry : packet.func_148974_c().entrySet()) {
            StatBase statbase = entry.getKey();
            int i = entry.getValue();

            if (statbase.isAchievement() && i > 0) {
                if (this.field_147308_k && this.gameController.player.getStatFileWriter().readStat(statbase) == 0) {
                    Achievement achievement = (Achievement) statbase;
                    this.gameController.guiAchievement.displayAchievement(achievement);

                    if (statbase == AchievementList.OPEN_INVENTORY) {
                        this.gameController.gameSettings.showInventoryAchievementHint = false;
                        this.gameController.gameSettings.saveOptions();
                    }
                }

                flag = true;
            }

            this.gameController.player.getStatFileWriter().unlockAchievement(this.gameController.player, statbase, i);
        }

        if (!this.field_147308_k && !flag && this.gameController.gameSettings.showInventoryAchievementHint) {
            this.gameController.guiAchievement.displayUnformattedAchievement(AchievementList.OPEN_INVENTORY);
        }

        this.field_147308_k = true;

        if (this.gameController.currentScreen instanceof IProgressMeter progressMeter) {
            progressMeter.doneLoading();
        }
    }

    public void handleEntityEffect(S1DPacketEntityEffect packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityId());

        if (entity instanceof EntityLivingBase entityLivingBase) {
            PotionEffect potioneffect = new PotionEffect(packet.getEffectId(), packet.getDuration(), packet.getAmplifier(), false, packet.func_179707_f());
            potioneffect.setPotionDurationMax(packet.func_149429_c());
            entityLivingBase.addPotionEffect(potioneffect);
        }
    }

    public void handleCombatEvent(S42PacketCombatEvent packet) {
    }// TODO: Possibly Fully remove packet?

    public void handleServerDifficulty(S41PacketServerDifficulty packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.world.getWorldInfo().setDifficulty(packet.getDifficulty());
        this.gameController.world.getWorldInfo().setDifficultyLocked(packet.isDifficultyLocked());
    }

    public void handleCamera(S43PacketCamera packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = packet.getEntity(this.clientWorldController);

        if (entity != null) {
            this.gameController.setRenderViewEntity(entity);
        }
    }

    public void handleWorldBorder(S44PacketWorldBorder packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        packet.func_179788_a(this.clientWorldController.getWorldBorder());
    }

    public void handleTitle(S45PacketTitle packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        S45PacketTitle.Type type = packet.getType();
        String title = null;
        String subtitle = null;
        String packetText = packet.getMessage() != null ? packet.getMessage().getFormattedText() : "";

        switch (type) {
            case TITLE -> title = packetText;
            case SUBTITLE -> subtitle = packetText;
            case RESET -> {
                this.gameController.ingameGUI.displayTitle("", "", -1, -1, -1);
                this.gameController.ingameGUI.setDefaultTitlesTimes();
                return;
            }
        }

        this.gameController.ingameGUI.displayTitle(title, subtitle, packet.getFadeInTime(), packet.getDisplayTime(), packet.getFadeOutTime());
    }

    public void handleSetCompressionLevel(S46PacketSetCompressionLevel packet) {
        if (!this.netManager.isLocalChannel()) {
            this.netManager.setCompressionThreshold(packet.getThreshold());
        }
    }

    public void handlePlayerListHeaderFooter(S47PacketPlayerListHeaderFooter packet) {
        this.gameController.ingameGUI.getTabList().setHeader(packet.getHeader().getFormattedText().isEmpty() ? null : packet.getHeader());
        this.gameController.ingameGUI.getTabList().setFooter(packet.getFooter().getFormattedText().isEmpty() ? null : packet.getFooter());
    }

    public void handleRemoveEntityEffect(S1EPacketRemoveEntityEffect packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityId());

        if (entity instanceof EntityLivingBase entityLiving) {
            entityLiving.removePotionEffectClient(packet.getEffectId());
        }
    }

    public void handlePlayerListItem(S38PacketPlayerListItem packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        for (S38PacketPlayerListItem.AddPlayerData s38packetplayerlistitem$addplayerdata : packet.getEntries()) {
            if (packet.getAction() == S38PacketPlayerListItem.Action.REMOVE_PLAYER) {
                this.playerInfoMap.remove(s38packetplayerlistitem$addplayerdata.getProfile().getId());
            } else {
                NetworkPlayerInfo networkplayerinfo = this.playerInfoMap.get(s38packetplayerlistitem$addplayerdata.getProfile().getId());

                if (packet.getAction() == S38PacketPlayerListItem.Action.ADD_PLAYER) {
                    networkplayerinfo = new NetworkPlayerInfo(s38packetplayerlistitem$addplayerdata);
                    this.playerInfoMap.put(networkplayerinfo.getGameProfile().getId(), networkplayerinfo);
                }

                if (networkplayerinfo != null) {
                    switch (packet.getAction()) {
                        case ADD_PLAYER:
                            networkplayerinfo.setGameType(s38packetplayerlistitem$addplayerdata.getGameMode());
                            networkplayerinfo.setResponseTime(s38packetplayerlistitem$addplayerdata.getPing());
                            break;

                        case UPDATE_GAME_MODE:
                            networkplayerinfo.setGameType(s38packetplayerlistitem$addplayerdata.getGameMode());
                            break;

                        case UPDATE_LATENCY:
                            networkplayerinfo.setResponseTime(s38packetplayerlistitem$addplayerdata.getPing());
                            break;

                        case UPDATE_DISPLAY_NAME:
                            networkplayerinfo.setDisplayName(s38packetplayerlistitem$addplayerdata.getDisplayName());
                    }
                }
            }
        }
    }

    public void handleKeepAlive(S00PacketKeepAlive packet) {
        this.addToSendQueue(new C00PacketKeepAlive(packet.func_149134_c()));
    }

    public void handlePlayerAbilities(S39PacketPlayerAbilities packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        EntityPlayer entityplayer = this.gameController.player;
        entityplayer.capabilities.isFlying = packet.isFlying();
        entityplayer.capabilities.isCreativeMode = packet.isCreativeMode();
        entityplayer.capabilities.disableDamage = packet.isInvulnerable();
        entityplayer.capabilities.allowFlying = packet.isAllowFlying();
        entityplayer.capabilities.setFlySpeed(packet.getFlySpeed());
        entityplayer.capabilities.setPlayerWalkSpeed(packet.getWalkSpeed());
    }

    public void handleTabComplete(S3APacketTabComplete packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        String[] astring = packet.func_149630_c();

        if (this.gameController.currentScreen instanceof GuiChat guichat) {
            guichat.onAutocompleteResponse(astring);
        }
    }

    public void handleSoundEffect(S29PacketSoundEffect packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        this.gameController.world.playSound(packet.getX(), packet.getY(), packet.getZ(), packet.getSoundName(), packet.getVolume(), packet.getPitch(), false);
    }

    public void handleResourcePack(S48PacketResourcePackSend packet) {
        final String url = packet.getURL();
        final String hash = packet.getHash();

        try { // BUGFIX: Resource Pack Traversal Exploit
            // Check for unsupported protocols
            if (!url.matches("(http|https|level)://+.*")) {
                netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
                throw new URISyntaxException(url, "Unsupported Protocol");
            }


            if (url.startsWith("level://")) {
                String s2 = url.substring("level://".length());

                // Check for invalid path
                String decode = URLDecoder.decode(s2, StandardCharsets.UTF_8);
                if (decode.contains("..") || !decode.endsWith("/resources.zip")) {
                    throw new URISyntaxException(url, "Invalid level storage resource pack path");
                }

                File file1 = new File(this.gameController.mcDataDir, "saves");
                File file2 = new File(file1, s2);

                if (file2.isFile()) {
                    this.netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                    Futures.addCallback(this.gameController.getResourcePackRepository().setResourcePackInstance(file2), new FutureCallback<>() {
                        public void onSuccess(Object throwable) {
                            netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
                        }

                        public void onFailure(Throwable throwable) {
                            netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
                        }
                    }, Runnable::run);
                } else {
                    this.netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
                }
            } else {
                if (this.gameController.getCurrentServerData() != null && this.gameController.getCurrentServerData().getResourceMode() == ServerData.ServerResourceMode.ENABLED) {
                    this.netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                    Futures.addCallback(this.gameController.getResourcePackRepository().downloadResourcePack(url, hash), new FutureCallback<>() {
                        public void onSuccess(Object throwable) {
                            netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
                        }

                        public void onFailure(Throwable throwable) {
                            netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
                        }
                    }, Runnable::run);
                } else if (this.gameController.getCurrentServerData() != null && this.gameController.getCurrentServerData().getResourceMode() != ServerData.ServerResourceMode.PROMPT) {
                    this.netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.DECLINED));
                } else {
                    this.gameController.addScheduledTask(() -> gameController.displayGuiScreen(new GuiYesNo((result, id) -> {
                        this.gameController = Minecraft.get();

                        if (result) {
                            if (gameController.getCurrentServerData() != null) {
                                gameController.getCurrentServerData().setResourceMode(ServerData.ServerResourceMode.ENABLED);
                            }

                            netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.ACCEPTED));
                            Futures.addCallback(this.gameController.getResourcePackRepository().downloadResourcePack(url, hash), new FutureCallback<>() {
                                public void onSuccess(Object throwable) {
                                    netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.SUCCESSFULLY_LOADED));
                                }

                                public void onFailure(Throwable throwable) {
                                    netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.FAILED_DOWNLOAD));
                                }
                            }, Runnable::run);
                        } else {
                            if (gameController.getCurrentServerData() != null) {
                                gameController.getCurrentServerData().setResourceMode(ServerData.ServerResourceMode.DISABLED);
                            }

                            netManager.sendPacket(new C19PacketResourcePackStatus(hash, C19PacketResourcePackStatus.Action.DECLINED));
                        }

                        ServerList.func_147414_b(gameController.getCurrentServerData());
                        gameController.displayGuiScreen(null);
                    }, I18n.format("multiplayer.texturePrompt.line1"), I18n.format("multiplayer.texturePrompt.line2"), 0)));
                }
            }
        } catch (URISyntaxException exception) {
            exception.printStackTrace();
        }
    }

    public void handleEntityNBT(S49PacketUpdateEntityNBT packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = packet.getEntity(this.clientWorldController);

        if (entity != null) {
            entity.clientUpdateEntityNBT(packet.getTagCompound());
        }
    }

    public void handleCustomPayload(S3FPacketCustomPayload packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if ("MC|TrList".equals(packet.getChannel())) {
            PacketBuffer buffer = packet.getData();

            try {
                int windowID = buffer.readInt();
                GuiScreen screen = this.gameController.currentScreen;

                if (screen instanceof GuiMerchant gui && windowID == this.gameController.player.openContainer.windowId) {
                    IMerchant merchant = gui.getMerchant();
                    MerchantRecipeList recipes = MerchantRecipeList.readFromBuf(buffer);
                    merchant.setRecipes(recipes);
                }
            } catch (IOException exception) {
                LOGGER.error("Couldn't load trade info", exception);
            } finally {
                buffer.release();
            }
        } else if ("MC|Brand".equals(packet.getChannel())) {
            this.gameController.player.setClientBrand(packet.getData().readStringFromBuffer(32767));
        } else if ("MC|BOpen".equals(packet.getChannel())) {
            ItemStack itemstack = this.gameController.player.getCurrentEquippedItem();

            if (itemstack != null && itemstack.getItem() == Items.WRITTEN_BOOK) {
                this.gameController.displayGuiScreen(new GuiScreenBook(this.gameController.player, itemstack, false));
            }
        }
    }

    public void handleScoreboardObjective(S3BPacketScoreboardObjective packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Scoreboard scoreboard = this.clientWorldController.getScoreboard();

        if (packet.func_149338_e() == 0) {
            ScoreObjective objective = scoreboard.addScoreObjective(packet.func_149339_c(), IScoreObjectiveCriteria.DUMMY);
            objective.setDisplayName(packet.func_149337_d());
            objective.setRenderType(packet.func_179817_d());
        } else {
            ScoreObjective objective = scoreboard.getObjective(packet.func_149339_c());

            if (packet.func_149338_e() == 1) {
                scoreboard.removeObjective(objective);
            } else if (packet.func_149338_e() == 2) {
                objective.setDisplayName(packet.func_149337_d());
                objective.setRenderType(packet.func_179817_d());
            }
        }
    }

    public void handleUpdateScore(S3CPacketUpdateScore packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Scoreboard scoreboard = this.clientWorldController.getScoreboard();
        ScoreObjective objective = scoreboard.getObjective(packet.getObjectiveName());

        if (packet.getScoreAction() == S3CPacketUpdateScore.Action.CHANGE) {
            Score score = scoreboard.getValueFromObjective(packet.getPlayerName(), objective);
            score.setScorePoints(packet.getScoreValue());
        } else if (packet.getScoreAction() == S3CPacketUpdateScore.Action.REMOVE) {
            if (StringUtils.isNullOrEmpty(packet.getObjectiveName())) {
                scoreboard.removeObjectiveFromEntity(packet.getPlayerName(), null);
            } else if (objective != null) {
                scoreboard.removeObjectiveFromEntity(packet.getPlayerName(), objective);
            }
        }
    }

    public void handleDisplayScoreboard(S3DPacketDisplayScoreboard packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Scoreboard scoreboard = this.clientWorldController.getScoreboard();

        if (packet.func_149370_d().isEmpty()) {
            scoreboard.setObjectiveInDisplaySlot(packet.func_149371_c(), null);
        } else {
            ScoreObjective objective = scoreboard.getObjective(packet.func_149370_d());
            scoreboard.setObjectiveInDisplaySlot(packet.func_149371_c(), objective);
        }
    }

    public void handleTeams(S3EPacketTeams packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Scoreboard scoreboard = this.clientWorldController.getScoreboard();
        ScorePlayerTeam playerTeam;

        if (packet.getAction() == 0) {
            playerTeam = scoreboard.createTeam(packet.getName());
        } else {
            playerTeam = scoreboard.getTeam(packet.getName());
        }

        if (playerTeam == null) return;

        if (packet.getAction() == 0 || packet.getAction() == 2) {
            playerTeam.setTeamName(packet.getDisplayName());
            playerTeam.setNamePrefix(packet.getPrefix());
            playerTeam.setNameSuffix(packet.getSuffix());
            playerTeam.setChatFormat(Formatting.func_175744_a(packet.getColor()));
            playerTeam.func_98298_a(packet.getFriendlyFlags());
            Team.EnumVisible teamVisibility = Team.EnumVisible.func_178824_a(packet.getNameTagVisibility());

            if (teamVisibility != null) {
                playerTeam.setNameTagVisibility(teamVisibility);
            }
        }

        if (packet.getAction() == 0 || packet.getAction() == 3) {
            for (String name : packet.getPlayers()) {
                scoreboard.addPlayerToTeam(name, packet.getName());
            }
        }

        if (packet.getAction() == 4) {
            for (String name : packet.getPlayers()) {
                scoreboard.removePlayerFromTeam(name, playerTeam);
            }
        }

        if (packet.getAction() == 1) {
            scoreboard.removeTeam(playerTeam);
        }
    }

    public void handleParticles(S2APacketParticles packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);

        if (packet.getParticleCount() == 0) {
            double x = (packet.getParticleSpeed() * packet.getXOffset());
            double y = (packet.getParticleSpeed() * packet.getYOffset());
            double z = (packet.getParticleSpeed() * packet.getZOffset());

            try {
                this.clientWorldController.spawnParticle(packet.getParticleType(), packet.isLongDistance(), packet.getXCoordinate(), packet.getYCoordinate(), packet.getZCoordinate(), x, y, z, packet.getParticleArgs());
            } catch (Throwable throwable) {
                LOGGER.warn("Could not spawn particle effect {}", packet.getParticleType());
            }
        } else {
            for (int i = 0; i < packet.getParticleCount(); ++i) {
                double d1 = this.avRandomizer.nextGaussian() * packet.getXOffset();
                double d3 = this.avRandomizer.nextGaussian() * packet.getYOffset();
                double d5 = this.avRandomizer.nextGaussian() * packet.getZOffset();
                double d6 = this.avRandomizer.nextGaussian() * packet.getParticleSpeed();
                double d7 = this.avRandomizer.nextGaussian() * packet.getParticleSpeed();
                double d8 = this.avRandomizer.nextGaussian() * packet.getParticleSpeed();

                try {
                    this.clientWorldController.spawnParticle(packet.getParticleType(), packet.isLongDistance(), packet.getXCoordinate() + d1, packet.getYCoordinate() + d3, packet.getZCoordinate() + d5, d6, d7, d8, packet.getParticleArgs());
                } catch (Throwable throwable) {
                    LOGGER.warn("Could not spawn particle effect {}", packet.getParticleType());
                    return;
                }
            }
        }
    }

    public void handleEntityProperties(S20PacketEntityProperties packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.gameController);
        Entity entity = this.clientWorldController.getEntityByID(packet.getEntityId());

        if (entity != null) {
            if (!(entity instanceof EntityLivingBase entityLivingBase)) {
                throw new IllegalStateException("Server tried to update attributes of a non-living entity (actually: " + entity + ")");
            } else {
                BaseAttributeMap attributeMap = entityLivingBase.getAttributeMap();

                for (S20PacketEntityProperties.Snapshot snapshot : packet.func_149441_d()) {
                    IAttributeInstance attribute = attributeMap.getAttributeInstanceByName(snapshot.func_151409_a());

                    if (attribute == null) {
                        attribute = attributeMap.registerAttribute(new RangedAttribute(null, snapshot.func_151409_a(), 0.0D, 2.2250738585072014E-308D, Double.MAX_VALUE));
                    }

                    attribute.setBaseValue(snapshot.func_151410_b());
                    attribute.removeAllModifiers();

                    for (AttributeModifier modifier : snapshot.func_151408_c()) {
                        attribute.applyModifier(modifier);
                    }
                }
            }
        }
    }

    public NetworkManager getNetworkManager() {
        return this.netManager;
    }

    public Collection<NetworkPlayerInfo> getPlayerInfoMap() {
        return this.playerInfoMap.values();
    }

    public NetworkPlayerInfo getPlayerInfo(UUID uuid) {
        return this.playerInfoMap.get(uuid);
    }

    public NetworkPlayerInfo getPlayerInfo(String name) {
        for (NetworkPlayerInfo playerInfo : this.playerInfoMap.values()) {
            if (playerInfo.getGameProfile().getName().equals(name)) {
                return playerInfo;
            }
        }

        return null;
    }

    public GameProfile getGameProfile() {
        return this.profile;
    }
}
