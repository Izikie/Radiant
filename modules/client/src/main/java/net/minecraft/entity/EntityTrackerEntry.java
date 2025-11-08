package net.minecraft.entity;

import net.minecraft.block.Block;
import net.minecraft.entity.ai.attributes.IAttributeInstance;
import net.minecraft.entity.ai.attributes.ServersideAttributeMap;
import net.minecraft.entity.item.*;
import net.minecraft.entity.passive.IAnimals;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.projectile.*;
import net.minecraft.init.Items;
import net.minecraft.item.ItemMap;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.packet.api.Packet;
import net.minecraft.network.packet.impl.play.server.*;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.storage.MapData;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EntityTrackerEntry {
    private static final Logger LOGGER = LoggerFactory.getLogger(EntityTrackerEntry.class);
    public final Entity trackedEntity;
    public final int trackingDistanceThreshold;
    public final int updateFrequency;
    public int encodedPosX;
    public int encodedPosY;
    public int encodedPosZ;
    public int encodedRotationYaw;
    public int encodedRotationPitch;
    public int lastHeadMotion;
    public double lastTrackedEntityMotionX;
    public double lastTrackedEntityMotionY;
    public double motionZ;
    public int updateCounter;
    private double lastTrackedEntityPosX;
    private double lastTrackedEntityPosY;
    private double lastTrackedEntityPosZ;
    private boolean firstUpdateDone;
    private final boolean sendVelocityUpdates;
    private int ticksSinceLastForcedTeleport;
    private Entity field_85178_v;
    private boolean ridingEntity;
    private boolean onGround;
    public boolean playerEntitiesUpdated;
    public final Set<EntityPlayerMP> trackingPlayers = new HashSet<>();

    public EntityTrackerEntry(Entity trackedEntityIn, int trackingDistanceThresholdIn, int updateFrequencyIn, boolean sendVelocityUpdatesIn) {
        this.trackedEntity = trackedEntityIn;
        this.trackingDistanceThreshold = trackingDistanceThresholdIn;
        this.updateFrequency = updateFrequencyIn;
        this.sendVelocityUpdates = sendVelocityUpdatesIn;
        this.encodedPosX = MathHelper.floor(trackedEntityIn.posX * 32.0D);
        this.encodedPosY = MathHelper.floor(trackedEntityIn.posY * 32.0D);
        this.encodedPosZ = MathHelper.floor(trackedEntityIn.posZ * 32.0D);
        this.encodedRotationYaw = MathHelper.floor(trackedEntityIn.rotationYaw * 256.0F / 360.0F);
        this.encodedRotationPitch = MathHelper.floor(trackedEntityIn.rotationPitch * 256.0F / 360.0F);
        this.lastHeadMotion = MathHelper.floor(trackedEntityIn.getRotationYawHead() * 256.0F / 360.0F);
        this.onGround = trackedEntityIn.onGround;
    }

    public boolean equals(Object p_equals_1_) {
        return p_equals_1_ instanceof EntityTrackerEntry entityTrackerEntry && entityTrackerEntry.trackedEntity.getEntityId() == this.trackedEntity.getEntityId();
    }

    public int hashCode() {
        return this.trackedEntity.getEntityId();
    }

    public void updatePlayerList(List<EntityPlayer> players) {
        this.playerEntitiesUpdated = false;

        if (!this.firstUpdateDone || this.trackedEntity.getDistanceSq(this.lastTrackedEntityPosX, this.lastTrackedEntityPosY, this.lastTrackedEntityPosZ) > 16.0D) {
            this.lastTrackedEntityPosX = this.trackedEntity.posX;
            this.lastTrackedEntityPosY = this.trackedEntity.posY;
            this.lastTrackedEntityPosZ = this.trackedEntity.posZ;
            this.firstUpdateDone = true;
            this.playerEntitiesUpdated = true;
            this.updatePlayerEntities(players);
        }

        if (this.field_85178_v != this.trackedEntity.ridingEntity || this.trackedEntity.ridingEntity != null && this.updateCounter % 60 == 0) {
            this.field_85178_v = this.trackedEntity.ridingEntity;
            this.sendPacketToTrackedPlayers(new S1BPacketEntityAttach(0, this.trackedEntity, this.trackedEntity.ridingEntity));
        }

        if (this.trackedEntity instanceof EntityItemFrame entityitemframe && this.updateCounter % 10 == 0) {
            ItemStack itemstack = entityitemframe.getDisplayedItem();

            if (itemstack != null && itemstack.getItem() instanceof ItemMap) {
                MapData mapdata = Items.FILLED_MAP.getMapData(itemstack, this.trackedEntity.worldObj);

                for (EntityPlayer entityplayer : players) {
                    EntityPlayerMP entityplayermp = (EntityPlayerMP) entityplayer;
                    mapdata.updateVisiblePlayers(entityplayermp, itemstack);
                    Packet<?> packet = Items.FILLED_MAP.createMapDataPacket(itemstack, this.trackedEntity.worldObj, entityplayermp);

                    if (packet != null) {
                        entityplayermp.playerNetServerHandler.sendPacket(packet);
                    }
                }
            }

            this.sendMetadataToAllAssociatedPlayers();
        }

        if (this.updateCounter % this.updateFrequency == 0 || this.trackedEntity.isAirBorne || this.trackedEntity.getDataWatcher().hasObjectChanged()) {
            if (this.trackedEntity.ridingEntity == null) {
                ++this.ticksSinceLastForcedTeleport;
                int k = MathHelper.floor(this.trackedEntity.posX * 32.0D);
                int j1 = MathHelper.floor(this.trackedEntity.posY * 32.0D);
                int k1 = MathHelper.floor(this.trackedEntity.posZ * 32.0D);
                int l1 = MathHelper.floor(this.trackedEntity.rotationYaw * 256.0F / 360.0F);
                int i2 = MathHelper.floor(this.trackedEntity.rotationPitch * 256.0F / 360.0F);
                int j2 = k - this.encodedPosX;
                int k2 = j1 - this.encodedPosY;
                int i = k1 - this.encodedPosZ;
                Packet<?> packet = null;
                boolean flag = Math.abs(j2) >= 4 || Math.abs(k2) >= 4 || Math.abs(i) >= 4 || this.updateCounter % 60 == 0;
                boolean flag1 = Math.abs(l1 - this.encodedRotationYaw) >= 4 || Math.abs(i2 - this.encodedRotationPitch) >= 4;

                if (this.updateCounter > 0 || this.trackedEntity instanceof EntityArrow) {
                    if (j2 >= -128 && j2 < 128 && k2 >= -128 && k2 < 128 && i >= -128 && i < 128 && this.ticksSinceLastForcedTeleport <= 400 && !this.ridingEntity && this.onGround == this.trackedEntity.onGround) {
                        if ((!flag || !flag1) && !(this.trackedEntity instanceof EntityArrow)) {
                            if (flag) {
                                packet = new S14PacketEntity.S15PacketEntityRelMove(this.trackedEntity.getEntityId(), (byte) j2, (byte) k2, (byte) i, this.trackedEntity.onGround);
                            } else if (flag1) {
                                packet = new S14PacketEntity.S16PacketEntityLook(this.trackedEntity.getEntityId(), (byte) l1, (byte) i2, this.trackedEntity.onGround);
                            }
                        } else {
                            packet = new S14PacketEntity.S17PacketEntityLookMove(this.trackedEntity.getEntityId(), (byte) j2, (byte) k2, (byte) i, (byte) l1, (byte) i2, this.trackedEntity.onGround);
                        }
                    } else {
                        this.onGround = this.trackedEntity.onGround;
                        this.ticksSinceLastForcedTeleport = 0;
                        packet = new S18PacketEntityTeleport(this.trackedEntity.getEntityId(), k, j1, k1, (byte) l1, (byte) i2, this.trackedEntity.onGround);
                    }
                }

                if (this.sendVelocityUpdates) {
                    double d0 = this.trackedEntity.motionX - this.lastTrackedEntityMotionX;
                    double d1 = this.trackedEntity.motionY - this.lastTrackedEntityMotionY;
                    double d2 = this.trackedEntity.motionZ - this.motionZ;
                    double d3 = 0.02D;
                    double d4 = d0 * d0 + d1 * d1 + d2 * d2;

                    if (d4 > d3 * d3 || d4 > 0.0D && this.trackedEntity.motionX == 0.0D && this.trackedEntity.motionY == 0.0D && this.trackedEntity.motionZ == 0.0D) {
                        this.lastTrackedEntityMotionX = this.trackedEntity.motionX;
                        this.lastTrackedEntityMotionY = this.trackedEntity.motionY;
                        this.motionZ = this.trackedEntity.motionZ;
                        this.sendPacketToTrackedPlayers(new S12PacketEntityVelocity(this.trackedEntity.getEntityId(), this.lastTrackedEntityMotionX, this.lastTrackedEntityMotionY, this.motionZ));
                    }
                }

                if (packet != null) {
                    this.sendPacketToTrackedPlayers(packet);
                }

                this.sendMetadataToAllAssociatedPlayers();

                if (flag) {
                    this.encodedPosX = k;
                    this.encodedPosY = j1;
                    this.encodedPosZ = k1;
                }

                if (flag1) {
                    this.encodedRotationYaw = l1;
                    this.encodedRotationPitch = i2;
                }

                this.ridingEntity = false;
            } else {
                int j = MathHelper.floor(this.trackedEntity.rotationYaw * 256.0F / 360.0F);
                int i1 = MathHelper.floor(this.trackedEntity.rotationPitch * 256.0F / 360.0F);
                boolean flag2 = Math.abs(j - this.encodedRotationYaw) >= 4 || Math.abs(i1 - this.encodedRotationPitch) >= 4;

                if (flag2) {
                    this.sendPacketToTrackedPlayers(new S14PacketEntity.S16PacketEntityLook(this.trackedEntity.getEntityId(), (byte) j, (byte) i1, this.trackedEntity.onGround));
                    this.encodedRotationYaw = j;
                    this.encodedRotationPitch = i1;
                }

                this.encodedPosX = MathHelper.floor(this.trackedEntity.posX * 32.0D);
                this.encodedPosY = MathHelper.floor(this.trackedEntity.posY * 32.0D);
                this.encodedPosZ = MathHelper.floor(this.trackedEntity.posZ * 32.0D);
                this.sendMetadataToAllAssociatedPlayers();
                this.ridingEntity = true;
            }

            int l = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0F / 360.0F);

            if (Math.abs(l - this.lastHeadMotion) >= 4) {
                this.sendPacketToTrackedPlayers(new S19PacketEntityHeadLook(this.trackedEntity, (byte) l));
                this.lastHeadMotion = l;
            }

            this.trackedEntity.isAirBorne = false;
        }

        ++this.updateCounter;

        if (this.trackedEntity.velocityChanged) {
            this.func_151261_b(new S12PacketEntityVelocity(this.trackedEntity));
            this.trackedEntity.velocityChanged = false;
        }
    }

    private void sendMetadataToAllAssociatedPlayers() {
        DataWatcher datawatcher = this.trackedEntity.getDataWatcher();

        if (datawatcher.hasObjectChanged()) {
            this.func_151261_b(new S1CPacketEntityMetadata(this.trackedEntity.getEntityId(), datawatcher, false));
        }

        if (this.trackedEntity instanceof EntityLivingBase) {
            ServersideAttributeMap serversideattributemap = (ServersideAttributeMap) ((EntityLivingBase) this.trackedEntity).getAttributeMap();
            Set<IAttributeInstance> set = serversideattributemap.getAttributeInstanceSet();

            if (!set.isEmpty()) {
                this.func_151261_b(new S20PacketEntityProperties(this.trackedEntity.getEntityId(), set));
            }

            set.clear();
        }
    }

    public void sendPacketToTrackedPlayers(Packet<?> packet) {
        for (EntityPlayerMP entityplayermp : this.trackingPlayers) {
            entityplayermp.playerNetServerHandler.sendPacket(packet);
        }
    }

    public void func_151261_b(Packet<?> packet) {
        this.sendPacketToTrackedPlayers(packet);

        if (this.trackedEntity instanceof EntityPlayerMP entityPlayerMP) {
            entityPlayerMP.playerNetServerHandler.sendPacket(packet);
        }
    }

    public void sendDestroyEntityPacketToTrackedPlayers() {
        for (EntityPlayerMP entityplayermp : this.trackingPlayers) {
            entityplayermp.removeEntity(this.trackedEntity);
        }
    }

    public void removeFromTrackedPlayers(EntityPlayerMP playerMP) {
        if (this.trackingPlayers.contains(playerMP)) {
            playerMP.removeEntity(this.trackedEntity);
            this.trackingPlayers.remove(playerMP);
        }
    }

    public void updatePlayerEntity(EntityPlayerMP playerMP) {
        if (playerMP != this.trackedEntity) {
            if (this.func_180233_c(playerMP)) {
                if (!this.trackingPlayers.contains(playerMP) && (this.isPlayerWatchingThisChunk(playerMP) || this.trackedEntity.forceSpawn)) {
                    this.trackingPlayers.add(playerMP);
                    Packet<?> packet = this.createSpawnPacket();
                    playerMP.playerNetServerHandler.sendPacket(packet);

                    if (!this.trackedEntity.getDataWatcher().getIsBlank()) {
                        playerMP.playerNetServerHandler.sendPacket(new S1CPacketEntityMetadata(this.trackedEntity.getEntityId(), this.trackedEntity.getDataWatcher(), true));
                    }

                    NBTTagCompound nbttagcompound = this.trackedEntity.getNBTTagCompound();

                    if (nbttagcompound != null) {
                        playerMP.playerNetServerHandler.sendPacket(new S49PacketUpdateEntityNBT(this.trackedEntity.getEntityId(), nbttagcompound));
                    }

                    if (this.trackedEntity instanceof EntityLivingBase) {
                        ServersideAttributeMap serversideattributemap = (ServersideAttributeMap) ((EntityLivingBase) this.trackedEntity).getAttributeMap();
                        Collection<IAttributeInstance> collection = serversideattributemap.getWatchedAttributes();

                        if (!collection.isEmpty()) {
                            playerMP.playerNetServerHandler.sendPacket(new S20PacketEntityProperties(this.trackedEntity.getEntityId(), collection));
                        }
                    }

                    this.lastTrackedEntityMotionX = this.trackedEntity.motionX;
                    this.lastTrackedEntityMotionY = this.trackedEntity.motionY;
                    this.motionZ = this.trackedEntity.motionZ;

                    if (this.sendVelocityUpdates && !(packet instanceof S0FPacketSpawnMob)) {
                        playerMP.playerNetServerHandler.sendPacket(new S12PacketEntityVelocity(this.trackedEntity.getEntityId(), this.trackedEntity.motionX, this.trackedEntity.motionY, this.trackedEntity.motionZ));
                    }

                    if (this.trackedEntity.ridingEntity != null) {
                        playerMP.playerNetServerHandler.sendPacket(new S1BPacketEntityAttach(0, this.trackedEntity, this.trackedEntity.ridingEntity));
                    }

                    if (this.trackedEntity instanceof EntityLiving entityLiving && entityLiving.getLeashedToEntity() != null) {
                        playerMP.playerNetServerHandler.sendPacket(new S1BPacketEntityAttach(1, this.trackedEntity, entityLiving.getLeashedToEntity()));
                    }

                    if (this.trackedEntity instanceof EntityLivingBase) {
                        for (int i = 0; i < 5; ++i) {
                            ItemStack itemstack = ((EntityLivingBase) this.trackedEntity).getEquipmentInSlot(i);

                            if (itemstack != null) {
                                playerMP.playerNetServerHandler.sendPacket(new S04PacketEntityEquipment(this.trackedEntity.getEntityId(), i, itemstack));
                            }
                        }
                    }

                    if (this.trackedEntity instanceof EntityPlayer entityplayer) {

                        if (entityplayer.isPlayerSleeping()) {
                            playerMP.playerNetServerHandler.sendPacket(new S0APacketUseBed(entityplayer, new BlockPos(this.trackedEntity)));
                        }
                    }

                    if (this.trackedEntity instanceof EntityLivingBase entitylivingbase) {

                        for (PotionEffect potioneffect : entitylivingbase.getActivePotionEffects()) {
                            playerMP.playerNetServerHandler.sendPacket(new S1DPacketEntityEffect(this.trackedEntity.getEntityId(), potioneffect));
                        }
                    }
                }
            } else if (this.trackingPlayers.contains(playerMP)) {
                this.trackingPlayers.remove(playerMP);
                playerMP.removeEntity(this.trackedEntity);
            }
        }
    }

    public boolean func_180233_c(EntityPlayerMP playerMP) {
        double d0 = playerMP.posX - (this.encodedPosX / 32);
        double d1 = playerMP.posZ - (this.encodedPosZ / 32);
        return d0 >= (-this.trackingDistanceThreshold) && d0 <= this.trackingDistanceThreshold && d1 >= (-this.trackingDistanceThreshold) && d1 <= this.trackingDistanceThreshold && this.trackedEntity.isSpectatedByPlayer(playerMP);
    }

    private boolean isPlayerWatchingThisChunk(EntityPlayerMP playerMP) {
        return playerMP.getServerForPlayer().getPlayerManager().isPlayerWatchingChunk(playerMP, this.trackedEntity.chunkCoordX, this.trackedEntity.chunkCoordZ);
    }

    public void updatePlayerEntities(List<EntityPlayer> players) {
        for (EntityPlayer player : players) {
            this.updatePlayerEntity((EntityPlayerMP) player);
        }
    }

    private Packet<?> createSpawnPacket() {
        if (this.trackedEntity.isDead) {
            LOGGER.warn("Fetching addPacket for removed entity");
        }

        switch (this.trackedEntity) {
            case EntityItem entityItem -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 2, 1);
            }
            case EntityPlayerMP entityPlayerMP -> {
                return new S0CPacketSpawnPlayer((EntityPlayer) this.trackedEntity);
            }
            case EntityMinecart entityminecart -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 10, entityminecart.getMinecartType().getNetworkID());
            }
            case EntityBoat entityBoat -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 1);
            }
            case IAnimals iAnimals -> {
                this.lastHeadMotion = MathHelper.floor(this.trackedEntity.getRotationYawHead() * 256.0F / 360.0F);
                return new S0FPacketSpawnMob((EntityLivingBase) this.trackedEntity);
            }
            case EntityFishHook entityFishHook -> {
                Entity entity1 = entityFishHook.angler;
                return new S0EPacketSpawnObject(this.trackedEntity, 90, entity1 != null ? entity1.getEntityId() : this.trackedEntity.getEntityId());
            }
            case EntityArrow entityArrow -> {
                Entity entity = entityArrow.shootingEntity;
                return new S0EPacketSpawnObject(this.trackedEntity, 60, entity != null ? entity.getEntityId() : this.trackedEntity.getEntityId());
            }
            case EntitySnowball entitySnowball -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 61);
            }
            case EntityPotion entityPotion -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 73, entityPotion.getPotionDamage());
            }
            case EntityExpBottle entityExpBottle -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 75);
            }
            case EntityEnderPearl entityEnderPearl -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 65);
            }
            case EntityEnderEye entityEnderEye -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 72);
            }
            case EntityFireworkRocket entityFireworkRocket -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 76);
            }
            case EntityFireball entityfireball -> {
                S0EPacketSpawnObject s0epacketspawnobject2;
                int i = 63;

                if (this.trackedEntity instanceof EntitySmallFireball) {
                    i = 64;
                } else if (this.trackedEntity instanceof EntityWitherSkull) {
                    i = 66;
                }

                if (entityfireball.shootingEntity != null) {
                    s0epacketspawnobject2 = new S0EPacketSpawnObject(this.trackedEntity, i, ((EntityFireball) this.trackedEntity).shootingEntity.getEntityId());
                } else {
                    s0epacketspawnobject2 = new S0EPacketSpawnObject(this.trackedEntity, i, 0);
                }

                s0epacketspawnobject2.setSpeedX((int) (entityfireball.accelerationX * 8000.0D));
                s0epacketspawnobject2.setSpeedY((int) (entityfireball.accelerationY * 8000.0D));
                s0epacketspawnobject2.setSpeedZ((int) (entityfireball.accelerationZ * 8000.0D));
                return s0epacketspawnobject2;
            }
            case EntityEgg entityEgg -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 62);
            }
            case EntityTNTPrimed entityTNTPrimed -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 50);
            }
            case EntityEnderCrystal entityEnderCrystal -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 51);
            }
            case EntityFallingBlock entityfallingblock -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 70, Block.getStateId(entityfallingblock.getBlock()));
            }
            case EntityArmorStand entityArmorStand -> {
                return new S0EPacketSpawnObject(this.trackedEntity, 78);
            }
            case EntityPainting entityPainting -> {
                return new S10PacketSpawnPainting(entityPainting);
            }
            case EntityItemFrame entityitemframe -> {
                S0EPacketSpawnObject s0epacketspawnobject1 = new S0EPacketSpawnObject(this.trackedEntity, 71, entityitemframe.facingDirection.getHorizontalIndex());
                BlockPos blockpos1 = entityitemframe.getHangingPosition();
                s0epacketspawnobject1.setX(MathHelper.floor((blockpos1.getX() * 32)));
                s0epacketspawnobject1.setY(MathHelper.floor((blockpos1.getY() * 32)));
                s0epacketspawnobject1.setZ(MathHelper.floor((blockpos1.getZ() * 32)));
                return s0epacketspawnobject1;
            }
            case EntityLeashKnot entityleashknot -> {
                S0EPacketSpawnObject s0epacketspawnobject = new S0EPacketSpawnObject(this.trackedEntity, 77);
                BlockPos blockpos = entityleashknot.getHangingPosition();
                s0epacketspawnobject.setX(MathHelper.floor((blockpos.getX() * 32)));
                s0epacketspawnobject.setY(MathHelper.floor((blockpos.getY() * 32)));
                s0epacketspawnobject.setZ(MathHelper.floor((blockpos.getZ() * 32)));
                return s0epacketspawnobject;
            }
            case EntityXPOrb entityXPOrb -> {
                return new S11PacketSpawnExperienceOrb(entityXPOrb);
            }
            default ->
                    throw new IllegalArgumentException("Don't know how to add " + this.trackedEntity.getClass() + "!");
        }
    }

    public void removeTrackedPlayerSymmetric(EntityPlayerMP playerMP) {
        if (this.trackingPlayers.contains(playerMP)) {
            this.trackingPlayers.remove(playerMP);
            playerMP.removeEntity(this.trackedEntity);
        }
    }
}
