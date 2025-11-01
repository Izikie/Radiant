package net.minecraft.network;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Floats;
import com.google.common.util.concurrent.Futures;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ShortOpenHashMap;
import net.minecraft.block.material.Material;
import net.minecraft.command.server.CommandBlockLogic;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityMinecartCommandBlock;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.item.EntityXPOrb;
import net.minecraft.entity.passive.EntityHorse;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.entity.projectile.EntityArrow;
import net.minecraft.init.Items;
import net.minecraft.inventory.*;
import net.minecraft.item.ItemEditableBook;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemWritableBook;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.api.PacketBuffer;
import net.minecraft.network.packet.api.PacketThreadUtil;
import net.minecraft.network.packet.impl.play.INetHandlerPlayServer;
import net.minecraft.network.packet.impl.play.client.*;
import net.minecraft.network.packet.impl.play.server.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.UserListBansEntry;
import net.minecraft.stats.AchievementList;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.tileentity.TileEntityCommandBlock;
import net.minecraft.tileentity.TileEntitySign;
import net.minecraft.util.*;
import net.minecraft.util.chat.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.world.WorldServer;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public class NetHandlerPlayServer implements INetHandlerPlayServer, ITickable {
    private static final Logger LOGGER = LoggerFactory.getLogger(NetHandlerPlayServer.class);
    public final NetworkManager netManager;
    private final MinecraftServer serverController;
    public EntityPlayerMP playerEntity;
    private int networkTickCount;
    private int field_175090_f;
    private int floatingTickCount;
    private int lastPingTimeInt;
    private long lastPingTime;
    private long lastSentPingPacket;
    private int chatSpamThresholdCount;
    private int itemDropThreshold;
    private final Int2ShortOpenHashMap field_147372_n = new Int2ShortOpenHashMap();
    private double lastPosX;
    private double lastPosY;
    private double lastPosZ;
    private boolean hasMoved = true;

    public NetHandlerPlayServer(MinecraftServer server, NetworkManager networkManagerIn, EntityPlayerMP playerIn) {
        this.serverController = server;
        this.netManager = networkManagerIn;
        networkManagerIn.setNetHandler(this);
        this.playerEntity = playerIn;
        playerIn.playerNetServerHandler = this;
    }

    @Override
    public void update() {
        ++this.networkTickCount;

        if (this.networkTickCount - this.lastSentPingPacket > 40L) {
            this.lastSentPingPacket = this.networkTickCount;
            this.lastPingTime = this.currentTimeMillis();
            this.lastPingTimeInt = (int) this.lastPingTime;
            this.sendPacket(new S00PacketKeepAlive(this.lastPingTimeInt));
        }


        if (this.chatSpamThresholdCount > 0) {
            --this.chatSpamThresholdCount;
        }

        if (this.itemDropThreshold > 0) {
            --this.itemDropThreshold;
        }

        if (this.playerEntity.getLastActiveTime() > 0L && this.serverController.getMaxPlayerIdleMinutes() > 0 && MinecraftServer.getCurrentTimeMillis() - this.playerEntity.getLastActiveTime() > ((long) this.serverController.getMaxPlayerIdleMinutes() * 1000 * 60)) {
            this.kickPlayerFromServer("You have been idle for too long!");
        }
    }

    public NetworkManager getNetworkManager() {
        return this.netManager;
    }

    public void kickPlayerFromServer(String reason) {
        final ChatComponentText component = new ChatComponentText(reason);
        this.netManager.sendPacket(new S40PacketDisconnect(component), _ -> this.netManager.closeChannel(component));
        this.netManager.disableAutoRead();
        Futures.getUnchecked(this.serverController.addScheduledTask(this.netManager::checkDisconnected));
    }

    @Override
    public void processInput(C0CPacketInput packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.setEntityActionState(packet.getStrafeSpeed(), packet.getForwardSpeed(), packet.isJumping(), packet.isSneaking());
    }

    private boolean func_183006_b(C03PacketPlayer packet) {
        return !Doubles.isFinite(packet.getPositionX()) || !Doubles.isFinite(packet.getPositionY()) || !Doubles.isFinite(packet.getPositionZ()) || !Floats.isFinite(packet.getPitch()) || !Floats.isFinite(packet.getYaw());
    }

    @Override
    public void processPlayer(C03PacketPlayer packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());

        if (this.func_183006_b(packet)) {
            this.kickPlayerFromServer("Invalid move packet received");
        } else {
            WorldServer worldserver = this.serverController.worldServerForDimension(this.playerEntity.dimension);

            if (!this.playerEntity.playerConqueredTheEnd) {
                double d0 = this.playerEntity.posX;
                double d1 = this.playerEntity.posY;
                double d2 = this.playerEntity.posZ;
                double d3 = 0.0D;
                double d4 = packet.getPositionX() - this.lastPosX;
                double d5 = packet.getPositionY() - this.lastPosY;
                double d6 = packet.getPositionZ() - this.lastPosZ;

                if (packet.isMoving()) {
                    d3 = d4 * d4 + d5 * d5 + d6 * d6;

                    if (!this.hasMoved && d3 < 0.25D) {
                        this.hasMoved = true;
                    }
                }

                if (this.hasMoved) {
                    this.field_175090_f = this.networkTickCount;

                    if (this.playerEntity.ridingEntity != null) {
                        float f4 = this.playerEntity.rotationYaw;
                        float f = this.playerEntity.rotationPitch;
                        this.playerEntity.ridingEntity.updateRiderPosition();
                        double d16 = this.playerEntity.posX;
                        double d17 = this.playerEntity.posY;
                        double d18 = this.playerEntity.posZ;

                        if (packet.getRotating()) {
                            f4 = packet.getYaw();
                            f = packet.getPitch();
                        }

                        this.playerEntity.onGround = packet.isOnGround();
                        this.playerEntity.onUpdateEntity();
                        this.playerEntity.setPositionAndRotation(d16, d17, d18, f4, f);

                        if (this.playerEntity.ridingEntity != null) {
                            this.playerEntity.ridingEntity.updateRiderPosition();
                        }

                        this.serverController.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);

                        if (this.playerEntity.ridingEntity != null) {
                            if (d3 > 4.0D) {
                                Entity entity = this.playerEntity.ridingEntity;
                                this.playerEntity.playerNetServerHandler.sendPacket(new S18PacketEntityTeleport(entity));
                                this.setPlayerLocation(this.playerEntity.posX, this.playerEntity.posY, this.playerEntity.posZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                            }

                            this.playerEntity.ridingEntity.isAirBorne = true;
                        }

                        if (this.hasMoved) {
                            this.lastPosX = this.playerEntity.posX;
                            this.lastPosY = this.playerEntity.posY;
                            this.lastPosZ = this.playerEntity.posZ;
                        }

                        worldserver.updateEntity(this.playerEntity);
                        return;
                    }

                    if (this.playerEntity.isPlayerSleeping()) {
                        this.playerEntity.onUpdateEntity();
                        this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                        worldserver.updateEntity(this.playerEntity);
                        return;
                    }

                    double d7 = this.playerEntity.posY;
                    this.lastPosX = this.playerEntity.posX;
                    this.lastPosY = this.playerEntity.posY;
                    this.lastPosZ = this.playerEntity.posZ;
                    double d8 = this.playerEntity.posX;
                    double d9 = this.playerEntity.posY;
                    double d10 = this.playerEntity.posZ;
                    float f1 = this.playerEntity.rotationYaw;
                    float f2 = this.playerEntity.rotationPitch;

                    if (packet.isMoving() && packet.getPositionY() == -999.0D) {
                        packet.setMoving(false);
                    }

                    if (packet.isMoving()) {
                        d8 = packet.getPositionX();
                        d9 = packet.getPositionY();
                        d10 = packet.getPositionZ();

                        if (Math.abs(packet.getPositionX()) > 3.0E7D || Math.abs(packet.getPositionZ()) > 3.0E7D) {
                            this.kickPlayerFromServer("Illegal position");
                            return;
                        }
                    }

                    if (packet.getRotating()) {
                        f1 = packet.getYaw();
                        f2 = packet.getPitch();
                    }

                    this.playerEntity.onUpdateEntity();
                    this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, f1, f2);

                    if (!this.hasMoved) {
                        return;
                    }

                    double d11 = d8 - this.playerEntity.posX;
                    double d12 = d9 - this.playerEntity.posY;
                    double d13 = d10 - this.playerEntity.posZ;
                    double d14 = this.playerEntity.motionX * this.playerEntity.motionX + this.playerEntity.motionY * this.playerEntity.motionY + this.playerEntity.motionZ * this.playerEntity.motionZ;
                    double d15 = d11 * d11 + d12 * d12 + d13 * d13;

                    if (d15 - d14 > 100.0D && (!this.serverController.isSinglePlayer() || !this.serverController.getServerOwner().equals(this.playerEntity.getName()))) {
                        LOGGER.warn("{} moved too quickly! {},{},{} ({}, {}, {})", this.playerEntity.getName(), d11, d12, d13, d11, d12, d13);
                        this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                        return;
                    }

                    float f3 = 0.0625F;
                    boolean flag = worldserver.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.getEntityBoundingBox().contract(f3, f3, f3)).isEmpty();

                    if (this.playerEntity.onGround && !packet.isOnGround() && d12 > 0.0D) {
                        this.playerEntity.jump();
                    }

                    this.playerEntity.moveEntity(d11, d12, d13);
                    this.playerEntity.onGround = packet.isOnGround();
                    d11 = d8 - this.playerEntity.posX;
                    d12 = d9 - this.playerEntity.posY;

                    if (d12 > -0.5D || d12 < 0.5D) {
                        d12 = 0.0D;
                    }

                    d13 = d10 - this.playerEntity.posZ;
                    d15 = d11 * d11 + d12 * d12 + d13 * d13;
                    boolean flag1 = false;

                    if (d15 > 0.0625D && !this.playerEntity.isPlayerSleeping() && !this.playerEntity.theItemInWorldManager.isCreative()) {
                        flag1 = true;
                        LOGGER.warn("{} moved wrongly!", this.playerEntity.getName());
                    }

                    this.playerEntity.setPositionAndRotation(d8, d9, d10, f1, f2);
                    this.playerEntity.addMovementStat(this.playerEntity.posX - d0, this.playerEntity.posY - d1, this.playerEntity.posZ - d2);

                    if (!this.playerEntity.noClip) {
                        boolean flag2 = worldserver.getCollidingBoundingBoxes(this.playerEntity, this.playerEntity.getEntityBoundingBox().contract(f3, f3, f3)).isEmpty();

                        if (flag && (flag1 || !flag2) && !this.playerEntity.isPlayerSleeping()) {
                            this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, f1, f2);
                            return;
                        }
                    }

                    AxisAlignedBB axisalignedbb = this.playerEntity.getEntityBoundingBox().expand(f3, f3, f3).addCoord(0.0D, -0.55D, 0.0D);

                    if (!this.serverController.isFlightAllowed() && !this.playerEntity.capabilities.allowFlying && !worldserver.checkBlockCollision(axisalignedbb)) {
                        if (d12 >= -0.03125D) {
                            ++this.floatingTickCount;

                            if (this.floatingTickCount > 80) {
                                LOGGER.warn("{} was kicked for floating too long!", this.playerEntity.getName());
                                this.kickPlayerFromServer("Flying is not enabled on this server");
                                return;
                            }
                        }
                    } else {
                        this.floatingTickCount = 0;
                    }

                    this.playerEntity.onGround = packet.isOnGround();
                    this.serverController.getConfigurationManager().serverUpdateMountedMovingPlayer(this.playerEntity);
                    this.playerEntity.handleFalling(this.playerEntity.posY - d7, packet.isOnGround());
                } else if (this.networkTickCount - this.field_175090_f > 20) {
                    this.setPlayerLocation(this.lastPosX, this.lastPosY, this.lastPosZ, this.playerEntity.rotationYaw, this.playerEntity.rotationPitch);
                }
            }
        }
    }

    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch) {
        this.setPlayerLocation(x, y, z, yaw, pitch, EnumSet.noneOf(S08PacketPlayerPosLook.Flag.class));
    }

    public void setPlayerLocation(double x, double y, double z, float yaw, float pitch, EnumSet<S08PacketPlayerPosLook.Flag> relativeSet) {
        this.hasMoved = false;
        this.lastPosX = x;
        this.lastPosY = y;
        this.lastPosZ = z;

        if (relativeSet.contains(S08PacketPlayerPosLook.Flag.X)) {
            this.lastPosX += this.playerEntity.posX;
        }

        if (relativeSet.contains(S08PacketPlayerPosLook.Flag.Y)) {
            this.lastPosY += this.playerEntity.posY;
        }

        if (relativeSet.contains(S08PacketPlayerPosLook.Flag.Z)) {
            this.lastPosZ += this.playerEntity.posZ;
        }

        float f = yaw;
        float f1 = pitch;

        if (relativeSet.contains(S08PacketPlayerPosLook.Flag.Y_ROT)) {
            f = yaw + this.playerEntity.rotationYaw;
        }

        if (relativeSet.contains(S08PacketPlayerPosLook.Flag.X_ROT)) {
            f1 = pitch + this.playerEntity.rotationPitch;
        }

        this.playerEntity.setPositionAndRotation(this.lastPosX, this.lastPosY, this.lastPosZ, f, f1);
        this.playerEntity.playerNetServerHandler.sendPacket(new S08PacketPlayerPosLook(x, y, z, yaw, pitch, relativeSet));
    }

    @Override
    public void processPlayerDigging(C07PacketPlayerDigging packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        WorldServer worldServer = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        BlockPos blockPos = packet.getPosition();
        this.playerEntity.markPlayerActive();

        switch (packet.getStatus()) {
            case DROP_ITEM:
                if (!this.playerEntity.isSpectator()) {
                    this.playerEntity.dropOneItem(false);
                }

                return;

            case DROP_ALL_ITEMS:
                if (!this.playerEntity.isSpectator()) {
                    this.playerEntity.dropOneItem(true);
                }

                return;

            case RELEASE_USE_ITEM:
                this.playerEntity.stopUsingItem();
                return;

            case START_DESTROY_BLOCK:
            case ABORT_DESTROY_BLOCK:
            case STOP_DESTROY_BLOCK:
                double d0 = this.playerEntity.posX - (blockPos.getX() + 0.5D);
                double d1 = this.playerEntity.posY - (blockPos.getY() + 0.5D) + 1.5D;
                double d2 = this.playerEntity.posZ - (blockPos.getZ() + 0.5D);
                double d3 = d0 * d0 + d1 * d1 + d2 * d2;

                if (!(d3 > 36.0D) && blockPos.getY() < this.serverController.getBuildLimit()) {
                    if (packet.getStatus() == C07PacketPlayerDigging.Action.START_DESTROY_BLOCK) {
                        if (!this.serverController.isBlockProtected(worldServer, blockPos, this.playerEntity) && worldServer.getWorldBorder().contains(blockPos)) {
                            this.playerEntity.theItemInWorldManager.onBlockClicked(blockPos, packet.getFacing());
                        } else {
                            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldServer, blockPos));
                        }
                    } else {
                        if (packet.getStatus() == C07PacketPlayerDigging.Action.STOP_DESTROY_BLOCK) {
                            this.playerEntity.theItemInWorldManager.blockRemoving(blockPos);
                        } else if (packet.getStatus() == C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK) {
                            this.playerEntity.theItemInWorldManager.cancelDestroyingBlock();
                        }

                        if (worldServer.getBlockState(blockPos).getBlock().getMaterial() != Material.AIR) {
                            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldServer, blockPos));
                        }
                    }

                }
                return;

            default:
                throw new IllegalArgumentException("Invalid player action");
        }
    }

    @Override
    public void processPlayerBlockPlacement(C08PacketPlayerBlockPlacement packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        WorldServer worldServer = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        ItemStack itemStack = this.playerEntity.inventory.getCurrentItem();
        boolean flag = false;
        BlockPos blockPos = packet.getPosition();
        Direction direction = Direction.getFront(packet.getPlacedBlockDirection());
        this.playerEntity.markPlayerActive();

        if (packet.getPlacedBlockDirection() == 255) {
            if (itemStack == null) {
                return;
            }

            this.playerEntity.theItemInWorldManager.tryUseItem(this.playerEntity, worldServer, itemStack);
        } else if (blockPos.getY() < this.serverController.getBuildLimit() - 1 || direction != Direction.UP && blockPos.getY() < this.serverController.getBuildLimit()) {
            if (this.hasMoved && this.playerEntity.getDistanceSq(blockPos.getX() + 0.5D, blockPos.getY() + 0.5D, blockPos.getZ() + 0.5D) < 64.0D && !this.serverController.isBlockProtected(worldServer, blockPos, this.playerEntity) && worldServer.getWorldBorder().contains(blockPos)) {
                this.playerEntity.theItemInWorldManager.activateBlockOrUseItem(this.playerEntity, worldServer, itemStack, blockPos, direction, packet.getPlacedBlockOffsetX(), packet.getPlacedBlockOffsetY(), packet.getPlacedBlockOffsetZ());
            }

            flag = true;
        } else {
            ChatComponentTranslation chatcomponenttranslation = new ChatComponentTranslation("build.tooHigh", this.serverController.getBuildLimit());
            chatcomponenttranslation.getChatStyle().setColor(Formatting.RED);
            this.playerEntity.playerNetServerHandler.sendPacket(new S02PacketChat(chatcomponenttranslation));
            flag = true;
        }

        if (flag) {
            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldServer, blockPos));
            this.playerEntity.playerNetServerHandler.sendPacket(new S23PacketBlockChange(worldServer, blockPos.offset(direction)));
        }

        itemStack = this.playerEntity.inventory.getCurrentItem();

        if (itemStack != null && itemStack.stackSize == 0) {
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = null;
            itemStack = null;
        }

        if (itemStack == null || itemStack.getMaxItemUseDuration() == 0) {
            this.playerEntity.isChangingQuantityOnly = true;
            this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem] = ItemStack.copyItemStack(this.playerEntity.inventory.mainInventory[this.playerEntity.inventory.currentItem]);
            Slot slot = this.playerEntity.openContainer.getSlotFromInventory(this.playerEntity.inventory, this.playerEntity.inventory.currentItem);
            this.playerEntity.openContainer.detectAndSendChanges();
            this.playerEntity.isChangingQuantityOnly = false;

            if (!ItemStack.areItemStacksEqual(this.playerEntity.inventory.getCurrentItem(), packet.getStack())) {
                this.sendPacket(new S2FPacketSetSlot(this.playerEntity.openContainer.windowId, slot.slotNumber, this.playerEntity.inventory.getCurrentItem()));
            }
        }
    }

    @Override
    public void handleSpectate(C18PacketSpectate packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());

        if (this.playerEntity.isSpectator()) {
            Entity entity = null;

            for (WorldServer worldServer : this.serverController.worldServers) {
                if (worldServer != null) {
                    entity = packet.getEntity(worldServer);

                    if (entity != null) {
                        break;
                    }
                }
            }

            if (entity != null) {
                this.playerEntity.setSpectatingEntity(this.playerEntity);
                this.playerEntity.mountEntity(null);

                if (entity.worldObj != this.playerEntity.worldObj) {
                    WorldServer serverForPlayer = this.playerEntity.getServerForPlayer();
                    WorldServer entityWorld = (WorldServer) entity.worldObj;
                    this.playerEntity.dimension = entity.dimension;
                    this.sendPacket(new S07PacketRespawn(this.playerEntity.dimension, serverForPlayer.getDifficulty(), serverForPlayer.getWorldInfo().getTerrainType(), this.playerEntity.theItemInWorldManager.getGameType()));
                    serverForPlayer.removePlayerEntityDangerously(this.playerEntity);
                    this.playerEntity.isDead = false;
                    this.playerEntity.setLocationAndAngles(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch);

                    if (this.playerEntity.isEntityAlive()) {
                        serverForPlayer.updateEntityWithOptionalForce(this.playerEntity, false);
                        entityWorld.spawnEntityInWorld(this.playerEntity);
                        entityWorld.updateEntityWithOptionalForce(this.playerEntity, false);
                    }

                    this.playerEntity.setWorld(entityWorld);
                    this.serverController.getConfigurationManager().preparePlayer(this.playerEntity, serverForPlayer);
                    this.playerEntity.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
                    this.playerEntity.theItemInWorldManager.setWorld(entityWorld);
                    this.serverController.getConfigurationManager().updateTimeAndWeatherForPlayer(this.playerEntity, entityWorld);
                    this.serverController.getConfigurationManager().syncPlayerInventory(this.playerEntity);
                } else {
                    this.playerEntity.setPositionAndUpdate(entity.posX, entity.posY, entity.posZ);
                }
            }
        }
    }

    @Override
    public void handleResourcePackStatus(C19PacketResourcePackStatus packet) {
    }

    @Override
    public void onDisconnect(IChatComponent reason) {
        LOGGER.info("{} lost connection: {}", this.playerEntity.getName(), reason);
        this.serverController.refreshStatusNextTick();
        ChatComponentTranslation componentTranslation = new ChatComponentTranslation("multiplayer.player.left", this.playerEntity.getDisplayName());
        componentTranslation.getChatStyle().setColor(Formatting.YELLOW);
        this.serverController.getConfigurationManager().sendChatMsg(componentTranslation);
        this.playerEntity.mountEntityAndWakeUp();
        this.serverController.getConfigurationManager().playerLoggedOut(this.playerEntity);

        if (this.serverController.isSinglePlayer() && this.playerEntity.getName().equals(this.serverController.getServerOwner())) {
            LOGGER.info("Stopping singleplayer server as player logged out");
            this.serverController.initiateShutdown();
        }
    }

    public void sendPacket(Packet<?> packet) {
        if (packet instanceof S02PacketChat packetChat) {
            EntityPlayer.ChatVisibility chatVisibility = this.playerEntity.getChatVisibility();

            if (chatVisibility == EntityPlayer.ChatVisibility.HIDDEN) {
                return;
            }

            if (chatVisibility == EntityPlayer.ChatVisibility.SYSTEM && !packetChat.isChat()) {
                return;
            }
        }

        try {
            this.netManager.sendPacket(packet);
        } catch (Throwable throwable) {
            CrashReport report = CrashReport.makeCrashReport(throwable, "Sending packet");
            CrashReportCategory category = report.makeCategory("Packet being sent");
            category.addCrashSectionCallable("Packet Class", () -> packet.getClass().getCanonicalName());
            throw new ReportedException(report);
        }
    }

    @Override
    public void processHeldItemChange(C09PacketHeldItemChange packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());

        if (packet.getSlotId() >= 0 && packet.getSlotId() < InventoryPlayer.getHotbarSize()) {
            this.playerEntity.inventory.currentItem = packet.getSlotId();
            this.playerEntity.markPlayerActive();
        } else {
            LOGGER.warn("{} tried to set an invalid carried item", this.playerEntity.getName());
        }
    }

    @Override
    public void processChatMessage(C01PacketChatMessage packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());

        if (this.playerEntity.getChatVisibility() == EntityPlayer.ChatVisibility.HIDDEN) {
            ChatComponentTranslation componentTranslation = new ChatComponentTranslation("chat.cannotSend");
            componentTranslation.getChatStyle().setColor(Formatting.RED);
            this.sendPacket(new S02PacketChat(componentTranslation));
        } else {
            this.playerEntity.markPlayerActive();
            String message = StringUtils.normalizeSpace(packet.getMessage());

            for (int i = 0; i < message.length(); ++i) {
                if (!ChatAllowedCharacters.isAllowedCharacter(message.charAt(i))) {
                    this.kickPlayerFromServer("Illegal characters in chat");
                    return;
                }
            }

            if (message.startsWith("/")) {
                this.serverController.getCommandManager().executeCommand(this.playerEntity, message);
            } else {
                IChatComponent component = new ChatComponentTranslation("chat.type.text", this.playerEntity.getDisplayName(), message);
                this.serverController.getConfigurationManager().sendChatMsgImpl(component, false);
            }

            this.chatSpamThresholdCount += 20;

            if (this.chatSpamThresholdCount > 200 && !this.serverController.getConfigurationManager().canSendCommands(this.playerEntity.getGameProfile())) {
                this.kickPlayerFromServer("disconnect.spam");
            }
        }
    }

    @Override
    public void handleAnimation(C0APacketAnimation packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();
        this.playerEntity.swingItem();
    }

    @Override
    public void processEntityAction(C0BPacketEntityAction packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();

        switch (packet.getAction()) {
            case START_SNEAKING -> this.playerEntity.setSneaking(true);
            case STOP_SNEAKING -> this.playerEntity.setSneaking(false);
            case START_SPRINTING -> this.playerEntity.setSprinting(true);
            case STOP_SPRINTING -> this.playerEntity.setSprinting(false);
            case STOP_SLEEPING -> {
                this.playerEntity.wakeUpPlayer(false, true, true);
                this.hasMoved = false;
            }
            case RIDING_JUMP -> {
                if (this.playerEntity.ridingEntity instanceof EntityHorse horse) {
                    horse.setJumpPower(packet.getAuxData());
                }
            }
            case OPEN_INVENTORY -> {
                if (this.playerEntity.ridingEntity instanceof EntityHorse entityHorse) {
                    entityHorse.openGUI(this.playerEntity);
                }
            }
            default -> throw new IllegalArgumentException("Invalid client command!");
        }
    }

    @Override
    public void processUseEntity(C02PacketUseEntity packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        WorldServer worldServer = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        Entity entity = packet.getEntityFromWorld(worldServer);
        this.playerEntity.markPlayerActive();

        if (entity != null) {
            boolean entityVisible = this.playerEntity.canEntityBeSeen(entity);
            double d0 = 36.0D;

            if (!entityVisible) {
                d0 = 9.0D;
            }

            if (this.playerEntity.getDistanceSqToEntity(entity) < d0) {
                if (packet.getAction() == C02PacketUseEntity.Action.INTERACT) {
                    this.playerEntity.interactWith(entity);
                } else if (packet.getAction() == C02PacketUseEntity.Action.INTERACT_AT) {
                    entity.interactAt(this.playerEntity, packet.getHitVec());
                } else if (packet.getAction() == C02PacketUseEntity.Action.ATTACK) {
                    if (entity instanceof EntityItem || entity instanceof EntityXPOrb || entity instanceof EntityArrow || entity == this.playerEntity) {
                        this.kickPlayerFromServer("Attempting to attack an invalid entity");
                        this.serverController.logWarning("Player " + this.playerEntity.getName() + " tried to attack an invalid entity");
                        return;
                    }

                    this.playerEntity.attackTargetEntityWithCurrentItem(entity);
                }
            }
        }
    }

    @Override
    public void processClientStatus(C16PacketClientStatus packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();

        switch (packet.getStatus()) {
            case PERFORM_RESPAWN -> {
                if (this.playerEntity.playerConqueredTheEnd) {
                    this.playerEntity = this.serverController.getConfigurationManager().recreatePlayerEntity(this.playerEntity, 0, true);
                } else if (this.playerEntity.getServerForPlayer().getWorldInfo().isHardcoreModeEnabled()) {
                    if (this.serverController.isSinglePlayer() && this.playerEntity.getName().equals(this.serverController.getServerOwner())) {
                        this.serverController.deleteWorldAndStopServer();
                    } else {
                        UserListBansEntry banEntry = new UserListBansEntry(this.playerEntity.getGameProfile(), null, "(You just lost the game)", null, "Death in Hardcore");
                        this.serverController.getConfigurationManager().getBannedPlayers().addEntry(banEntry);
                    }

                    this.playerEntity.playerNetServerHandler.kickPlayerFromServer("You have died. Game over, man, it's game over!");
                } else {
                    if (this.playerEntity.getHealth() > 0.0F) {
                        return;
                    }

                    this.playerEntity = this.serverController.getConfigurationManager().recreatePlayerEntity(this.playerEntity, 0, false);
                }
            }
            case REQUEST_STATS -> this.playerEntity.getStatFile().func_150876_a(this.playerEntity);
            case OPEN_INVENTORY_ACHIEVEMENT -> this.playerEntity.triggerAchievement(AchievementList.OPEN_INVENTORY);
        }
    }

    @Override
    public void processCloseWindow(C0DPacketCloseWindow packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.closeContainer();
    }

    @Override
    public void processClickWindow(C0EPacketClickWindow packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();

        if (this.playerEntity.openContainer.windowId == packet.getWindowId() && this.playerEntity.openContainer.getCanCraft(this.playerEntity)) {
            if (this.playerEntity.isSpectator()) {
                List<ItemStack> list = new ArrayList<>();

                for (int i = 0; i < this.playerEntity.openContainer.inventorySlots.size(); ++i) {
                    list.add(this.playerEntity.openContainer.inventorySlots.get(i).getStack());
                }

                this.playerEntity.updateCraftingInventory(this.playerEntity.openContainer, list);
            } else {
                ItemStack itemStack = this.playerEntity.openContainer.slotClick(packet.getSlotId(), packet.getUsedButton(), packet.getMode(), this.playerEntity);

                if (ItemStack.areItemStacksEqual(packet.getClickedItem(), itemStack)) {
                    this.playerEntity.playerNetServerHandler.sendPacket(new S32PacketConfirmTransaction(packet.getWindowId(), packet.getActionNumber(), true));
                    this.playerEntity.isChangingQuantityOnly = true;
                    this.playerEntity.openContainer.detectAndSendChanges();
                    this.playerEntity.updateHeldItem();
                    this.playerEntity.isChangingQuantityOnly = false;
                } else {
                    this.field_147372_n.put(this.playerEntity.openContainer.windowId, packet.getActionNumber());
                    this.playerEntity.playerNetServerHandler.sendPacket(new S32PacketConfirmTransaction(packet.getWindowId(), packet.getActionNumber(), false));
                    this.playerEntity.openContainer.setCanCraft(this.playerEntity, false);
                    List<ItemStack> stacks = new ArrayList<>();

                    for (int j = 0; j < this.playerEntity.openContainer.inventorySlots.size(); ++j) {
                        stacks.add(this.playerEntity.openContainer.inventorySlots.get(j).getStack());
                    }

                    this.playerEntity.updateCraftingInventory(this.playerEntity.openContainer, stacks);
                }
            }
        }
    }

    @Override
    public void processEnchantItem(C11PacketEnchantItem packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();

        if (this.playerEntity.openContainer.windowId == packet.getWindowId() && this.playerEntity.openContainer.getCanCraft(this.playerEntity) && !this.playerEntity.isSpectator()) {
            this.playerEntity.openContainer.enchantItem(this.playerEntity, packet.getButton());
            this.playerEntity.openContainer.detectAndSendChanges();
        }
    }

    @Override
    public void processCreativeInventoryAction(C10PacketCreativeInventoryAction packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());

        if (!this.playerEntity.theItemInWorldManager.isCreative())
            return;

        boolean flag = packet.getSlotId() < 0;
        ItemStack itemStack = packet.getStack();

        if (itemStack != null && itemStack.hasTagCompound() && itemStack.getTagCompound().hasKey("BlockEntityTag", 10)) {
            NBTTagCompound nbtTag = itemStack.getTagCompound().getCompoundTag("BlockEntityTag");

            if (nbtTag.hasKey("x") && nbtTag.hasKey("y") && nbtTag.hasKey("z")) {
                BlockPos blockPos = new BlockPos(nbtTag.getInteger("x"), nbtTag.getInteger("y"), nbtTag.getInteger("z"));
                TileEntity tileEntity = this.playerEntity.worldObj.getTileEntity(blockPos);

                if (tileEntity != null) {
                    NBTTagCompound tagCompound = new NBTTagCompound();
                    tileEntity.writeToNBT(tagCompound);
                    tagCompound.removeTag("x");
                    tagCompound.removeTag("y");
                    tagCompound.removeTag("z");
                    itemStack.setTagInfo("BlockEntityTag", tagCompound);
                }
            }
        }

        boolean flag1 = packet.getSlotId() >= 1 && packet.getSlotId() < 36 + InventoryPlayer.getHotbarSize();
        boolean flag2 = itemStack == null || itemStack.getItem() != null;
        boolean flag3 = itemStack == null || itemStack.getMetadata() >= 0 && itemStack.stackSize <= 64 && itemStack.stackSize > 0;

        if (flag1 && flag2 && flag3) {
            this.playerEntity.inventoryContainer.putStackInSlot(packet.getSlotId(), itemStack);

            this.playerEntity.inventoryContainer.setCanCraft(this.playerEntity, true);
        } else if (flag && flag2 && flag3 && this.itemDropThreshold < 200) {
            this.itemDropThreshold += 20;
            EntityItem entityitem = this.playerEntity.dropPlayerItemWithRandomChoice(itemStack, true);

            if (entityitem != null) {
                entityitem.setAgeToCreativeDespawnTime();
            }
        }
    }

    @Override
    public void processConfirmTransaction(C0FPacketConfirmTransaction packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        Short window = this.field_147372_n.get(this.playerEntity.openContainer.windowId);

        if (window != null && packet.getUid() == window && this.playerEntity.openContainer.windowId == packet.getWindowId() && !this.playerEntity.openContainer.getCanCraft(this.playerEntity) && !this.playerEntity.isSpectator()) {
            this.playerEntity.openContainer.setCanCraft(this.playerEntity, true);
        }
    }

    @Override
    public void processUpdateSign(C12PacketUpdateSign packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.markPlayerActive();
        WorldServer worldServer = this.serverController.worldServerForDimension(this.playerEntity.dimension);
        BlockPos blockPos = packet.getPosition();

        if (worldServer.isBlockLoaded(blockPos)) {
            TileEntity tileEntity = worldServer.getTileEntity(blockPos);

            if (!(tileEntity instanceof TileEntitySign entitySign)) {
                return;
            }

            if (!entitySign.getIsEditable() || entitySign.getPlayer() != this.playerEntity) {
                this.serverController.logWarning("Player " + this.playerEntity.getName() + " just tried to change non-editable sign");
                return;
            }

            IChatComponent[] chatComponents = packet.getLines();

            for (int i = 0; i < chatComponents.length; ++i) {
                entitySign.signText[i] = new ChatComponentText(Formatting.getTextWithoutFormattingCodes(chatComponents[i].getUnformattedText()));
            }

            entitySign.markDirty();
            worldServer.markBlockForUpdate(blockPos);
        }
    }

    @Override
    public void processKeepAlive(C00PacketKeepAlive packet) {
        if (packet.getKey() == this.lastPingTimeInt) {
            int lastTime = (int) (this.currentTimeMillis() - this.lastPingTime);
            this.playerEntity.ping = (this.playerEntity.ping * 3 + lastTime) / 4;
        }
    }

    private long currentTimeMillis() {
        return System.nanoTime() / 1000000L;
    }

    @Override
    public void processPlayerAbilities(C13PacketPlayerAbilities packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.capabilities.isFlying = packet.isFlying() && this.playerEntity.capabilities.allowFlying;
    }

    @Override
    public void processTabComplete(C14PacketTabComplete packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());

        List<String> command = new ArrayList<>(this.serverController.getTabCompletions(this.playerEntity, packet.getMessage(), packet.getTargetBlock()));

        this.playerEntity.playerNetServerHandler.sendPacket(new S3APacketTabComplete(command.toArray(new String[0])));
    }

    @Override
    public void processClientSettings(C15PacketClientSettings packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());
        this.playerEntity.handleClientSettings(packet);
    }

    @Override
    public void processCustomPayload(C17PacketCustomPayload packet) {
        PacketThreadUtil.checkThreadAndEnqueue(packet, this, this.playerEntity.getServerForPlayer());

        if ("MC|BEdit".equals(packet.getChannel())) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(packet.getData()));

            try {
                ItemStack itemStack = buffer.readItemStackFromBuffer();

                if (itemStack != null) {
                    if (!ItemWritableBook.isNBTValid(itemStack.getTagCompound())) {
                        throw new IOException("Invalid book tag!");
                    }

                    ItemStack currentItem = this.playerEntity.inventory.getCurrentItem();

                    if (currentItem == null) {
                        return;
                    }

                    if (itemStack.getItem() == Items.WRITABLE_BOOK && itemStack.getItem() == currentItem.getItem()) {
                        currentItem.setTagInfo("pages", itemStack.getTagCompound().getTagList("pages", 8));
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Couldn't handle book info", exception);
            } finally {
                buffer.release();
            }
        } else if ("MC|BSign".equals(packet.getChannel())) {
            PacketBuffer buffer = new PacketBuffer(Unpooled.wrappedBuffer(packet.getData()));

            try {
                ItemStack itemStack = buffer.readItemStackFromBuffer();

                if (itemStack != null) {
                    if (!ItemEditableBook.validBookTagContents(itemStack.getTagCompound())) {
                        throw new IOException("Invalid book tag!");
                    }

                    ItemStack currentItem = this.playerEntity.inventory.getCurrentItem();

                    if (currentItem == null) {
                        return;
                    }

                    if (itemStack.getItem() == Items.WRITTEN_BOOK && currentItem.getItem() == Items.WRITABLE_BOOK) {
                        currentItem.setTagInfo("author", new NBTTagString(this.playerEntity.getName()));
                        currentItem.setTagInfo("title", new NBTTagString(itemStack.getTagCompound().getString("title")));
                        currentItem.setTagInfo("pages", itemStack.getTagCompound().getTagList("pages", 8));
                        currentItem.setItem(Items.WRITTEN_BOOK);
                    }
                }
            } catch (Exception exception) {
                LOGGER.error("Couldn't sign book", exception);
            } finally {
                buffer.release();
            }
        } else if ("MC|TrSel".equals(packet.getChannel())) {
            try {
                int i = packet.getData().readInt();
                Container container = this.playerEntity.openContainer;

                if (container instanceof ContainerMerchant containerMerchant) {
                    containerMerchant.setCurrentRecipeIndex(i);
                }
            } catch (Exception exception) {
                LOGGER.error("Couldn't select trade", exception);
            }
        } else if ("MC|AdvCdm".equals(packet.getChannel())) {
            if (!this.serverController.isCommandBlockEnabled()) {
                this.playerEntity.addChatMessage(new ChatComponentTranslation("advMode.notEnabled"));
            } else if (this.playerEntity.canCommandSenderUseCommand(2, "") && this.playerEntity.capabilities.isCreativeMode) {
                PacketBuffer buffer = packet.getData();

                try {
                    int j = buffer.readByte();
                    CommandBlockLogic commandblocklogic = null;

                    if (j == 0) {
                        TileEntity tileentity = this.playerEntity.worldObj.getTileEntity(new BlockPos(buffer.readInt(), buffer.readInt(), buffer.readInt()));

                        if (tileentity instanceof TileEntityCommandBlock tileEntityCommandBlock) {
                            commandblocklogic = tileEntityCommandBlock.getCommandBlockLogic();
                        }
                    } else if (j == 1) {
                        Entity entity = this.playerEntity.worldObj.getEntityByID(buffer.readInt());

                        if (entity instanceof EntityMinecartCommandBlock entityMinecartCommandBlock) {
                            commandblocklogic = entityMinecartCommandBlock.getCommandBlockLogic();
                        }
                    }

                    String s1 = buffer.readStringFromBuffer(buffer.readableBytes());
                    boolean flag = buffer.readBoolean();

                    if (commandblocklogic != null) {
                        commandblocklogic.setCommand(s1);
                        commandblocklogic.setTrackOutput(flag);

                        if (!flag) {
                            commandblocklogic.setLastOutput(null);
                        }

                        commandblocklogic.updateCommand();
                        this.playerEntity.addChatMessage(new ChatComponentTranslation("advMode.setCommand.success", s1));
                    }
                } catch (Exception exception) {
                    LOGGER.error("Couldn't set command block", exception);
                } finally {
                    buffer.release();
                }
            } else {
                this.playerEntity.addChatMessage(new ChatComponentTranslation("advMode.notAllowed"));
            }
        } else if ("MC|Beacon".equals(packet.getChannel())) {
            if (this.playerEntity.openContainer instanceof ContainerBeacon container) {
                try {
                    PacketBuffer buffer = packet.getData();
                    int k = buffer.readInt();
                    int l = buffer.readInt();
                    Slot slot = container.getSlot(0);

                    if (slot.getHasStack()) {
                        slot.decrStackSize(1);
                        IInventory inventory = container.func_180611_e();
                        inventory.setField(1, k);
                        inventory.setField(2, l);
                        inventory.markDirty();
                    }
                } catch (Exception exception) {
                    LOGGER.error("Couldn't set beacon", exception);
                }
            }
        } else if ("MC|ItemName".equals(packet.getChannel()) && this.playerEntity.openContainer instanceof ContainerRepair container) {
            if (packet.getData() != null && packet.getData().readableBytes() >= 1) {
                String allowedCharacters = ChatAllowedCharacters.filterAllowedCharacters(packet.getData().readStringFromBuffer(32767));

                if (allowedCharacters.length() <= 30) {
                    container.updateItemName(allowedCharacters);
                }
            } else {
                container.updateItemName("");
            }
        }
    }
}
