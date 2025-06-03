package net.minecraft.server.integrated;

import com.google.common.util.concurrent.Futures;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ThreadLanServerPing;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.crash.CrashReport;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.play.server.S2BPacketChangeGameState;
import net.minecraft.server.MinecraftServer;
import net.minecraft.src.Config;
import net.minecraft.util.BlockPos;
import net.minecraft.util.CryptManager;
import net.minecraft.util.HttpUtil;
import net.minecraft.util.Util;
import net.minecraft.world.*;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.optifine.ClearWater;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class IntegratedServer extends MinecraftServer {
    private static final Logger LOGGER = LogManager.getLogger();
    private final Minecraft mc;
    private final WorldSettings theWorldSettings;
    private boolean isGamePaused;
    private boolean isPublic;
    private ThreadLanServerPing lanServerPing;
    private long ticksSaveLast = 0L;
    public World difficultyUpdateWorld = null;
    public BlockPos difficultyUpdatePos = null;
    public DifficultyInstance difficultyLast = null;

    public IntegratedServer(Minecraft mcIn) {
        super(mcIn.getProxy(), new File(mcIn.mcDataDir, USER_CACHE_FILE.getName()));
        this.mc = mcIn;
        this.theWorldSettings = null;
    }

    public IntegratedServer(Minecraft mcIn, String folderName, String worldName, WorldSettings settings) {
        super(new File(mcIn.mcDataDir, "saves"), mcIn.getProxy(), new File(mcIn.mcDataDir, USER_CACHE_FILE.getName()));
        this.setServerOwner(mcIn.getSession().getUsername());
        this.setFolderName(folderName);
        this.setWorldName(worldName);
        this.canCreateBonusChest(settings.isBonusChestEnabled());
        this.setBuildLimit(256);
        this.setConfigManager(new IntegratedPlayerList(this));
        this.mc = mcIn;
        this.theWorldSettings = settings;
        ISaveHandler isavehandler = this.getActiveAnvilConverter().getSaveLoader(folderName, false);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();

        if (worldinfo != null) {
            NBTTagCompound nbttagcompound = worldinfo.getPlayerNBTTagCompound();

            if (nbttagcompound != null && nbttagcompound.hasKey("Dimension")) {
                PacketThreadUtil.lastDimensionId = nbttagcompound.getInteger("Dimension");
                this.mc.loadingScreen.setLoadingProgress(-1);
            }
        }
    }

    protected ServerCommandManager createNewCommandManager() {
        return new IntegratedServerCommandManager();
    }

    protected void loadAllWorlds(String saveName, String worldNameIn, long seed, WorldType type, String worldNameIn2) {
        this.convertMapIfNeeded(saveName);

        this.worldServers = new WorldServer[3];
        this.timeOfLastDimensionTick = new long[this.worldServers.length][100];

        ISaveHandler isavehandler = this.getActiveAnvilConverter().getSaveLoader(saveName, true);
        this.setResourcePackFromWorld(this.getFolderName(), isavehandler);
        WorldInfo worldinfo = isavehandler.loadWorldInfo();

        if (worldinfo == null) {
            worldinfo = new WorldInfo(this.theWorldSettings, worldNameIn);
        } else {
            worldinfo.setWorldName(worldNameIn);
        }

        for (int l = 0; l < this.worldServers.length; ++l) {
            int i1 = 0;

            if (l == 1) {
                i1 = -1;
            }

            if (l == 2) {
                i1 = 1;
            }

            if (l == 0) {
                this.worldServers[l] = (WorldServer) (new WorldServer(this, isavehandler, worldinfo, i1)).init();
                this.worldServers[l].initialize(this.theWorldSettings);
            } else {
                this.worldServers[l] = (WorldServer) (new WorldServerMulti(this, isavehandler, i1, this.worldServers[0])).init();
            }

            this.worldServers[l].addWorldAccess(new WorldManager(this, this.worldServers[l]));
        }

        this.getConfigurationManager().setPlayerManager(this.worldServers);

        if (this.worldServers[0].getWorldInfo().getDifficulty() == null) {
            this.setDifficultyForAllWorlds(this.mc.gameSettings.difficulty);
        }

        this.initialWorldChunkLoad();
    }

    protected boolean startServer() throws IOException {
        LOGGER.info("Starting integrated minecraft server version 1.9");
        this.setOnlineMode(true);
        this.setCanSpawnAnimals(true);
        this.setCanSpawnNPCs(true);
        this.setAllowPvp(true);
        this.setAllowFlight(true);
        LOGGER.info("Generating keypair");
        this.setKeyPair(CryptManager.generateKeyPair());

        this.loadAllWorlds(this.getFolderName(), this.getWorldName(), this.theWorldSettings.getSeed(), this.theWorldSettings.getTerrainType(), this.theWorldSettings.getWorldName());
        this.setMOTD(this.getServerOwner() + " - " + this.worldServers[0].getWorldInfo().getWorldName());

        return true;
    }

    public void tick() {
        this.onTick();
        boolean flag = this.isGamePaused;
        this.isGamePaused = Minecraft.getMinecraft().getNetHandler() != null && Minecraft.getMinecraft().isGamePaused();

        if (!flag && this.isGamePaused) {
            LOGGER.info("Saving and pausing game...");
            this.getConfigurationManager().saveAllPlayerData();
            this.saveAllWorlds(false);
        }

        if (this.isGamePaused) {
            synchronized (this.futureTaskQueue) {
                while (!this.futureTaskQueue.isEmpty()) {
                    Util.runTask(futureTaskQueue.poll(), LOGGER);
                }
            }
        } else {
            super.tick();

            if (this.mc.gameSettings.renderDistanceChunks != this.getConfigurationManager().getViewDistance()) {
                LOGGER.info("Changing view distance to {}, from {}", new Object[]{this.mc.gameSettings.renderDistanceChunks, this.getConfigurationManager().getViewDistance()});
                this.getConfigurationManager().setViewDistance(this.mc.gameSettings.renderDistanceChunks);
            }

            if (this.mc.world != null) {
                WorldInfo worldinfo1 = this.worldServers[0].getWorldInfo();
                WorldInfo worldinfo = this.mc.world.getWorldInfo();

                if (!worldinfo1.isDifficultyLocked() && worldinfo.getDifficulty() != worldinfo1.getDifficulty()) {
                    LOGGER.info("Changing difficulty to {}, from {}", new Object[]{worldinfo.getDifficulty(), worldinfo1.getDifficulty()});
                    this.setDifficultyForAllWorlds(worldinfo.getDifficulty());
                } else if (worldinfo.isDifficultyLocked() && !worldinfo1.isDifficultyLocked()) {
                    LOGGER.info("Locking difficulty to {}", new Object[]{worldinfo.getDifficulty()});

                    for (WorldServer worldserver : this.worldServers) {
                        if (worldserver != null) {
                            worldserver.getWorldInfo().setDifficultyLocked(true);
                        }
                    }
                }
            }
        }
    }

    public boolean canStructuresSpawn() {
        return false;
    }

    public WorldSettings.GameType getGameType() {
        return this.theWorldSettings.getGameType();
    }

    public Difficulty getDifficulty() {
        return this.mc.world == null ? this.mc.gameSettings.difficulty : this.mc.world.getWorldInfo().getDifficulty();
    }

    public boolean isHardcore() {
        return this.theWorldSettings.getHardcoreEnabled();
    }

    public boolean shouldBroadcastRconToOps() {
        return true;
    }

    public boolean shouldBroadcastConsoleToOps() {
        return true;
    }

    public void saveAllWorlds(boolean dontLog) {
        if (dontLog) {
            int i = this.getTickCounter();
            int j = this.mc.gameSettings.ofAutoSaveTicks;

            if (i < this.ticksSaveLast + j) {
                return;
            }

            this.ticksSaveLast = i;
        }

        super.saveAllWorlds(dontLog);
    }

    public File getDataDirectory() {
        return this.mc.mcDataDir;
    }

    public boolean isDedicatedServer() {
        return false;
    }

    protected void finalTick(CrashReport report) {
        this.mc.crashed(report);
    }

    public CrashReport addServerInfoToCrashReport(CrashReport report) {
        report = super.addServerInfoToCrashReport(report);
        report.getCategory().addCrashSectionCallable("Type", () -> "Integrated Server (map_client.txt)");
        report.getCategory().addCrashSectionCallable("Is Modded", () -> {
            String s = ClientBrandRetriever.getClientModName();

            if (!s.equals("vanilla")) {
                return "Definitely; Client brand changed to '" + s + "'";
            } else {
                s = IntegratedServer.this.getServerModName();
                return !s.equals("vanilla") ? "Definitely; Server brand changed to '" + s + "'" : (Minecraft.class.getSigners() == null ? "Very likely; Jar signature invalidated" : "Probably not. Jar signature remains and both client + server brands are untouched.");
            }
        });
        return report;
    }

    public void setDifficultyForAllWorlds(Difficulty difficulty) {
        super.setDifficultyForAllWorlds(difficulty);

        if (this.mc.world != null) {
            this.mc.world.getWorldInfo().setDifficulty(difficulty);
        }
    }

    public String shareToLAN(WorldSettings.GameType type, boolean allowCheats) {
        try {
            int i = -1;

            try {
                i = HttpUtil.getSuitableLanPort();
            } catch (IOException exception) {
            }

            if (i <= 0) {
                i = 25564;
            }

            this.getNetworkSystem().addLanEndpoint(null, i);
            LOGGER.info("Started on {}", i);
            this.isPublic = true;
            this.lanServerPing = new ThreadLanServerPing(this.getMOTD(), i + "");
            this.lanServerPing.start();
            this.getConfigurationManager().setGameType(type);
            this.getConfigurationManager().setCommandsAllowedForAll(allowCheats);
            return i + "";
        } catch (IOException exception) {
            return null;
        }
    }

    public void stopServer() {
        super.stopServer();

        if (this.lanServerPing != null) {
            this.lanServerPing.interrupt();
            this.lanServerPing = null;
        }
    }

    public void initiateShutdown() {
        if (this.isServerRunning()) {
            Futures.getUnchecked(this.addScheduledTask(() -> {
                for (EntityPlayerMP entityplayermp : new ArrayList<>(IntegratedServer.this.getConfigurationManager().getPlayerList())) {
                    IntegratedServer.this.getConfigurationManager().playerLoggedOut(entityplayermp);
                }
            }));
        }

        super.initiateShutdown();

        if (this.lanServerPing != null) {
            this.lanServerPing.interrupt();
            this.lanServerPing = null;
        }
    }

    public void setStaticInstance() {
        this.setInstance();
    }

    public boolean getPublic() {
        return this.isPublic;
    }

    public void setGameType(WorldSettings.GameType gameMode) {
        this.getConfigurationManager().setGameType(gameMode);
    }

    public boolean isCommandBlockEnabled() {
        return true;
    }

    public int getOpPermissionLevel() {
        return 4;
    }

    private void onTick() {
        for (WorldServer worldserver : this.worldServers) {
            this.onTick(worldserver);
        }
    }

    public DifficultyInstance getDifficultyAsync(World p_getDifficultyAsync_1_, BlockPos p_getDifficultyAsync_2_) {
        this.difficultyUpdateWorld = p_getDifficultyAsync_1_;
        this.difficultyUpdatePos = p_getDifficultyAsync_2_;
        return this.difficultyLast;
    }

    private void onTick(WorldServer p_onTick_1_) {
        if (!Config.isTimeDefault()) {
            this.fixWorldTime(p_onTick_1_);
        }

        if (!Config.isWeatherEnabled()) {
            this.fixWorldWeather(p_onTick_1_);
        }

        if (Config.waterOpacityChanged) {
            Config.waterOpacityChanged = false;
            ClearWater.updateWaterOpacity(Config.getGameSettings(), p_onTick_1_);
        }

        if (this.difficultyUpdateWorld == p_onTick_1_ && this.difficultyUpdatePos != null) {
            this.difficultyLast = p_onTick_1_.getDifficultyForLocation(this.difficultyUpdatePos);
            this.difficultyUpdateWorld = null;
            this.difficultyUpdatePos = null;
        }
    }

    private void fixWorldWeather(WorldServer p_fixWorldWeather_1_) {
        WorldInfo worldinfo = p_fixWorldWeather_1_.getWorldInfo();

        if (worldinfo.isRaining() || worldinfo.isThundering()) {
            worldinfo.setRainTime(0);
            worldinfo.setRaining(false);
            p_fixWorldWeather_1_.setRainStrength(0.0F);
            worldinfo.setThunderTime(0);
            worldinfo.setThundering(false);
            p_fixWorldWeather_1_.setThunderStrength(0.0F);
            this.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(2, 0.0F));
            this.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(7, 0.0F));
            this.getConfigurationManager().sendPacketToAllPlayers(new S2BPacketChangeGameState(8, 0.0F));
        }
    }

    private void fixWorldTime(WorldServer p_fixWorldTime_1_) {
        WorldInfo worldinfo = p_fixWorldTime_1_.getWorldInfo();

        if (worldinfo.getGameType().getID() == 1) {
            long i = p_fixWorldTime_1_.getWorldTime();
            long j = i % 24000L;

            if (Config.isTimeDayOnly()) {
                if (j <= 1000L) {
                    p_fixWorldTime_1_.setWorldTime(i - j + 1001L);
                }

                if (j >= 11000L) {
                    p_fixWorldTime_1_.setWorldTime(i - j + 24001L);
                }
            }

            if (Config.isTimeNightOnly()) {
                if (j <= 14000L) {
                    p_fixWorldTime_1_.setWorldTime(i - j + 14001L);
                }

                if (j >= 22000L) {
                    p_fixWorldTime_1_.setWorldTime(i - j + 24000L + 14001L);
                }
            }
        }
    }
}
