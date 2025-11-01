package net.minecraft.client;

import com.google.common.collect.Queues;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListenableFutureTask;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.MinecraftSessionService;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.audio.MusicTicker;
import net.minecraft.client.audio.SoundHandler;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.*;
import net.minecraft.client.gui.achievement.GuiAchievement;
import net.minecraft.client.gui.inventory.GuiInventory;
import net.minecraft.client.main.GameConfiguration;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.multiplayer.PlayerControllerMP;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerLoginClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.chunk.RenderChunk;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.*;
import net.minecraft.client.resources.data.*;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.client.shader.Framebuffer;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLeashKnot;
import net.minecraft.entity.EntityList;
import net.minecraft.entity.boss.BossStatus;
import net.minecraft.entity.item.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.init.Bootstrap;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.NetworkState;
import net.minecraft.network.packet.impl.handshake.client.C00Handshake;
import net.minecraft.network.packet.impl.login.client.C00PacketLoginStart;
import net.minecraft.network.packet.impl.play.client.C16PacketClientStatus;
import net.minecraft.server.integrated.IntegratedServer;
import net.minecraft.stats.AchievementList;
import net.minecraft.stats.StatFileWriter;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Timer;
import net.minecraft.util.*;
import net.minecraft.util.input.MouseHelper;
import net.minecraft.util.input.MovementInputFromOptions;
import net.minecraft.util.input.MovingObjectPosition;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Difficulty;
import net.minecraft.world.WorldProviderEnd;
import net.minecraft.world.WorldProviderHell;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.chunk.storage.AnvilSaveConverter;
import net.minecraft.world.storage.ISaveFormat;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.WorldInfo;
import net.optifine.Lang;
import net.radiant.lwjgl.LWJGLException;
import net.radiant.lwjgl.input.Keyboard;
import net.radiant.lwjgl.input.Mouse;
import net.radiant.lwjgl.opengl.Display;
import net.radiant.lwjgl.opengl.DisplayMode;
import net.radiant.lwjgl.opengl.OpenGLException;
import net.radiant.lwjgl.opengl.PixelFormat;
import net.radiant.lwjgl.util.glu.GLU;
import net.radiant.util.NativeImage;
import org.lwjgl.Version;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class Minecraft implements IThreadListener {

	static {
		if (!GLFW.glfwInit()) {
			throw new IllegalStateException("Unable to initialize glfw");
		}
	}

	private static final Logger LOGGER = LoggerFactory.getLogger(Minecraft.class);
	private static final ResourceLocation LOCATION_MOJANG_PNG = new ResourceLocation("textures/gui/title/mojang.png");
	public static final boolean IS_RUNNING_ON_MAC = Util.getOSType() == Util.OperatingSystem.MAC;
	private static final List<DisplayMode> MAC_DISPLAY_MODES = List.of(new DisplayMode(2560, 1600), new DisplayMode(2880, 1800));

	public static final Random RANDOM = new Random();
	public static byte[] memoryReserve = new byte[10485760];

	private final File fileResourcepacks;
	private final PropertyMap profileProperties;
	private ServerData currentServerData;
	private TextureManager renderEngine;
	private static Minecraft instance;
	public PlayerControllerMP playerController;
	private boolean fullscreen;
	private boolean hasCrashed;
	private CrashReport crashReporter;
	public int displayWidth;
	public int displayHeight;
	private final Timer timer = new Timer(20.0F);
	public WorldClient world;
	public RenderGlobal renderGlobal;
	private RenderManager renderManager;
	private RenderItem renderItem;
	private ItemRenderer itemRenderer;
	public EntityPlayerSP player;
	private Entity renderViewEntity;
	public Entity pointedEntity;
	public EffectRenderer effectRenderer;
	private Session session;
	private boolean isGamePaused;

	public FontRenderer fontRendererObj;
	public FontRenderer standardGalacticFontRenderer;

	public GuiScreen currentScreen;
	public LoadingScreenRenderer loadingScreen;
	public EntityRenderer entityRenderer;

	private int leftClickCounter;
	private final int tempDisplayWidth;
	private final int tempDisplayHeight;
	private IntegratedServer theIntegratedServer;
	public GuiAchievement guiAchievement;
	public GuiIngame ingameGUI;
	public boolean skipRenderWorld;
	public MovingObjectPosition objectMouseOver;
	public GameSettings gameSettings;
	public MouseHelper mouseHelper;
	public final File mcDataDir;
	private final File fileAssets;
	private final Proxy proxy;
	private ISaveFormat saveLoader;
	private static int debugFPS;
	private int rightClickDelayTimer;
	private String serverName;
	private int serverPort;
	public boolean inGameHasFocus;
	long systemTime = getSystemTime();
	private int joinPlayerCounter;
	public final FrameTimer frameTimer = new FrameTimer();
	long startNanoTime = System.nanoTime();
	private NetworkManager myNetworkManager;
	private boolean integratedServerIsRunning;
	private long debugCrashKeyPressTime = -1L;
	private IReloadableResourceManager mcResourceManager;
	private final IMetadataSerializer metadataSerializer_ = new IMetadataSerializer();
	private final List<IResourcePack> defaultResourcePacks = new ArrayList<>();
	private final DefaultResourcePack mcDefaultResourcePack;
	private ResourcePackRepository mcResourcePackRepository;
	private LanguageManager mcLanguageManager;
	private Framebuffer framebufferMc;
	private TextureMap textureMapBlocks;
	private SoundHandler mcSoundHandler;
	private MusicTicker mcMusicTicker;
	private ResourceLocation mojangLogo;
	private final MinecraftSessionService sessionService;
	private SkinManager skinManager;
	private final Queue<FutureTask<?>> scheduledTasks = Queues.newArrayDeque();
	private final Thread mcThread = Thread.currentThread();
	private BlockRendererDispatcher blockRenderDispatcher;
	volatile boolean running = true;
	public String debug = "";
	public final boolean renderChunksMany = true;
	long debugUpdateTime = getSystemTime();
	int fpsCounter;
	private final long startTime;

	public Minecraft(GameConfiguration gameConfig) {
		instance = this;

		startTime = System.currentTimeMillis();

		mcDataDir = gameConfig.folderInfo().mcDataDir();
		fileAssets = gameConfig.folderInfo().assetsDir();
		fileResourcepacks = gameConfig.folderInfo().resourcePacksDir();
		profileProperties = gameConfig.userInfo().profileProperties();
		mcDefaultResourcePack = new DefaultResourcePack((new ResourceIndex(gameConfig.folderInfo().assetsDir(), "1.8")).getResourceMap());
		proxy = gameConfig.userInfo().proxy() == null ? Proxy.NO_PROXY : gameConfig.userInfo().proxy();
		sessionService = (new YggdrasilAuthenticationService(gameConfig.userInfo().proxy(), UUID.randomUUID().toString())).createMinecraftSessionService();
		session = gameConfig.userInfo().session();
		LOGGER.info("Setting user: {}", session.getUsername());
		LOGGER.info("(Session ID is {})", session.getSessionID());
		displayWidth = Math.max(1, gameConfig.displayInfo().width());
		displayHeight = Math.max(1, gameConfig.displayInfo().height());
		tempDisplayWidth = gameConfig.displayInfo().width();
		tempDisplayHeight = gameConfig.displayInfo().height();
		fullscreen = gameConfig.displayInfo().fullscreen();

		theIntegratedServer = new IntegratedServer(this);

		if (gameConfig.serverInfo().address() != null) {
			serverName = gameConfig.serverInfo().address();
			serverPort = gameConfig.serverInfo().port();
		}

		Bootstrap.register();
	}

	public void run() {
		running = true;

		try {
			startGame();
		} catch (Throwable throwable) {
			throwable.printStackTrace();
			CrashReport report = CrashReport.makeCrashReport(throwable, "Initializing game");
			report.makeCategory("Initialization");
			displayCrashReport(addGraphicsAndWorldToCrashReport(report));
			return;
		}

		LOGGER.info("Game launched in {}ms", System.currentTimeMillis() - startTime);

		try {
			while (running) {
				if (!hasCrashed || crashReporter == null) {
					runGameLoop();
				} else {
					displayCrashReport(crashReporter);
				}
			}
		} catch (OutOfMemoryError error) {
			freeMemory();
			displayGuiScreen(new GuiMemoryErrorScreen());
			System.gc();
		} catch (MinecraftError _) {
		} catch (ReportedException exception) {
			addGraphicsAndWorldToCrashReport(exception.getCrashReport());
			freeMemory();
			LOGGER.error("Reported exception thrown!", exception);
			displayCrashReport(exception.getCrashReport());
		} catch (Throwable throwable) {
			CrashReport report = addGraphicsAndWorldToCrashReport(new CrashReport("Unexpected error", throwable));
			freeMemory();
			LOGGER.error("Unreported exception thrown!", throwable);
			displayCrashReport(report);
		} finally {
			shutdownMinecraftApplet();
		}
	}

	private void startGame() throws LWJGLException {
		gameSettings = new GameSettings(this, mcDataDir);
		defaultResourcePacks.add(mcDefaultResourcePack);

		if (gameSettings.overrideHeight > 0 && gameSettings.overrideWidth > 0) {
			displayWidth = gameSettings.overrideWidth;
			displayHeight = gameSettings.overrideHeight;
		}

		LOGGER.info("LWJGL Version: {}", Version.getVersion());

		setWindowIcon();
		setInitialDisplayMode();
		createDisplay();
		OpenGlHelper.initializeTextures();
		framebufferMc = new Framebuffer(displayWidth, displayHeight, true);
		framebufferMc.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
		registerMetadataSerializers();
		mcResourcePackRepository = new ResourcePackRepository(fileResourcepacks, new File(mcDataDir, "server-resource-packs"), mcDefaultResourcePack, metadataSerializer_, gameSettings);
		mcResourceManager = new SimpleReloadableResourceManager(metadataSerializer_);
		mcLanguageManager = new LanguageManager(metadataSerializer_, gameSettings.language);
		mcResourceManager.registerReloadListener(mcLanguageManager);
		refreshResources();
		renderEngine = new TextureManager(mcResourceManager);
		mcResourceManager.registerReloadListener(renderEngine);
		drawSplashScreen(renderEngine);
		skinManager = new SkinManager(renderEngine, new File(fileAssets, "skins"), sessionService);
		saveLoader = new AnvilSaveConverter(new File(mcDataDir, "saves"));
		mcSoundHandler = new SoundHandler(mcResourceManager, gameSettings);
		mcResourceManager.registerReloadListener(mcSoundHandler);
		mcMusicTicker = new MusicTicker(this);
		fontRendererObj = new FontRenderer(gameSettings, new ResourceLocation("textures/font/ascii.png"), renderEngine, false);

		if (gameSettings.language != null) {
			fontRendererObj.setUnicodeFlag(isUnicode());
			fontRendererObj.setBidiFlag(mcLanguageManager.isCurrentLanguageBidirectional());
		}

		standardGalacticFontRenderer = new FontRenderer(gameSettings, new ResourceLocation("textures/font/ascii_sga.png"), renderEngine, false);
		mcResourceManager.registerReloadListener(fontRendererObj);
		mcResourceManager.registerReloadListener(standardGalacticFontRenderer);
		mcResourceManager.registerReloadListener(new GrassColorReloadListener());
		mcResourceManager.registerReloadListener(new FoliageColorReloadListener());
		AchievementList.OPEN_INVENTORY.setStatStringFormatter(str -> {
			try {
				return String.format(str, GameSettings.getKeyDisplayString(gameSettings.keyBindInventory.getKeyCode()));
			} catch (Exception exception) {
				return "Error: " + exception.getLocalizedMessage();
			}
		});
		mouseHelper = new MouseHelper();
		checkGLError("Pre startup");
		GlStateManager.enableTexture2D();
		GlStateManager.shadeModel(7425);
		GlStateManager.clearDepth(1.0D);
		GlStateManager.enableDepth();
		GlStateManager.depthFunc(515);
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);
		GlStateManager.cullFace(1029);
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.matrixMode(5888);
		checkGLError("Startup");
		textureMapBlocks = new TextureMap("textures");
		textureMapBlocks.setMipmapLevels(gameSettings.mipmapLevels);
		renderEngine.loadTickableTexture(TextureMap.LOCATION_BLOCKS_TEXTURE, textureMapBlocks);
		renderEngine.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
		textureMapBlocks.setBlurMipmapDirect(false, gameSettings.mipmapLevels > 0);
		ModelManager modelManager = new ModelManager(textureMapBlocks);
		mcResourceManager.registerReloadListener(modelManager);
		renderItem = new RenderItem(renderEngine, modelManager);
		renderManager = new RenderManager(renderEngine, renderItem);
		itemRenderer = new ItemRenderer(this);
		mcResourceManager.registerReloadListener(renderItem);
		entityRenderer = new EntityRenderer(this, mcResourceManager);
		mcResourceManager.registerReloadListener(entityRenderer);
		blockRenderDispatcher = new BlockRendererDispatcher(modelManager.getBlockModelShapes(), gameSettings);
		mcResourceManager.registerReloadListener(blockRenderDispatcher);
		renderGlobal = new RenderGlobal(this);
		mcResourceManager.registerReloadListener(renderGlobal);
		guiAchievement = new GuiAchievement(this);
		GlStateManager.viewport(0, 0, displayWidth, displayHeight);
		effectRenderer = new EffectRenderer(world, renderEngine);
		checkGLError("Post startup");
		ingameGUI = new GuiIngame(this);

		if (serverName != null) {
			displayGuiScreen(new GuiConnecting(new GuiMainMenu(), this, serverName, serverPort));
		} else {
			displayGuiScreen(new GuiMainMenu());
		}

		renderEngine.deleteTexture(mojangLogo);
		mojangLogo = null;
		loadingScreen = new LoadingScreenRenderer(this);

		if (gameSettings.fullScreen && !fullscreen) {
			toggleFullscreen();
		}

		try {
			Display.setVSyncEnabled(gameSettings.enableVsync);
		} catch (OpenGLException exception) {
			gameSettings.enableVsync = false;
			gameSettings.saveOptions();
		}

		renderGlobal.makeEntityOutlineShader();
	}

	private void registerMetadataSerializers() {
		metadataSerializer_.registerMetadataSectionType(new TextureMetadataSectionSerializer(), TextureMetadataSection.class);
		metadataSerializer_.registerMetadataSectionType(new FontMetadataSectionSerializer(), FontMetadataSection.class);
		metadataSerializer_.registerMetadataSectionType(new AnimationMetadataSectionSerializer(), AnimationMetadataSection.class);
		metadataSerializer_.registerMetadataSectionType(new PackMetadataSectionSerializer(), PackMetadataSection.class);
		metadataSerializer_.registerMetadataSectionType(new LanguageMetadataSectionSerializer(), LanguageMetadataSection.class);
	}

	private void createDisplay() throws LWJGLException {
		Display.setResizable(true);
		Display.setTitle("Minecraft 1.8.9");

		try {
			Display.create(new PixelFormat().withDepthBits(24));
		} catch (LWJGLException exception) {
			LOGGER.error("Couldn't set pixel format", exception);

			if (fullscreen) {
				updateDisplayMode();
			}

			Display.create();
		}
	}

	private void setInitialDisplayMode() throws LWJGLException {
		if (fullscreen) {
			Display.setFullscreen(true);
			DisplayMode displaymode = Display.getDisplayMode();
			displayWidth = Math.max(1, displaymode.getWidth());
			displayHeight = Math.max(1, displaymode.getHeight());
		} else {
			Display.setDisplayMode(new DisplayMode(displayWidth, displayHeight));
		}
	}

	private void setWindowIcon() {
		try (
			InputStream x16 = mcDefaultResourcePack.getInputStream(new ResourceLocation("icons/icon_16x16.png"));
			InputStream x32 = mcDefaultResourcePack.getInputStream(new ResourceLocation("icons/icon_32x32.png"))
		) {
			if (x16 != null && x32 != null) {
				ByteBuffer[] icons = new ByteBuffer[]{readImageToBuffer(x16), readImageToBuffer(x32)};
				Display.setIcon(icons);
			}
		} catch (IOException exception) {
			LOGGER.error("Couldn't set icon", exception);
		}
	}

	public Framebuffer getFramebuffer() {
		return framebufferMc;
	}

	public String getVersion() {
		return "Radiant/1.8.9-M6-Pre-2";
	}

	public void crashed(CrashReport crash) {
		hasCrashed = true;
		crashReporter = crash;
	}

	public void displayCrashReport(CrashReport crashReportIn) {
		File directory = new File(get().mcDataDir, "crash-reports");
		File file = new File(directory, "crash-" + (new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")).format(new Date()) + "-client.txt");
		Bootstrap.printToSYSOUT(crashReportIn.getCompleteReport());

		if (crashReportIn.getFile() != null) {
			Bootstrap.printToSYSOUT("#@!@# Game crashed! Crash report saved to: #@!@# " + crashReportIn.getFile());
			System.exit(-1);
		} else if (crashReportIn.saveToFile(file)) {
			Bootstrap.printToSYSOUT("#@!@# Game crashed! Crash report saved to: #@!@# " + file.getAbsolutePath());
			System.exit(-1);
		} else {
			Bootstrap.printToSYSOUT("#@?@# Game crashed! Crash report could not be saved. #@?@#");
			System.exit(-2);
		}
	}

	public boolean isUnicode() {
		return mcLanguageManager.isCurrentLocaleUnicode() || gameSettings.forceUnicodeFont;
	}

	public void reloadLanguageManager() {
		mcLanguageManager.onResourceManagerReload(mcResourceManager);
		Lang.resourcesReloaded();
	}

	public void refreshResources() {
		List<IResourcePack> list = new ArrayList<>(defaultResourcePacks);

		for (ResourcePackRepository.Entry entry : mcResourcePackRepository.getRepositoryEntries()) {
			list.add(entry.getResourcePack());
		}

		if (mcResourcePackRepository.getResourcePackInstance() != null) {
			list.add(mcResourcePackRepository.getResourcePackInstance());
		}

		try {
			mcResourceManager.reloadResources(list);
		} catch (RuntimeException exception) {
			LOGGER.info("Caught error stitching, removing all assigned resource packs", exception);
			list.clear();
			list.addAll(defaultResourcePacks);
			mcResourcePackRepository.setRepositories(Collections.emptyList());
			mcResourceManager.reloadResources(list);
			gameSettings.resourcePacks.clear();
			gameSettings.incompatibleResourcePacks.clear();
			gameSettings.saveOptions();
		}

		mcLanguageManager.parseLanguageMetadata(list);

		if (renderGlobal != null) {
			renderGlobal.loadRenderers();
		}
	}

	private ByteBuffer readImageToBuffer(InputStream imageStream) throws IOException {
		NativeImage image = NativeImage.loadFromInputStream(imageStream);
		int[] pixelData = image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
		ByteBuffer buffer = ByteBuffer.allocate(4 * pixelData.length);

		for (int pixel : pixelData) {
			buffer.putInt(pixel << 8 | pixel >> 24 & 255);
		}

		buffer.flip();
		return buffer;
	}

	private void updateDisplayMode() throws LWJGLException {
		Set<DisplayMode> set = new HashSet<>();
		Collections.addAll(set, Display.getAvailableDisplayModes());
		DisplayMode displaymode = Display.getDesktopDisplayMode();

		if (!set.contains(displaymode) && IS_RUNNING_ON_MAC) {
			label53:

			for (DisplayMode displaymode1 : MAC_DISPLAY_MODES) {
				boolean flag = true;

				for (DisplayMode displaymode2 : set) {
					if (displaymode2.getBitsPerPixel() == 32 && displaymode2.getWidth() == displaymode1.getWidth() && displaymode2.getHeight() == displaymode1.getHeight()) {
						flag = false;
						break;
					}
				}

				if (!flag) {
					Iterator<DisplayMode> iterator = set.iterator();
					DisplayMode displaymode3;

					do {
						if (!iterator.hasNext()) {
							continue label53;
						}

						displaymode3 = iterator.next();

					} while (displaymode3.getBitsPerPixel() != 32 || displaymode3.getWidth() != displaymode1.getWidth() / 2 || displaymode3.getHeight() != displaymode1.getHeight() / 2);

					displaymode = displaymode3;
				}
			}
		}

		Display.setDisplayMode(displaymode);
		displayWidth = displaymode.getWidth();
		displayHeight = displaymode.getHeight();
	}

	private void drawSplashScreen(TextureManager textureManagerInstance) {
		ScaledResolution scaledResolution = new ScaledResolution(this);
		int i = scaledResolution.getScaleFactor();
		Framebuffer framebuffer = new Framebuffer(scaledResolution.getScaledWidth() * i, scaledResolution.getScaledHeight() * i, true);
		framebuffer.bindFramebuffer(false);
		GlStateManager.matrixMode(5889);
		GlStateManager.loadIdentity();
		GlStateManager.ortho(0.0D, scaledResolution.getScaledWidth(), scaledResolution.getScaledHeight(), 0.0D, 1000.0D, 3000.0D);
		GlStateManager.matrixMode(5888);
		GlStateManager.loadIdentity();
		GlStateManager.translate(0.0F, 0.0F, -2000.0F);
		GlStateManager.disableLighting();
		GlStateManager.disableFog();
		GlStateManager.disableDepth();
		GlStateManager.enableTexture2D();

		try (InputStream inputstream = mcDefaultResourcePack.getInputStream(LOCATION_MOJANG_PNG)) {
			mojangLogo = textureManagerInstance.getDynamicTextureLocation("logo", new DynamicTexture(NativeImage.loadFromInputStream(inputstream)));
			textureManagerInstance.bindTexture(mojangLogo);
		} catch (IOException exception) {
			LOGGER.error("Unable to load logo: {}", LOCATION_MOJANG_PNG, exception);
		}

		Tessellator tessellator = Tessellator.get();
		WorldRenderer renderer = tessellator.getWorldRenderer();
		renderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		renderer.pos(0.0D, displayHeight, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
		renderer.pos(displayWidth, displayHeight, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
		renderer.pos(displayWidth, 0.0D, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
		renderer.pos(0.0D, 0.0D, 0.0D).tex(0.0D, 0.0D).color(255, 255, 255, 255).endVertex();
		tessellator.draw();
		GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
		int j = 256;
		int k = 256;
		draw((scaledResolution.getScaledWidth() - j) / 2, (scaledResolution.getScaledHeight() - k) / 2, 0, 0, j, k, 255, 255, 255, 255);
		GlStateManager.disableLighting();
		GlStateManager.disableFog();
		framebuffer.unbindFramebuffer();
		framebuffer.framebufferRender(scaledResolution.getScaledWidth() * i, scaledResolution.getScaledHeight() * i);
		GlStateManager.enableAlpha();
		GlStateManager.alphaFunc(516, 0.1F);
		updateDisplay();
	}

	public void draw(int posX, int posY, int texU, int texV, int width, int height, int red, int green, int blue, int alpha) {
		float f = 0.00390625F;
		float f1 = 0.00390625F;
		WorldRenderer worldrenderer = Tessellator.get().getWorldRenderer();
		worldrenderer.begin(7, DefaultVertexFormats.POSITION_TEX_COLOR);
		worldrenderer.pos(posX, posY + height, 0.0D).tex(texU * f, (texV + height) * f1).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(posX + width, posY + height, 0.0D).tex((texU + width) * f, (texV + height) * f1).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(posX + width, posY, 0.0D).tex((texU + width) * f, texV * f1).color(red, green, blue, alpha).endVertex();
		worldrenderer.pos(posX, posY, 0.0D).tex(texU * f, texV * f1).color(red, green, blue, alpha).endVertex();
		Tessellator.get().draw();
	}

	public ISaveFormat getSaveLoader() {
		return saveLoader;
	}

	public void displayGuiScreen(GuiScreen screen) {
		if (currentScreen != null) {
			currentScreen.onGuiClosed();
		}

		if (screen == null && world == null) {
			screen = new GuiMainMenu();
		} else if (screen == null && player.getHealth() <= 0.0F) {
			screen = new GuiGameOver();
		}

		if (screen instanceof GuiMainMenu) {
			gameSettings.showDebugInfo = false;
			ingameGUI.getChatGUI().clearChatMessages();
		}

		currentScreen = screen;

		if (screen != null) {
			setIngameNotInFocus();
			ScaledResolution scaledResolution = new ScaledResolution(this);
			int i = scaledResolution.getScaledWidth();
			int j = scaledResolution.getScaledHeight();
			screen.setWorldAndResolution(this, i, j);
			skipRenderWorld = false;
		} else {
			mcSoundHandler.resumeSounds();
			setIngameFocus();
		}
	}

	private void checkGLError(String message) {
		int error = GL11.glGetError();

		if (error != 0) {
			String errorMessage = GLU.gluErrorString(error);
			LOGGER.error("########## GL ERROR ##########");
			LOGGER.error("@ {}", message);
			LOGGER.error("{}: {}", error, errorMessage);
		}
	}

	public void shutdownMinecraftApplet() {
		try {
			LOGGER.info("Stopping!");

			try {
				loadWorld(null);
			} catch (Throwable _) {
			}

			mcSoundHandler.unloadSounds();
		} finally {
			Display.destroy();

			if (!hasCrashed) {
				System.exit(0);
			}
		}

		System.gc();
	}

	private void runGameLoop() throws IOException {
		long i = System.nanoTime();

		if (Display.isCreated() && Display.isCloseRequested()) {
			shutdown();
		}

		if (isGamePaused && world != null) {
			float f = timer.renderPartialTicks;
			timer.updateTimer();
			timer.renderPartialTicks = f;
		} else {
			timer.updateTimer();
		}

		synchronized (scheduledTasks) {
			while (!scheduledTasks.isEmpty()) {
				Util.runTask(scheduledTasks.poll(), LOGGER);
			}
		}

		for (int j = 0; j < timer.elapsedTicks; ++j) {
			runTick();
		}

		checkGLError("Pre render");
		mcSoundHandler.setListener(player, timer.renderPartialTicks);
		GlStateManager.pushMatrix();
		GlStateManager.clear(16640);
		framebufferMc.bindFramebuffer(true);
		GlStateManager.enableTexture2D();

		if (player != null && player.isEntityInsideOpaqueBlock()) {
			gameSettings.thirdPersonView = GameSettings.Perspective.FIRST_PERSON;
		}


		if (!skipRenderWorld) {
			entityRenderer.updateCameraAndRender(timer.renderPartialTicks, i);
		}

		guiAchievement.updateAchievementWindow();
		framebufferMc.unbindFramebuffer();
		GlStateManager.popMatrix();

		GlStateManager.pushMatrix();
		framebufferMc.framebufferRender(displayWidth, displayHeight);
		GlStateManager.popMatrix();

		updateDisplay();
		Thread.yield();
		checkGLError("Post render");
		++fpsCounter;
		isGamePaused = isSingleplayer() && currentScreen != null && currentScreen.doesGuiPauseGame() && !theIntegratedServer.getPublic();
		long k = System.nanoTime();
		frameTimer.addFrame(k - startNanoTime);
		startNanoTime = k;

		while (getSystemTime() >= debugUpdateTime + 1000L) {
			debugFPS = fpsCounter;
			debug = String.format("%d fps (%d chunk update%s) T: %s%s%s%s%s", debugFPS, RenderChunk.renderChunksUpdated, RenderChunk.renderChunksUpdated != 1 ? "s" : "", gameSettings.limitFramerate == GameSettings.Options.FRAMERATE_LIMIT.getValueMax() ? "inf" : Integer.valueOf(gameSettings.limitFramerate), gameSettings.enableVsync ? " vsync" : "", gameSettings.fancyGraphics ? "" : " fast", gameSettings.clouds == 0 ? "" : (gameSettings.clouds == 1 ? " fast-clouds" : " fancy-clouds"), OpenGlHelper.useVbo() ? " vbo" : "");
			RenderChunk.renderChunksUpdated = 0;
			debugUpdateTime += 1000L;
			fpsCounter = 0;
		}

		if (isFramerateLimitBelowMax()) {
			Display.sync(getLimitFramerate());
		}
	}

	public void updateDisplay() {
		Display.update();
		checkWindowResize();
	}

	protected void checkWindowResize() {
		if (!fullscreen && Display.wasResized()) {
			int i = displayWidth;
			int j = displayHeight;
			displayWidth = Display.getWidth();
			displayHeight = Display.getHeight();

			if (displayWidth != i || displayHeight != j) {
				if (displayWidth <= 0) {
					displayWidth = 1;
				}

				if (displayHeight <= 0) {
					displayHeight = 1;
				}

				resize(displayWidth, displayHeight);
			}
		}
	}

	public int getLimitFramerate() {
		return world == null && currentScreen != null ? 30 : gameSettings.limitFramerate;
	}

	public boolean isFramerateLimitBelowMax() {
		return getLimitFramerate() < GameSettings.Options.FRAMERATE_LIMIT.getValueMax();
	}

	public void freeMemory() {
		memoryReserve = new byte[0];
		renderGlobal.deleteAllDisplayLists();

		System.gc();
		loadWorld(null);
		System.gc();
	}

	public void shutdown() {
		running = false;
	}

	public void setIngameFocus() {
		if (Display.isActive()) {
			if (!inGameHasFocus) {
				inGameHasFocus = true;
				mouseHelper.grabMouseCursor();
				displayGuiScreen(null);
				leftClickCounter = 10000;
			}
		}
	}

	public void setIngameNotInFocus() {
		if (inGameHasFocus) {
			KeyBinding.unPressAllKeys();
			inGameHasFocus = false;
			mouseHelper.ungrabMouseCursor();
		}
	}

	public void displayInGameMenu() {
		if (currentScreen == null) {
			displayGuiScreen(new GuiIngameMenu());

			if (isSingleplayer() && !theIntegratedServer.getPublic()) {
				mcSoundHandler.pauseSounds();
			}
		}
	}

	private void sendClickBlockToController(boolean leftClick) {
		if (!leftClick) {
			leftClickCounter = 0;
		}

		if (leftClickCounter <= 0 && !player.isUsingItem()) {
			if (leftClick && objectMouseOver != null && objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
				BlockPos blockPos = objectMouseOver.getBlockPos();

				if (world.getBlockState(blockPos).getBlock().getMaterial() != Material.AIR && playerController.onPlayerDamageBlock(blockPos, objectMouseOver.sideHit)) {
					effectRenderer.addBlockHitEffects(blockPos, objectMouseOver.sideHit);
					player.swingItem();
				}
			} else {
				playerController.resetBlockRemoving();
			}
		}
	}

	private void clickMouse() {
		if (leftClickCounter <= 0) {
			player.swingItem();

			if (objectMouseOver == null) {
				LOGGER.error("Null returned as 'hitResult', this shouldn't happen!");

				if (playerController.isNotCreative()) {
					leftClickCounter = 10;
				}
			} else {
				switch (objectMouseOver.typeOfHit) {
					case ENTITY:
						playerController.attackEntity(player, objectMouseOver.entityHit);
						break;

					case BLOCK:
						BlockPos blockpos = objectMouseOver.getBlockPos();

						if (world.getBlockState(blockpos).getBlock().getMaterial() != Material.AIR) {
							playerController.clickBlock(blockpos, objectMouseOver.sideHit);
							break;
						}

					case MISS:
					default:
						if (playerController.isNotCreative()) {
							leftClickCounter = 10;
						}
				}
			}
		}
	}


	private void rightClickMouse() {
		if (!playerController.getIsHittingBlock()) {
			rightClickDelayTimer = 4;
			boolean flag = true;
			ItemStack itemstack = player.inventory.getCurrentItem();

			if (objectMouseOver == null) {
				LOGGER.warn("Null returned as 'hitResult', this shouldn't happen!");
			} else {
				switch (objectMouseOver.typeOfHit) {
					case ENTITY:
						if (playerController.isPlayerRightClickingOnEntity(player, objectMouseOver.entityHit, objectMouseOver)) {
							flag = false;
						} else if (playerController.interactWithEntitySendPacket(player, objectMouseOver.entityHit)) {
							flag = false;
						}

						break;

					case BLOCK:
						BlockPos blockpos = objectMouseOver.getBlockPos();

						if (world.getBlockState(blockpos).getBlock().getMaterial() != Material.AIR) {
							int i = itemstack != null ? itemstack.stackSize : 0;

							if (playerController.onPlayerRightClick(player, world, itemstack, blockpos, objectMouseOver.sideHit, objectMouseOver.hitVec)) {
								flag = false;
								player.swingItem();
							}

							if (itemstack == null) {
								return;
							}

							if (itemstack.stackSize == 0) {
								player.inventory.mainInventory[player.inventory.currentItem] = null;
							} else if (itemstack.stackSize != i || playerController.isInCreativeMode()) {
								entityRenderer.itemRenderer.resetEquippedProgress();
							}
						}
				}
			}

			if (flag) {
				ItemStack itemstack1 = player.inventory.getCurrentItem();

				if (itemstack1 != null && playerController.sendUseItem(player, world, itemstack1)) {
					entityRenderer.itemRenderer.resetEquippedProgress2();
				}
			}
		}
	}

	public void toggleFullscreen() {
		try {
			fullscreen = !fullscreen;
			gameSettings.fullScreen = fullscreen;

			if (fullscreen) {
				updateDisplayMode();
				displayWidth = Display.getDisplayMode().getWidth();
				displayHeight = Display.getDisplayMode().getHeight();

			} else {
				Display.setDisplayMode(new DisplayMode(tempDisplayWidth, tempDisplayHeight));
				displayWidth = tempDisplayWidth;
				displayHeight = tempDisplayHeight;

			}
			if (displayWidth <= 0) {
				displayWidth = 1;
			}
			if (displayHeight <= 0) {
				displayHeight = 1;
			}

			if (currentScreen != null) {
				resize(displayWidth, displayHeight);
			} else {
				updateFramebufferSize();
			}

			Display.setFullscreen(fullscreen);
			Display.setVSyncEnabled(gameSettings.enableVsync);
			updateDisplay();
		} catch (Exception exception) {
			LOGGER.error("Couldn't toggle fullscreen", exception);
		}
	}

	private void resize(int width, int height) {
		displayWidth = Math.max(1, width);
		displayHeight = Math.max(1, height);

		if (currentScreen != null) {
			ScaledResolution scaledresolution = new ScaledResolution(this);
			currentScreen.onResize(this, scaledresolution.getScaledWidth(), scaledresolution.getScaledHeight());
		}

		loadingScreen = new LoadingScreenRenderer(this);
		updateFramebufferSize();
	}

	private void updateFramebufferSize() {
		framebufferMc.createBindFramebuffer(displayWidth, displayHeight);

		if (entityRenderer != null) {
			entityRenderer.updateShaderGroupSize(displayWidth, displayHeight);
		}
	}

	public MusicTicker getMusicTicker() {
		return mcMusicTicker;
	}

	public void runTick() throws IOException {
		if (rightClickDelayTimer > 0) {
			--rightClickDelayTimer;
		}


		if (!isGamePaused) {
			ingameGUI.updateTick();
		}

		entityRenderer.getMouseOver(1.0F);

		if (!isGamePaused && world != null) {
			playerController.updateController();
		}

		if (!isGamePaused) {
			renderEngine.tick();
		}

		if (currentScreen == null && player != null) {
			if (player.getHealth() <= 0.0F) {
				displayGuiScreen(null);
			} else if (player.isPlayerSleeping() && world != null) {
				displayGuiScreen(new GuiSleepMP());
			}
		} else if (currentScreen != null && currentScreen instanceof GuiSleepMP && !player.isPlayerSleeping()) {
			displayGuiScreen(null);
		}

		if (currentScreen != null) {
			leftClickCounter = 10000;
		}

		if (currentScreen != null) {
			try {
				currentScreen.handleInput();
			} catch (Throwable throwable) {
				CrashReport report = CrashReport.makeCrashReport(throwable, "Updating screen events");
				CrashReportCategory category = report.makeCategory("Affected screen");
				category.addCrashSectionCallable("Screen Name", () -> currentScreen.getClass().getCanonicalName());
				throw new ReportedException(report);
			}

			if (currentScreen != null) {
				try {
					currentScreen.updateScreen();
				} catch (Throwable throwable) {
					CrashReport report = CrashReport.makeCrashReport(throwable, "Ticking screen");
					CrashReportCategory category = report.makeCategory("Affected screen");
					category.addCrashSectionCallable("Screen Name", () -> currentScreen.getClass().getCanonicalName());
					throw new ReportedException(report);
				}
			}
		}

		if (currentScreen == null || currentScreen.allowUserInput) {
			while (Mouse.next()) {
				int i = Mouse.getEventButton();
				KeyBinding.setKeyBindState(i - 100, Mouse.getEventButtonState());

				if (Mouse.getEventButtonState()) {
					if (player.isSpectator() && i == 2) {
						ingameGUI.getSpectatorGui().func_175261_b();
					} else {
						KeyBinding.onTick(i - 100);
					}
				}

				long i1 = getSystemTime() - systemTime;

				if (i1 <= 200L) {
					int j = Mouse.getEventDWheel();

					if (j != 0) {
						if (player.isSpectator()) {
							j = j < 0 ? -1 : 1;

							if (ingameGUI.getSpectatorGui().func_175262_a()) {
								ingameGUI.getSpectatorGui().func_175259_b(-j);
							} else {
								float f = MathHelper.clamp(player.capabilities.getFlySpeed() + j * 0.005F, 0.0F, 0.2F);
								player.capabilities.setFlySpeed(f);
							}
						} else {
							player.inventory.changeCurrentItem(j);
						}
					}

					if (currentScreen == null) {
						if (!inGameHasFocus && Mouse.getEventButtonState()) {
							setIngameFocus();
						}
					} else {
						currentScreen.handleMouseInput();
					}
				}
			}

			if (leftClickCounter > 0) {
				--leftClickCounter;
			}

			while (Keyboard.next()) {
				int k = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() + 256 : Keyboard.getEventKey();
				KeyBinding.setKeyBindState(k, Keyboard.getEventKeyState());

				if (Keyboard.getEventKeyState()) {
					KeyBinding.onTick(k);
				}

				if (debugCrashKeyPressTime > 0L) {
					if (getSystemTime() - debugCrashKeyPressTime >= 6000L) {
						throw new ReportedException(new CrashReport("Manually triggered debug crash", new Throwable()));
					}

					if (!Keyboard.isKeyDown(46) || !Keyboard.isKeyDown(61)) {
						debugCrashKeyPressTime = -1L;
					}
				} else if (Keyboard.isKeyDown(46) && Keyboard.isKeyDown(61)) {
					debugCrashKeyPressTime = getSystemTime();
				}

				dispatchKeypresses();

				if (Keyboard.getEventKeyState()) {
					if (k == 62 && entityRenderer != null) {
						entityRenderer.switchUseShader();
					}

					if (currentScreen != null) {
						currentScreen.handleKeyboardInput();
					} else {
						switch (k) {
							case 1 -> displayInGameMenu();

							case 20, 31 -> {
								if (Keyboard.isKeyDown(61))
									refreshResources();
							}

							case 25 -> {
								if (Keyboard.isKeyDown(61)) {
									gameSettings.pauseOnLostFocus = !gameSettings.pauseOnLostFocus;
									gameSettings.saveOptions();
								}
							}

							case 30 -> {
								if (Keyboard.isKeyDown(61))
									renderGlobal.loadRenderers();
							}

							case 32 -> {
								if (Keyboard.isKeyDown(61) && ingameGUI != null)
									ingameGUI.getChatGUI().clearChatMessages();
							}

							case 33 -> {
								if (Keyboard.isKeyDown(61))
									gameSettings.setOptionValue(GameSettings.Options.RENDER_DISTANCE, GuiScreen.isShiftKeyDown() ? -1 : 1);
							}

							case 35 -> {
								if (Keyboard.isKeyDown(61)) {
									gameSettings.advancedItemTooltips = !gameSettings.advancedItemTooltips;
									gameSettings.saveOptions();
								}
							}

							case 48 -> {
								if (Keyboard.isKeyDown(61))
									renderManager.setDebugBoundingBox(!renderManager.isDebugBoundingBox());
							}

							case 59 -> gameSettings.hideGUI = !gameSettings.hideGUI;

							case 61 -> gameSettings.showDebugInfo = !gameSettings.showDebugInfo;
						}

						if (gameSettings.keyBindTogglePerspective.isPressed()) {

							gameSettings.thirdPersonView = gameSettings.thirdPersonView.next();

							if (gameSettings.thirdPersonView == GameSettings.Perspective.FIRST_PERSON) {
								entityRenderer.loadEntityShader(getRenderViewEntity());
							}
							// IMPROVEMENT: Don't reset the shader when switching to 3rd person view

							renderGlobal.setDisplayListEntitiesDirty();
						}

						if (gameSettings.keyBindSmoothCamera.isPressed()) {
							gameSettings.smoothCamera = !gameSettings.smoothCamera;
						}
					}
				}
			}

			for (int l = 0; l < 9; ++l) {
				if (gameSettings.keyBindsHotbar[l].isPressed()) {
					if (player.isSpectator()) {
						ingameGUI.getSpectatorGui().func_175260_a(l);
					} else {
						player.inventory.currentItem = l;
					}
				}
			}

			boolean flag = gameSettings.chatVisibility != EntityPlayer.ChatVisibility.HIDDEN;

			while (gameSettings.keyBindInventory.isPressed()) {
				if (playerController.isRidingHorse()) {
					player.sendHorseInventory();
				} else {
					getNetHandler().addToSendQueue(new C16PacketClientStatus(C16PacketClientStatus.EnumState.OPEN_INVENTORY_ACHIEVEMENT));
					displayGuiScreen(new GuiInventory(player));
				}
			}

			while (gameSettings.keyBindDrop.isPressed()) {
				if (!player.isSpectator()) {
					player.dropOneItem(GuiScreen.isCtrlKeyDown());
				}
			}

			while (gameSettings.keyBindChat.isPressed() && flag) {
				displayGuiScreen(new GuiChat());
			}

			if (currentScreen == null && gameSettings.keyBindCommand.isPressed() && flag) {
				displayGuiScreen(new GuiChat("/"));
			}

			if (player.isUsingItem()) {
				if (!gameSettings.keyBindUseItem.isKeyDown()) {
					playerController.onStoppedUsingItem(player);
				}

				// Apparently, this is needed to prevent incorrect behavior which could flag anticheats
				while (this.gameSettings.keyBindAttack.isPressed()) {
					;
				}
				while (this.gameSettings.keyBindUseItem.isPressed()) {
					;
				}
				while (this.gameSettings.keyBindPickBlock.isPressed()) {
					;
				}
			} else {
				while (gameSettings.keyBindAttack.isPressed()) {
					clickMouse();
				}

				while (gameSettings.keyBindUseItem.isPressed()) {
					rightClickMouse();
				}

				while (gameSettings.keyBindPickBlock.isPressed()) {
					middleClickMouse();
				}
			}

			if (gameSettings.keyBindUseItem.isKeyDown() && rightClickDelayTimer == 0 && !player.isUsingItem()) {
				rightClickMouse();
			}

			sendClickBlockToController(currentScreen == null && gameSettings.keyBindAttack.isKeyDown() && inGameHasFocus);
		}

		if (world != null) {
			if (player != null) {
				++joinPlayerCounter;

				if (joinPlayerCounter == 30) {
					joinPlayerCounter = 0;
					world.joinEntityInSurroundings(player);
				}
			}

			if (!isGamePaused) {
				entityRenderer.updateRenderer();
			}

			if (!isGamePaused) {
				renderGlobal.updateClouds();
			}

			if (!isGamePaused) {
				if (world.getLastLightningBolt() > 0) {
					world.setLastLightningBolt(world.getLastLightningBolt() - 1);
				}

				world.updateEntities();
			}
		} else if (entityRenderer.isShaderActive()) {
			entityRenderer.stopUseShader();
		}

		if (!isGamePaused) {
			mcMusicTicker.update();
			mcSoundHandler.update();
		}

		if (world != null) {
			if (!isGamePaused) {
				world.setAllowedSpawnTypes(world.getDifficulty() != Difficulty.PEACEFUL, true);

				try {
					world.tick();
				} catch (Throwable throwable) {
					CrashReport report = CrashReport.makeCrashReport(throwable, "Exception in world tick");

					if (world == null) {
						CrashReportCategory category = report.makeCategory("Affected level");
						category.addCrashSection("Problem", "Level is null!");
					} else {
						world.addWorldInfoToCrashReport(report);
					}

					throw new ReportedException(report);
				}
			}

			if (!isGamePaused && world != null) {
				world.doVoidFogParticles(MathHelper.floor(player.posX), MathHelper.floor(player.posY), MathHelper.floor(player.posZ));
			}

			if (!isGamePaused) {
				effectRenderer.updateEffects();
			}
		} else if (myNetworkManager != null) {
			myNetworkManager.processReceivedPackets();
		}

		systemTime = getSystemTime();
	}

	public void launchIntegratedServer(String folderName, String worldName, WorldSettings worldSettings) {
		loadWorld(null);
		System.gc();
		ISaveHandler saveHandler = saveLoader.getSaveLoader(folderName, false);
		WorldInfo worldInfo = saveHandler.loadWorldInfo();

		if (worldInfo == null && worldSettings != null) {
			worldInfo = new WorldInfo(worldSettings, folderName);
			saveHandler.saveWorldInfo(worldInfo);
		}

		if (worldSettings == null) {
			worldSettings = new WorldSettings(worldInfo);
		}

		try {
			theIntegratedServer = new IntegratedServer(this, folderName, worldName, worldSettings);
			theIntegratedServer.startServerThread();
			integratedServerIsRunning = true;
		} catch (Throwable throwable) {
			CrashReport report = CrashReport.makeCrashReport(throwable, "Starting integrated server");
			CrashReportCategory category = report.makeCategory("Starting integrated server");
			category.addCrashSection("Level ID", folderName);
			category.addCrashSection("Level Name", worldName);
			throw new ReportedException(report);
		}

		loadingScreen.displaySavingString(I18n.format("menu.loadingLevel"));

		while (!theIntegratedServer.serverIsInRunLoop()) {
			String userMessage = theIntegratedServer.getUserMessage();

			if (userMessage != null) {
				loadingScreen.displayLoadingString(I18n.format(userMessage));
			} else {
				loadingScreen.displayLoadingString("");
			}
		}

		displayGuiScreen(null);
		SocketAddress address = theIntegratedServer.getNetworkSystem().addLocalEndpoint();
		NetworkManager manager = NetworkManager.provideLocalClient(address);
		manager.setNetHandler(new NetHandlerLoginClient(manager, this, null));
		manager.sendPacket(new C00Handshake(47, address.toString(), 0, NetworkState.LOGIN));
		manager.sendPacket(new C00PacketLoginStart(getSession().getProfile()));
		myNetworkManager = manager;
	}

	public void loadWorld(WorldClient worldClient) {
		loadWorld(worldClient, "");
	}

	public void loadWorld(WorldClient worldClient, String loadingMessage) {
		if (worldClient == null) {
			NetHandlerPlayClient handlerPlayClient = getNetHandler();

			if (handlerPlayClient != null) {
				handlerPlayClient.cleanup();
			}

			if (theIntegratedServer != null && theIntegratedServer.isAnvilFileSet()) {
				theIntegratedServer.initiateShutdown();
				theIntegratedServer.setStaticInstance();
			}

			theIntegratedServer = null;
			guiAchievement.clearAchievements();
			entityRenderer.getMapItemRenderer().clearLoadedMaps();
		}

		renderViewEntity = null;
		myNetworkManager = null;

		if (loadingScreen != null) {
			loadingScreen.resetProgressAndMessage(loadingMessage);
			loadingScreen.displayLoadingString("");
		}

		if (worldClient == null && world != null) {
			mcResourcePackRepository.clearResourcePack();
			ingameGUI.resetPlayersOverlayFooterHeader();
			setServerData(null);
			integratedServerIsRunning = false;
		}

		mcSoundHandler.stopSounds();
		world = worldClient;

		if (worldClient != null) {
			if (renderGlobal != null) {
				renderGlobal.setWorldAndLoadRenderers(worldClient);
			}

			if (effectRenderer != null) {
				effectRenderer.clearEffects(worldClient);
			}

			if (player == null) {
				player = playerController.func_178892_a(worldClient, new StatFileWriter());
				playerController.flipPlayer(player);
			}

			player.preparePlayerToSpawn();
			worldClient.spawnEntityInWorld(player);
			player.movementInput = new MovementInputFromOptions(gameSettings);
			playerController.setPlayerCapabilities(player);
			renderViewEntity = player;
		} else {
			saveLoader.flushCache();
			player = null;
		}

		System.gc();
		systemTime = 0L;
	}

	public void setDimensionAndSpawnPlayer(int dimension) {
		world.setInitialSpawnLocation();
		world.removeAllEntities();
		int i = 0;
		String s = null;

		if (player != null) {
			i = player.getEntityId();
			world.removeEntity(player);
			s = player.getClientBrand();
		}

		renderViewEntity = null;
		EntityPlayerSP entityplayersp = player;
		player = playerController.func_178892_a(world, player == null ? new StatFileWriter() : player.getStatFileWriter());
		player.getDataWatcher().updateWatchedObjectsFromList(entityplayersp.getDataWatcher().getAllWatched());
		player.dimension = dimension;
		renderViewEntity = player;
		player.preparePlayerToSpawn();
		player.setClientBrand(s);
		world.spawnEntityInWorld(player);
		playerController.flipPlayer(player);
		player.movementInput = new MovementInputFromOptions(gameSettings);
		player.setEntityId(i);
		playerController.setPlayerCapabilities(player);
		player.setReducedDebug(entityplayersp.hasReducedDebug());

		if (currentScreen instanceof GuiGameOver) {
			displayGuiScreen(null);
		}
	}

	public NetHandlerPlayClient getNetHandler() {
		return player != null ? player.sendQueue : null;
	}

	public static boolean isGuiEnabled() {
		return instance == null || !instance.gameSettings.hideGUI;
	}

	public static boolean isAmbientOcclusionEnabled() {
		return instance != null && instance.gameSettings.ambientOcclusion != 0;
	}

	private void middleClickMouse() {
		if (objectMouseOver != null) {
			boolean flag = player.capabilities.isCreativeMode;
			int i = 0;
			boolean flag1 = false;
			TileEntity tileentity = null;
			Item item;

			if (objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
				BlockPos blockpos = objectMouseOver.getBlockPos();
				Block block = world.getBlockState(blockpos).getBlock();

				if (block.getMaterial() == Material.AIR) {
					return;
				}

				item = block.getItem(world, blockpos);

				if (item == null) {
					return;
				}

				if (flag && GuiScreen.isCtrlKeyDown()) {
					tileentity = world.getTileEntity(blockpos);
				}

				Block block1 = item instanceof ItemBlock && !block.isFlowerPot() ? Block.getBlockFromItem(item) : block;
				i = block1.getDamageValue(world, blockpos);
				flag1 = item.getHasSubtypes();
			} else {
				if (objectMouseOver.typeOfHit != MovingObjectPosition.MovingObjectType.ENTITY || objectMouseOver.entityHit == null || !flag) {
					return;
				}

				switch (objectMouseOver.entityHit) {
					case EntityPainting ignored -> item = Items.PAINTING;
					case EntityLeashKnot ignored -> item = Items.LEAD;
					case EntityItemFrame entityitemframe -> {
						ItemStack itemstack = entityitemframe.getDisplayedItem();

						if (itemstack == null) {
							item = Items.ITEM_FRAME;
						} else {
							item = itemstack.getItem();
							i = itemstack.getMetadata();
							flag1 = true;
						}
					}
					case EntityMinecart entityminecart -> item = switch (entityminecart.getMinecartType()) {
						case FURNACE -> Items.FURNACE_MINECART;
						case CHEST -> Items.CHEST_MINECART;
						case TNT -> Items.TNT_MINECART;
						case HOPPER -> Items.HOPPER_MINECART;
						case COMMAND_BLOCK -> Items.COMMAND_BLOCK_MINECART;
						default -> Items.MINECART;
					};
					case EntityBoat ignored -> item = Items.BOAT;
					case EntityArmorStand ignored -> item = Items.ARMOR_STAND;
					default -> {
						item = Items.SPAWN_EGG;
						i = EntityList.getEntityID(objectMouseOver.entityHit);
						flag1 = true;

						if (!EntityList.entityEggs.containsKey(i)) {
							return;
						}
					}
				}
			}

			InventoryPlayer inventoryplayer = player.inventory;

			if (tileentity == null) {
				inventoryplayer.setCurrentItem(item, i, flag1, flag);
			} else {
				ItemStack itemstack1 = pickBlockWithNBT(item, i, tileentity);
				inventoryplayer.setInventorySlotContents(inventoryplayer.currentItem, itemstack1);
			}

			if (flag) {
				int j = player.inventoryContainer.inventorySlots.size() - 9 + inventoryplayer.currentItem;
				playerController.sendSlotPacket(inventoryplayer.getStackInSlot(inventoryplayer.currentItem), j);
			}
		}
	}

	private ItemStack pickBlockWithNBT(Item itemIn, int meta, TileEntity tileEntity) {
		ItemStack itemStack = new ItemStack(itemIn, 1, meta);
		NBTTagCompound tagCompound = new NBTTagCompound();
		tileEntity.writeToNBT(tagCompound);

		if (itemIn == Items.SKULL && tagCompound.hasKey("Owner")) {
			NBTTagCompound owner = tagCompound.getCompoundTag("Owner");
			NBTTagCompound finalCompound = new NBTTagCompound();
			finalCompound.setTag("SkullOwner", owner);
			itemStack.setTagCompound(finalCompound);
		} else {
			itemStack.setTagInfo("BlockEntityTag", tagCompound);
			NBTTagCompound nbttagcompound1 = new NBTTagCompound();
			NBTTagList tagList = new NBTTagList();
			tagList.appendTag(new NBTTagString("(+NBT)"));
			nbttagcompound1.setTag("Lore", tagList);
			itemStack.setTagInfo("display", nbttagcompound1);
		}
		return itemStack;
	}

	public CrashReport addGraphicsAndWorldToCrashReport(CrashReport crash) {
		crash.getCategory().addCrashSectionCallable("Launched Version", this::getVersion);
		crash.getCategory().addCrashSectionCallable("LWJGL", Version::getVersion);
		crash.getCategory().addCrashSectionCallable("OpenGL", () -> GL11.glGetString(GL11.GL_RENDERER) + " GL version " + GL11.glGetString(GL11.GL_VERSION) + ", " + GL11.glGetString(GL11.GL_VENDOR));
		crash.getCategory().addCrashSectionCallable("GL Caps", OpenGlHelper::getLogText);
		crash.getCategory().addCrashSectionCallable("Using VBOs", () -> gameSettings.useVbo ? "Yes" : "No");
		crash.getCategory().addCrashSectionCallable("Is Modded", () -> Minecraft.class.getSigners() == null ? "Very likely; Jar signature invalidated" : "Probably not. Jar signature remains.");
		crash.getCategory().addCrashSectionCallable("Type", () -> "Client (map_client.txt)");
		crash.getCategory().addCrashSectionCallable("Resource Packs", () -> {
			StringBuilder builder = new StringBuilder();

			for (String pack : gameSettings.resourcePacks) {
				if (!builder.isEmpty()) {
					builder.append(", ");
				}

				builder.append(pack);

				if (gameSettings.incompatibleResourcePacks.contains(pack)) {
					builder.append(" (incompatible)");
				}
			}

			return builder.toString();
		});
		crash.getCategory().addCrashSectionCallable("Current Language", () -> mcLanguageManager.getCurrentLanguage().toString());

		if (world != null) {
			world.addWorldInfoToCrashReport(crash);
		}

		return crash;
	}

	public static Minecraft get() {
		return instance;
	}

	public ListenableFuture<Object> scheduleResourcesRefresh() {
		return addScheduledTask(this::refreshResources);
	}

	public void setServerData(ServerData serverDataIn) {
		currentServerData = serverDataIn;
	}

	public ServerData getCurrentServerData() {
		return currentServerData;
	}

	public boolean isIntegratedServerRunning() {
		return integratedServerIsRunning;
	}

	public boolean isSingleplayer() {
		return integratedServerIsRunning && theIntegratedServer != null;
	}

	public IntegratedServer getIntegratedServer() {
		return theIntegratedServer;
	}

	public static void stopIntegratedServer() {
		if (instance != null) {
			IntegratedServer integratedserver = instance.getIntegratedServer();

			if (integratedserver != null) {
				integratedserver.stopServer();
			}
		}
	}

	public static long getSystemTime() {
		return (long)(GLFW.glfwGetTime() * 1000.D);
	}

	public boolean isFullScreen() {
		return fullscreen;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public PropertyMap getProfileProperties() {
		if (profileProperties.isEmpty()) {
			GameProfile gameprofile = getSessionService().fillProfileProperties(session.getProfile(), false);
			profileProperties.putAll(gameprofile.getProperties());
		}

		return profileProperties;
	}

	public Proxy getProxy() {
		return proxy;
	}

	public TextureManager getTextureManager() {
		return renderEngine;
	}

	public IResourceManager getResourceManager() {
		return mcResourceManager;
	}

	public ResourcePackRepository getResourcePackRepository() {
		return mcResourcePackRepository;
	}

	public LanguageManager getLanguageManager() {
		return mcLanguageManager;
	}

	public TextureMap getTextureMapBlocks() {
		return textureMapBlocks;
	}

	public boolean isGamePaused() {
		return isGamePaused;
	}

	public SoundHandler getSoundHandler() {
		return mcSoundHandler;
	}

	public MusicTicker.MusicType getAmbientMusicType() {
		return player != null ? (player.worldObj.provider instanceof WorldProviderHell ? MusicTicker.MusicType.NETHER : (player.worldObj.provider instanceof WorldProviderEnd ? (BossStatus.bossName != null && BossStatus.statusBarTime > 0 ? MusicTicker.MusicType.END_BOSS : MusicTicker.MusicType.END) : (player.capabilities.isCreativeMode && player.capabilities.allowFlying ? MusicTicker.MusicType.CREATIVE : MusicTicker.MusicType.GAME))) : MusicTicker.MusicType.MENU;
	}

	public void dispatchKeypresses() {
		int i = Keyboard.getEventKey() == 0 ? Keyboard.getEventCharacter() : Keyboard.getEventKey();

		if (i != 0 && !Keyboard.isRepeatEvent()) {
			if (!(currentScreen instanceof GuiControls guiControls) || guiControls.time <= getSystemTime() - 20L) {
				if (Keyboard.getEventKeyState()) {
					if (i == gameSettings.keyBindFullscreen.getKeyCode()) {
						toggleFullscreen();
					} else if (i == gameSettings.keyBindScreenshot.getKeyCode()) {
						ingameGUI.getChatGUI().printChatMessage(ScreenShotHelper.saveScreenshot(mcDataDir, displayWidth, displayHeight, framebufferMc));
					}
				}
			}
		}
	}

	public MinecraftSessionService getSessionService() {
		return sessionService;
	}

	public SkinManager getSkinManager() {
		return skinManager;
	}

	public Entity getRenderViewEntity() {
		return renderViewEntity;
	}

	public void setRenderViewEntity(Entity viewingEntity) {
		renderViewEntity = viewingEntity;
		entityRenderer.loadEntityShader(viewingEntity);
	}

	public <V> ListenableFuture<V> addScheduledTask(Callable<V> callableToSchedule) {
		Objects.requireNonNull(callableToSchedule);

		if (!isCallingFromMinecraftThread()) {
			ListenableFutureTask<V> listenablefuturetask = ListenableFutureTask.create(callableToSchedule);

			synchronized (scheduledTasks) {
				scheduledTasks.add(listenablefuturetask);
				return listenablefuturetask;
			}
		} else {
			try {
				return Futures.immediateFuture(callableToSchedule.call());
			} catch (Exception exception) {
				return Futures.immediateFailedFuture(exception);
			}
		}
	}

	@Override
    public ListenableFuture<Object> addScheduledTask(Runnable runnableToSchedule) {
		Objects.requireNonNull(runnableToSchedule);
		return addScheduledTask(Executors.callable(runnableToSchedule));
	}

	@Override
    public boolean isCallingFromMinecraftThread() {
		return Thread.currentThread() == mcThread;
	}

	public BlockRendererDispatcher getBlockRendererDispatcher() {
		return blockRenderDispatcher;
	}

	public RenderManager getRenderManager() {
		return renderManager;
	}

	public RenderItem getRenderItem() {
		return renderItem;
	}

	public ItemRenderer getItemRenderer() {
		return itemRenderer;
	}

	public static int getDebugFPS() {
		return debugFPS;
	}

	public FrameTimer getFrameTimer() {
		return frameTimer;
	}

	public static Map<String, String> getSessionInfo() {
		return Map.of(
				"X-Minecraft-Username", get().getSession().getUsername(),
				"X-Minecraft-UUID", get().getSession().getPlayerID(),
				"X-Minecraft-Version", "1.8.9"
		);
	}

	public DefaultResourcePack getDefaultResourcePack() {
		return mcDefaultResourcePack;
	}
}
