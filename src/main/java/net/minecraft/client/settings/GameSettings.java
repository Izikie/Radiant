package net.minecraft.client.settings;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gson.Gson;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.SoundCategory;
import net.minecraft.client.gui.GuiNewChat;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EnumPlayerModelParts;
import net.minecraft.network.play.client.C15PacketClientSettings;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumDifficulty;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.Display;

public class GameSettings {
    private static final Logger logger = LogManager.getLogger();
    private static final Gson gson = new Gson();
    private static final ParameterizedType typeListString = new ParameterizedType() {
        public Type[] getActualTypeArguments() {
            return new Type[]{String.class};
        }

        public Type getRawType() {
            return List.class;
        }

        public Type getOwnerType() {
            return null;
        }
    };

    /**
     * GUI scale values
     */
    private static final String[] GUISCALES = new String[]{"options.guiScale.auto", "options.guiScale.small", "options.guiScale.normal", "options.guiScale.large"};
    private static final String[] PARTICLES = new String[]{"options.particles.all", "options.particles.decreased", "options.particles.minimal"};
    private static final String[] AMBIENT_OCCLUSIONS = new String[]{"options.ao.off", "options.ao.min", "options.ao.max"};
    private static final String[] CLOUDS_TYPES = new String[]{"options.off", "options.graphics.fast", "options.graphics.fancy"};
    public float mouseSensitivity = 0.5F;
    public boolean invertMouse;
    public int renderDistanceChunks = -1;
    public boolean viewBobbing = true;
    public boolean anaglyph;
    public boolean fboEnable = true;
    public int limitFramerate = 120;

    /**
     * Clouds flag
     */
    public int clouds = 2;
    public boolean fancyGraphics = true;

    /**
     * Smooth Lighting
     */
    public int ambientOcclusion = 2;
    public List<String> resourcePacks = Lists.<String>newArrayList();
    public List<String> incompatibleResourcePacks = Lists.<String>newArrayList();
    public EntityPlayer.EnumChatVisibility chatVisibility = EntityPlayer.EnumChatVisibility.FULL;
    public boolean chatColours = true;
    public boolean chatLinks = true;
    public boolean chatLinksPrompt = true;
    public float chatOpacity = 1.0F;
    public boolean snooperEnabled = true;
    public boolean fullScreen;
    public boolean enableVsync = true;
    public boolean useVbo = false;
    public boolean allowBlockAlternatives = true;
    public boolean reducedDebugInfo = false;
    public boolean hideServerAddress;

    /**
     * Whether to show advanced information on item tooltips, toggled by F3+H
     */
    public boolean advancedItemTooltips;

    /**
     * Whether to pause when the game loses focus, toggled by F3+P
     */
    public boolean pauseOnLostFocus = true;
    private final Set<EnumPlayerModelParts> setModelParts = Sets.newHashSet(EnumPlayerModelParts.values());
    public boolean touchscreen;
    public int overrideWidth;
    public int overrideHeight;
    public boolean heldItemTooltips = true;
    public float chatScale = 1.0F;
    public float chatWidth = 1.0F;
    public float chatHeightUnfocused = 0.44366196F;
    public float chatHeightFocused = 1.0F;
    public boolean showInventoryAchievementHint = true;
    public int mipmapLevels = 4;
    private Map<SoundCategory, Float> mapSoundLevels = Maps.newEnumMap(SoundCategory.class);

    public boolean useNativeTransport = true;
    public boolean entityShadows = true;
    public KeyBinding keyBindForward = new KeyBinding("key.forward", 17, "key.categories.movement");
    public KeyBinding keyBindLeft = new KeyBinding("key.left", 30, "key.categories.movement");
    public KeyBinding keyBindBack = new KeyBinding("key.back", 31, "key.categories.movement");
    public KeyBinding keyBindRight = new KeyBinding("key.right", 32, "key.categories.movement");
    public KeyBinding keyBindJump = new KeyBinding("key.jump", 57, "key.categories.movement");
    public KeyBinding keyBindSneak = new KeyBinding("key.sneak", 42, "key.categories.movement");
    public KeyBinding keyBindSprint = new KeyBinding("key.sprint", 29, "key.categories.movement");
    public KeyBinding keyBindInventory = new KeyBinding("key.inventory", 18, "key.categories.inventory");
    public KeyBinding keyBindUseItem = new KeyBinding("key.use", -99, "key.categories.gameplay");
    public KeyBinding keyBindDrop = new KeyBinding("key.drop", 16, "key.categories.gameplay");
    public KeyBinding keyBindAttack = new KeyBinding("key.attack", -100, "key.categories.gameplay");
    public KeyBinding keyBindPickBlock = new KeyBinding("key.pickItem", -98, "key.categories.gameplay");
    public KeyBinding keyBindChat = new KeyBinding("key.chat", 20, "key.categories.multiplayer");
    public KeyBinding keyBindPlayerList = new KeyBinding("key.playerlist", 15, "key.categories.multiplayer");
    public KeyBinding keyBindCommand = new KeyBinding("key.command", 53, "key.categories.multiplayer");
    public KeyBinding keyBindScreenshot = new KeyBinding("key.screenshot", 60, "key.categories.misc");
    public KeyBinding keyBindTogglePerspective = new KeyBinding("key.togglePerspective", 63, "key.categories.misc");
    public KeyBinding keyBindSmoothCamera = new KeyBinding("key.smoothCamera", 0, "key.categories.misc");
    public KeyBinding keyBindFullscreen = new KeyBinding("key.fullscreen", 87, "key.categories.misc");
    public KeyBinding keyBindSpectatorOutlines = new KeyBinding("key.spectatorOutlines", 0, "key.categories.misc");
    public KeyBinding[] keyBindsHotbar = new KeyBinding[]{new KeyBinding("key.hotbar.1", 2, "key.categories.inventory"), new KeyBinding("key.hotbar.2", 3, "key.categories.inventory"), new KeyBinding("key.hotbar.3", 4, "key.categories.inventory"), new KeyBinding("key.hotbar.4", 5, "key.categories.inventory"), new KeyBinding("key.hotbar.5", 6, "key.categories.inventory"), new KeyBinding("key.hotbar.6", 7, "key.categories.inventory"), new KeyBinding("key.hotbar.7", 8, "key.categories.inventory"), new KeyBinding("key.hotbar.8", 9, "key.categories.inventory"), new KeyBinding("key.hotbar.9", 10, "key.categories.inventory")};
    public KeyBinding[] keyBindings;
    protected Minecraft mc;
    private File optionsFile;
    public EnumDifficulty difficulty;
    public boolean hideGUI;
    public int thirdPersonView;

    /**
     * true if debug info should be displayed instead of version
     */
    public boolean showDebugInfo;
    public boolean showDebugProfilerChart;
    public boolean showLagometer;

    /**
     * The lastServer string.
     */
    public String lastServer;

    /**
     * Smooth Camera Toggle
     */
    public boolean smoothCamera;
    public boolean debugCamEnable;
    public float fovSetting;
    public float gammaSetting;
    public float saturation;

    /**
     * GUI scale
     */
    public int guiScale;

    /**
     * Determines amount of particles. 0 = All, 1 = Decreased, 2 = Minimal
     */
    public int particleSetting;

    /**
     * Game settings language
     */
    public String language;
    public boolean forceUnicodeFont;

    public GameSettings(Minecraft mcIn, File optionsFileIn) {
        this.keyBindings = (KeyBinding[]) ArrayUtils.addAll(new KeyBinding[]{this.keyBindAttack, this.keyBindUseItem, this.keyBindForward, this.keyBindLeft, this.keyBindBack, this.keyBindRight, this.keyBindJump, this.keyBindSneak, this.keyBindSprint, this.keyBindDrop, this.keyBindInventory, this.keyBindChat, this.keyBindPlayerList, this.keyBindPickBlock, this.keyBindCommand, this.keyBindScreenshot, this.keyBindTogglePerspective, this.keyBindSmoothCamera, this.keyBindFullscreen, this.keyBindSpectatorOutlines}, this.keyBindsHotbar);
        this.difficulty = EnumDifficulty.NORMAL;
        this.lastServer = "";
        this.fovSetting = 70.0F;
        this.language = "en_US";
        this.forceUnicodeFont = false;
        this.mc = mcIn;
        this.optionsFile = new File(optionsFileIn, "options.txt");

        if (mcIn.isJava64bit() && Runtime.getRuntime().maxMemory() >= 1000000000L) {
            GameSettings.Options.RENDER_DISTANCE.setValueMax(32.0F);
        } else {
            GameSettings.Options.RENDER_DISTANCE.setValueMax(16.0F);
        }

        this.renderDistanceChunks = mcIn.isJava64bit() ? 12 : 8;
        this.loadOptions();
    }

    public GameSettings() {
        this.keyBindings = (KeyBinding[]) ArrayUtils.addAll(new KeyBinding[]{this.keyBindAttack, this.keyBindUseItem, this.keyBindForward, this.keyBindLeft, this.keyBindBack, this.keyBindRight, this.keyBindJump, this.keyBindSneak, this.keyBindSprint, this.keyBindDrop, this.keyBindInventory, this.keyBindChat, this.keyBindPlayerList, this.keyBindPickBlock, this.keyBindCommand, this.keyBindScreenshot, this.keyBindTogglePerspective, this.keyBindSmoothCamera, this.keyBindFullscreen, this.keyBindSpectatorOutlines}, this.keyBindsHotbar);
        this.difficulty = EnumDifficulty.NORMAL;
        this.lastServer = "";
        this.fovSetting = 70.0F;
        this.language = "en_US";
        this.forceUnicodeFont = false;
    }

    /**
     * Represents a key or mouse button as a string. Args: key
     *
     * @param key The key to display
     */
    public static String getKeyDisplayString(int key) {
        return key < 0 ? I18n.format("key.mouseButton", new Object[]{Integer.valueOf(key + 101)}) : (key < 256 ? Keyboard.getKeyName(key) : String.format("%c", new Object[]{Character.valueOf((char) (key - 256))}).toUpperCase());
    }

    /**
     * Returns whether the specified key binding is currently being pressed.
     *
     * @param key The key tested
     */
    public static boolean isKeyDown(KeyBinding key) {
        return key.getKeyCode() == 0 ? false : (key.getKeyCode() < 0 ? Mouse.isButtonDown(key.getKeyCode() + 100) : Keyboard.isKeyDown(key.getKeyCode()));
    }

    /**
     * Sets a key binding and then saves all settings.
     *
     * @param key     The key that the option will be set
     * @param keyCode The option (keycode) to set.
     */
    public void setOptionKeyBinding(KeyBinding key, int keyCode) {
        key.setKeyCode(keyCode);
        this.saveOptions();
    }

    /**
     * If the specified option is controlled by a slider (float value), this will set the float value.
     *
     * @param settingsOption The option to set to a value
     * @param value          The value that the option will take
     */
    public void setOptionFloatValue(GameSettings.Options settingsOption, float value) {
        if (settingsOption == GameSettings.Options.SENSITIVITY) {
            this.mouseSensitivity = value;
        }

        if (settingsOption == GameSettings.Options.FOV) {
            this.fovSetting = value;
        }

        if (settingsOption == GameSettings.Options.GAMMA) {
            this.gammaSetting = value;
        }

        if (settingsOption == GameSettings.Options.FRAMERATE_LIMIT) {
            this.limitFramerate = (int) value;
        }

        if (settingsOption == GameSettings.Options.CHAT_OPACITY) {
            this.chatOpacity = value;
            this.mc.ingameGUI.getChatGUI().refreshChat();
        }

        if (settingsOption == GameSettings.Options.CHAT_HEIGHT_FOCUSED) {
            this.chatHeightFocused = value;
            this.mc.ingameGUI.getChatGUI().refreshChat();
        }

        if (settingsOption == GameSettings.Options.CHAT_HEIGHT_UNFOCUSED) {
            this.chatHeightUnfocused = value;
            this.mc.ingameGUI.getChatGUI().refreshChat();
        }

        if (settingsOption == GameSettings.Options.CHAT_WIDTH) {
            this.chatWidth = value;
            this.mc.ingameGUI.getChatGUI().refreshChat();
        }

        if (settingsOption == GameSettings.Options.CHAT_SCALE) {
            this.chatScale = value;
            this.mc.ingameGUI.getChatGUI().refreshChat();
        }

        if (settingsOption == GameSettings.Options.MIPMAP_LEVELS) {
            int i = this.mipmapLevels;
            this.mipmapLevels = (int) value;

            if ((float) i != value) {
                this.mc.getTextureMapBlocks().setMipmapLevels(this.mipmapLevels);
                this.mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
                this.mc.getTextureMapBlocks().setBlurMipmapDirect(false, this.mipmapLevels > 0);
                this.mc.scheduleResourcesRefresh();
            }
        }

        if (settingsOption == GameSettings.Options.BLOCK_ALTERNATIVES) {
            this.allowBlockAlternatives = !this.allowBlockAlternatives;
            this.mc.renderGlobal.loadRenderers();
        }

        if (settingsOption == GameSettings.Options.RENDER_DISTANCE) {
            this.renderDistanceChunks = (int) value;
            this.mc.renderGlobal.setDisplayListEntitiesDirty();
        }
    }

    /**
     * For non-float options. Toggles the option on/off, or cycles through the list i.e. render distances.
     *
     * @param settingsOption The option to set to a value
     * @param value          The value that the option will take
     */
    public void setOptionValue(GameSettings.Options settingsOption, int value) {
        if (settingsOption == GameSettings.Options.INVERT_MOUSE) {
            this.invertMouse = !this.invertMouse;
        }

        if (settingsOption == GameSettings.Options.GUI_SCALE) {
            this.guiScale = this.guiScale + value & 3;
        }

        if (settingsOption == GameSettings.Options.PARTICLES) {
            this.particleSetting = (this.particleSetting + value) % 3;
        }

        if (settingsOption == GameSettings.Options.VIEW_BOBBING) {
            this.viewBobbing = !this.viewBobbing;
        }

        if (settingsOption == GameSettings.Options.RENDER_CLOUDS) {
            this.clouds = (this.clouds + value) % 3;
        }

        if (settingsOption == GameSettings.Options.FORCE_UNICODE_FONT) {
            this.forceUnicodeFont = !this.forceUnicodeFont;
            this.mc.fontRendererObj.setUnicodeFlag(this.mc.getLanguageManager().isCurrentLocaleUnicode() || this.forceUnicodeFont);
        }

        if (settingsOption == GameSettings.Options.FBO_ENABLE) {
            this.fboEnable = !this.fboEnable;
        }

        if (settingsOption == GameSettings.Options.ANAGLYPH) {
            this.anaglyph = !this.anaglyph;
            this.mc.refreshResources();
        }

        if (settingsOption == GameSettings.Options.GRAPHICS) {
            this.fancyGraphics = !this.fancyGraphics;
            this.mc.renderGlobal.loadRenderers();
        }

        if (settingsOption == GameSettings.Options.AMBIENT_OCCLUSION) {
            this.ambientOcclusion = (this.ambientOcclusion + value) % 3;
            this.mc.renderGlobal.loadRenderers();
        }

        if (settingsOption == GameSettings.Options.CHAT_VISIBILITY) {
            this.chatVisibility = EntityPlayer.EnumChatVisibility.getEnumChatVisibility((this.chatVisibility.getChatVisibility() + value) % 3);
        }

        if (settingsOption == GameSettings.Options.CHAT_COLOR) {
            this.chatColours = !this.chatColours;
        }

        if (settingsOption == GameSettings.Options.CHAT_LINKS) {
            this.chatLinks = !this.chatLinks;
        }

        if (settingsOption == GameSettings.Options.CHAT_LINKS_PROMPT) {
            this.chatLinksPrompt = !this.chatLinksPrompt;
        }

        if (settingsOption == GameSettings.Options.SNOOPER_ENABLED) {
            this.snooperEnabled = !this.snooperEnabled;
        }

        if (settingsOption == GameSettings.Options.TOUCHSCREEN) {
            this.touchscreen = !this.touchscreen;
        }

        if (settingsOption == GameSettings.Options.USE_FULLSCREEN) {
            this.fullScreen = !this.fullScreen;

            if (this.mc.isFullScreen() != this.fullScreen) {
                this.mc.toggleFullscreen();
            }
        }

        if (settingsOption == GameSettings.Options.ENABLE_VSYNC) {
            this.enableVsync = !this.enableVsync;
            Display.setVSyncEnabled(this.enableVsync);
        }

        if (settingsOption == GameSettings.Options.USE_VBO) {
            this.useVbo = !this.useVbo;
            this.mc.renderGlobal.loadRenderers();
        }

        if (settingsOption == GameSettings.Options.BLOCK_ALTERNATIVES) {
            this.allowBlockAlternatives = !this.allowBlockAlternatives;
            this.mc.renderGlobal.loadRenderers();
        }

        if (settingsOption == GameSettings.Options.REDUCED_DEBUG_INFO) {
            this.reducedDebugInfo = !this.reducedDebugInfo;
        }

        if (settingsOption == GameSettings.Options.ENTITY_SHADOWS) {
            this.entityShadows = !this.entityShadows;
        }

        this.saveOptions();
    }

    public float getOptionFloatValue(GameSettings.Options settingOption) {
        switch (settingOption) {
            case FOV:
                return this.fovSetting;
            case GAMMA:
                return this.gammaSetting;
            case SATURATION:
                return this.saturation;
            case SENSITIVITY:
                return this.mouseSensitivity;
            case CHAT_OPACITY:
                return this.chatOpacity;
            case CHAT_HEIGHT_FOCUSED:
                return this.chatHeightFocused;
            case CHAT_HEIGHT_UNFOCUSED:
                return this.chatHeightUnfocused;
            case CHAT_SCALE:
                return this.chatScale;
            case CHAT_WIDTH:
                return this.chatWidth;
            case FRAMERATE_LIMIT:
                return (float) this.limitFramerate;
            case MIPMAP_LEVELS:
                return (float) this.mipmapLevels;
            case RENDER_DISTANCE:
                return (float) this.renderDistanceChunks;
            default:
                return 0.0f;
        }
    }

    public boolean getOptionOrdinalValue(GameSettings.Options settingOption) {
        switch (settingOption) {
            case INVERT_MOUSE:
                return this.invertMouse;

            case VIEW_BOBBING:
                return this.viewBobbing;

            case ANAGLYPH:
                return this.anaglyph;

            case FBO_ENABLE:
                return this.fboEnable;

            case CHAT_COLOR:
                return this.chatColours;

            case CHAT_LINKS:
                return this.chatLinks;

            case CHAT_LINKS_PROMPT:
                return this.chatLinksPrompt;

            case SNOOPER_ENABLED:
                return this.snooperEnabled;

            case USE_FULLSCREEN:
                return this.fullScreen;

            case ENABLE_VSYNC:
                return this.enableVsync;

            case USE_VBO:
                return this.useVbo;

            case TOUCHSCREEN:
                return this.touchscreen;

            case FORCE_UNICODE_FONT:
                return this.forceUnicodeFont;

            case BLOCK_ALTERNATIVES:
                return this.allowBlockAlternatives;

            case REDUCED_DEBUG_INFO:
                return this.reducedDebugInfo;

            case ENTITY_SHADOWS:
                return this.entityShadows;

            default:
                return false;
        }
    }

    /**
     * Returns the translation of the given index in the given String array. If the index is smaller than 0 or greater
     * than/equal to the length of the String array, it is changed to 0.
     *
     * @param strArray The array of string containing the string to translate
     * @param index    The index in the array of the string to translate
     */
    private static String getTranslation(String[] strArray, int index) {
        if (index < 0 || index >= strArray.length) {
            index = 0;
        }

        return I18n.format(strArray[index], new Object[0]);
    }

    /**
     * Gets a key binding.
     *
     * @param settingOption The KeyBinding is generated from this option
     */
    public String getKeyBinding(GameSettings.Options settingOption) {
        String s = I18n.format(settingOption.getEnumString(), new Object[0]) + ": ";

        if (settingOption.getEnumFloat()) {
            float f1 = this.getOptionFloatValue(settingOption);
            float f = settingOption.normalizeValue(f1);
            return settingOption == GameSettings.Options.SENSITIVITY
                    ? (f == 0.0F ? s + I18n.format("options.sensitivity.min", new Object[0])
                    : (f == 1.0F ? s + I18n.format("options.sensitivity.max", new Object[0])
                    : s + (int) (f * 200.0F) + "%"))
                    : (settingOption == GameSettings.Options.FOV
                    ? (f1 == 70.0F ? s + I18n.format("options.fov.min", new Object[0])
                    : (f1 == 110.0F ? s + I18n.format("options.fov.max", new Object[0])
                    : s + (int) f1)) : (settingOption == GameSettings.Options.FRAMERATE_LIMIT
                    ? (f1 == settingOption.valueMax ? s + I18n.format("options.framerateLimit.max", new Object[0])
                    : s + (int) f1 + " fps")
                    : (settingOption == GameSettings.Options.RENDER_CLOUDS
                    ? (f1 == settingOption.valueMin
                    ? s + I18n.format("options.cloudHeight.min", new Object[0])
                    : s + ((int) f1 + 128))
                    : (settingOption == GameSettings.Options.GAMMA
                    ? (f == 0.0F ? s + I18n.format("options.gamma.min", new Object[0])
                    : (f == 1.0F ? s + I18n.format("options.gamma.max", new Object[0])
                    : s + "+" + (int) (f * 100.0F) + "%"))
                    : (settingOption == GameSettings.Options.SATURATION ? s + (int) (f * 400.0F) + "%"
                    : (settingOption == GameSettings.Options.CHAT_OPACITY ? s + (int) (f * 90.0F + 10.0F) + "%"
                    : (settingOption == GameSettings.Options.CHAT_HEIGHT_UNFOCUSED
                    ? s + GuiNewChat.calculateChatboxHeight(f) + "px"
                    : (settingOption == GameSettings.Options.CHAT_HEIGHT_FOCUSED
                    ? s + GuiNewChat.calculateChatboxHeight(f) + "px"
                    : (settingOption == GameSettings.Options.CHAT_WIDTH
                    ? s + GuiNewChat.calculateChatboxWidth(f) + "px"
                    : (settingOption == GameSettings.Options.RENDER_DISTANCE
                    ? s + (int) f1 + " chunks"
                    : (settingOption == GameSettings.Options.MIPMAP_LEVELS
                    ? (f1 == 0.0F ? s + I18n.format("options.off", new Object[0])
                    : s + (int) f1)
                    : (f == 0.0F ? s + I18n.format("options.off")
                    : s + (int) (f * 100.0F) + "%"))))))))))));
        } else if (settingOption.getEnumBoolean()) {
            boolean flag = this.getOptionOrdinalValue(settingOption);
            return flag ? s + I18n.format("options.on", new Object[0]) : s + I18n.format("options.off", new Object[0]);
        } else if (settingOption == GameSettings.Options.GUI_SCALE) {
            return s + getTranslation(GUISCALES, this.guiScale);
        } else if (settingOption == GameSettings.Options.CHAT_VISIBILITY) {
            return s + I18n.format(this.chatVisibility.getResourceKey(), new Object[0]);
        } else if (settingOption == GameSettings.Options.PARTICLES) {
            return s + getTranslation(PARTICLES, this.particleSetting);
        } else if (settingOption == GameSettings.Options.AMBIENT_OCCLUSION) {
            return s + getTranslation(AMBIENT_OCCLUSIONS, this.ambientOcclusion);
        } else if (settingOption == GameSettings.Options.RENDER_CLOUDS) {
            return s + getTranslation(CLOUDS_TYPES, this.clouds);
        } else if (settingOption == GameSettings.Options.GRAPHICS) {
            if (this.fancyGraphics) {
                return s + I18n.format("options.graphics.fancy", new Object[0]);
            } else {
                String s1 = "options.graphics.fast";
                return s + I18n.format("options.graphics.fast", new Object[0]);
            }
        } else {
            return s;
        }
    }

    /**
     * Loads the options from the options file. It appears that this has replaced the previous 'loadOptions'
     */
    public void loadOptions() {
        try {
            if (!this.optionsFile.exists()) {
                return;
            }

            BufferedReader bufferedreader = new BufferedReader(new FileReader(this.optionsFile));
            String s = "";
            this.mapSoundLevels.clear();

            while ((s = bufferedreader.readLine()) != null) {
                try {
                    String[] astring = s.split(":");

                    if (astring[0].equals("mouseSensitivity")) {
                        this.mouseSensitivity = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("fov")) {
                        this.fovSetting = this.parseFloat(astring[1]) * 40.0F + 70.0F;
                    }

                    if (astring[0].equals("gamma")) {
                        this.gammaSetting = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("saturation")) {
                        this.saturation = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("invertYMouse")) {
                        this.invertMouse = astring[1].equals("true");
                    }

                    if (astring[0].equals("renderDistance")) {
                        this.renderDistanceChunks = Integer.parseInt(astring[1]);
                    }

                    if (astring[0].equals("guiScale")) {
                        this.guiScale = Integer.parseInt(astring[1]);
                    }

                    if (astring[0].equals("particles")) {
                        this.particleSetting = Integer.parseInt(astring[1]);
                    }

                    if (astring[0].equals("bobView")) {
                        this.viewBobbing = astring[1].equals("true");
                    }

                    if (astring[0].equals("anaglyph3d")) {
                        this.anaglyph = astring[1].equals("true");
                    }

                    if (astring[0].equals("maxFps")) {
                        this.limitFramerate = Integer.parseInt(astring[1]);
                    }

                    if (astring[0].equals("fboEnable")) {
                        this.fboEnable = astring[1].equals("true");
                    }

                    if (astring[0].equals("difficulty")) {
                        this.difficulty = EnumDifficulty.getDifficultyEnum(Integer.parseInt(astring[1]));
                    }

                    if (astring[0].equals("fancyGraphics")) {
                        this.fancyGraphics = astring[1].equals("true");
                    }

                    if (astring[0].equals("ao")) {
                        if (astring[1].equals("true")) {
                            this.ambientOcclusion = 2;
                        } else if (astring[1].equals("false")) {
                            this.ambientOcclusion = 0;
                        } else {
                            this.ambientOcclusion = Integer.parseInt(astring[1]);
                        }
                    }

                    if (astring[0].equals("renderClouds")) {
                        if (astring[1].equals("true")) {
                            this.clouds = 2;
                        } else if (astring[1].equals("false")) {
                            this.clouds = 0;
                        } else if (astring[1].equals("fast")) {
                            this.clouds = 1;
                        }
                    }

                    if (astring[0].equals("resourcePacks")) {
                        this.resourcePacks = (List) gson.fromJson((String) s.substring(s.indexOf(58) + 1), typeListString);

                        if (this.resourcePacks == null) {
                            this.resourcePacks = Lists.<String>newArrayList();
                        }
                    }

                    if (astring[0].equals("incompatibleResourcePacks")) {
                        this.incompatibleResourcePacks = (List) gson.fromJson((String) s.substring(s.indexOf(58) + 1), typeListString);

                        if (this.incompatibleResourcePacks == null) {
                            this.incompatibleResourcePacks = Lists.<String>newArrayList();
                        }
                    }

                    if (astring[0].equals("lastServer") && astring.length >= 2) {
                        this.lastServer = s.substring(s.indexOf(58) + 1);
                    }

                    if (astring[0].equals("lang") && astring.length >= 2) {
                        this.language = astring[1];
                    }

                    if (astring[0].equals("chatVisibility")) {
                        this.chatVisibility = EntityPlayer.EnumChatVisibility.getEnumChatVisibility(Integer.parseInt(astring[1]));
                    }

                    if (astring[0].equals("chatColors")) {
                        this.chatColours = astring[1].equals("true");
                    }

                    if (astring[0].equals("chatLinks")) {
                        this.chatLinks = astring[1].equals("true");
                    }

                    if (astring[0].equals("chatLinksPrompt")) {
                        this.chatLinksPrompt = astring[1].equals("true");
                    }

                    if (astring[0].equals("chatOpacity")) {
                        this.chatOpacity = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("snooperEnabled")) {
                        this.snooperEnabled = astring[1].equals("true");
                    }

                    if (astring[0].equals("fullscreen")) {
                        this.fullScreen = astring[1].equals("true");
                    }

                    if (astring[0].equals("enableVsync")) {
                        this.enableVsync = astring[1].equals("true");
                    }

                    if (astring[0].equals("useVbo")) {
                        this.useVbo = astring[1].equals("true");
                    }

                    if (astring[0].equals("hideServerAddress")) {
                        this.hideServerAddress = astring[1].equals("true");
                    }

                    if (astring[0].equals("advancedItemTooltips")) {
                        this.advancedItemTooltips = astring[1].equals("true");
                    }

                    if (astring[0].equals("pauseOnLostFocus")) {
                        this.pauseOnLostFocus = astring[1].equals("true");
                    }

                    if (astring[0].equals("touchscreen")) {
                        this.touchscreen = astring[1].equals("true");
                    }

                    if (astring[0].equals("overrideHeight")) {
                        this.overrideHeight = Integer.parseInt(astring[1]);
                    }

                    if (astring[0].equals("overrideWidth")) {
                        this.overrideWidth = Integer.parseInt(astring[1]);
                    }

                    if (astring[0].equals("heldItemTooltips")) {
                        this.heldItemTooltips = astring[1].equals("true");
                    }

                    if (astring[0].equals("chatHeightFocused")) {
                        this.chatHeightFocused = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("chatHeightUnfocused")) {
                        this.chatHeightUnfocused = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("chatScale")) {
                        this.chatScale = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("chatWidth")) {
                        this.chatWidth = this.parseFloat(astring[1]);
                    }

                    if (astring[0].equals("showInventoryAchievementHint")) {
                        this.showInventoryAchievementHint = astring[1].equals("true");
                    }

                    if (astring[0].equals("mipmapLevels")) {
                        this.mipmapLevels = Integer.parseInt(astring[1]);
                    }

                    if (astring[0].equals("forceUnicodeFont")) {
                        this.forceUnicodeFont = astring[1].equals("true");
                    }

                    if (astring[0].equals("allowBlockAlternatives")) {
                        this.allowBlockAlternatives = astring[1].equals("true");
                    }

                    if (astring[0].equals("reducedDebugInfo")) {
                        this.reducedDebugInfo = astring[1].equals("true");
                    }

                    if (astring[0].equals("useNativeTransport")) {
                        this.useNativeTransport = astring[1].equals("true");
                    }

                    if (astring[0].equals("entityShadows")) {
                        this.entityShadows = astring[1].equals("true");
                    }

                    for (KeyBinding keybinding : this.keyBindings) {
                        if (astring[0].equals("key_" + keybinding.getKeyDescription())) {
                            keybinding.setKeyCode(Integer.parseInt(astring[1]));
                        }
                    }

                    for (SoundCategory soundcategory : SoundCategory.values()) {
                        if (astring[0].equals("soundCategory_" + soundcategory.getCategoryName())) {
                            this.mapSoundLevels.put(soundcategory, Float.valueOf(this.parseFloat(astring[1])));
                        }
                    }

                    for (EnumPlayerModelParts enumplayermodelparts : EnumPlayerModelParts.values()) {
                        if (astring[0].equals("modelPart_" + enumplayermodelparts.getPartName())) {
                            this.setModelPartEnabled(enumplayermodelparts, astring[1].equals("true"));
                        }
                    }
                } catch (Exception var8) {
                    logger.warn("Skipping bad option: " + s);
                }
            }

            KeyBinding.resetKeyBindingArrayAndHash();
            bufferedreader.close();
        } catch (Exception exception) {
            logger.error((String) "Failed to load options", (Throwable) exception);
        }
    }

    /**
     * Parses a string into a float.
     *
     * @param str The string to parse
     */
    private float parseFloat(String str) {
        return str.equals("true") ? 1.0F : (str.equals("false") ? 0.0F : Float.parseFloat(str));
    }

    /**
     * Saves the options to the options file.
     */
    public void saveOptions() {
        try {
            PrintWriter printwriter = new PrintWriter(new FileWriter(this.optionsFile));
            printwriter.println("invertYMouse:" + this.invertMouse);
            printwriter.println("mouseSensitivity:" + this.mouseSensitivity);
            printwriter.println("fov:" + (this.fovSetting - 70.0F) / 40.0F);
            printwriter.println("gamma:" + this.gammaSetting);
            printwriter.println("saturation:" + this.saturation);
            printwriter.println("renderDistance:" + this.renderDistanceChunks);
            printwriter.println("guiScale:" + this.guiScale);
            printwriter.println("particles:" + this.particleSetting);
            printwriter.println("bobView:" + this.viewBobbing);
            printwriter.println("anaglyph3d:" + this.anaglyph);
            printwriter.println("maxFps:" + this.limitFramerate);
            printwriter.println("fboEnable:" + this.fboEnable);
            printwriter.println("difficulty:" + this.difficulty.getDifficultyId());
            printwriter.println("fancyGraphics:" + this.fancyGraphics);
            printwriter.println("ao:" + this.ambientOcclusion);

            switch (this.clouds) {
                case 0:
                    printwriter.println("renderClouds:false");
                    break;

                case 1:
                    printwriter.println("renderClouds:fast");
                    break;

                case 2:
                    printwriter.println("renderClouds:true");
            }

            printwriter.println("resourcePacks:" + gson.toJson((Object) this.resourcePacks));
            printwriter.println("incompatibleResourcePacks:" + gson.toJson((Object) this.incompatibleResourcePacks));
            printwriter.println("lastServer:" + this.lastServer);
            printwriter.println("lang:" + this.language);
            printwriter.println("chatVisibility:" + this.chatVisibility.getChatVisibility());
            printwriter.println("chatColors:" + this.chatColours);
            printwriter.println("chatLinks:" + this.chatLinks);
            printwriter.println("chatLinksPrompt:" + this.chatLinksPrompt);
            printwriter.println("chatOpacity:" + this.chatOpacity);
            printwriter.println("snooperEnabled:" + this.snooperEnabled);
            printwriter.println("fullscreen:" + this.fullScreen);
            printwriter.println("enableVsync:" + this.enableVsync);
            printwriter.println("useVbo:" + this.useVbo);
            printwriter.println("hideServerAddress:" + this.hideServerAddress);
            printwriter.println("advancedItemTooltips:" + this.advancedItemTooltips);
            printwriter.println("pauseOnLostFocus:" + this.pauseOnLostFocus);
            printwriter.println("touchscreen:" + this.touchscreen);
            printwriter.println("overrideWidth:" + this.overrideWidth);
            printwriter.println("overrideHeight:" + this.overrideHeight);
            printwriter.println("heldItemTooltips:" + this.heldItemTooltips);
            printwriter.println("chatHeightFocused:" + this.chatHeightFocused);
            printwriter.println("chatHeightUnfocused:" + this.chatHeightUnfocused);
            printwriter.println("chatScale:" + this.chatScale);
            printwriter.println("chatWidth:" + this.chatWidth);
            printwriter.println("showInventoryAchievementHint:" + this.showInventoryAchievementHint);
            printwriter.println("mipmapLevels:" + this.mipmapLevels);
            printwriter.println("forceUnicodeFont:" + this.forceUnicodeFont);
            printwriter.println("allowBlockAlternatives:" + this.allowBlockAlternatives);
            printwriter.println("reducedDebugInfo:" + this.reducedDebugInfo);
            printwriter.println("useNativeTransport:" + this.useNativeTransport);
            printwriter.println("entityShadows:" + this.entityShadows);

            for (KeyBinding keybinding : this.keyBindings) {
                printwriter.println("key_" + keybinding.getKeyDescription() + ":" + keybinding.getKeyCode());
            }

            for (SoundCategory soundcategory : SoundCategory.values()) {
                printwriter.println("soundCategory_" + soundcategory.getCategoryName() + ":" + this.getSoundLevel(soundcategory));
            }

            for (EnumPlayerModelParts enumplayermodelparts : EnumPlayerModelParts.values()) {
                printwriter.println("modelPart_" + enumplayermodelparts.getPartName() + ":" + this.setModelParts.contains(enumplayermodelparts));
            }

            printwriter.close();
        } catch (Exception exception) {
            logger.error((String) "Failed to save options", (Throwable) exception);
        }

        this.sendSettingsToServer();
    }

    public float getSoundLevel(SoundCategory sndCategory) {
        return this.mapSoundLevels.containsKey(sndCategory) ? ((Float) this.mapSoundLevels.get(sndCategory)).floatValue() : 1.0F;
    }

    public void setSoundLevel(SoundCategory sndCategory, float soundLevel) {
        this.mc.getSoundHandler().setSoundLevel(sndCategory, soundLevel);
        this.mapSoundLevels.put(sndCategory, Float.valueOf(soundLevel));
    }

    /**
     * Send a client info packet with settings information to the server
     */
    public void sendSettingsToServer() {
        if (this.mc.thePlayer != null) {
            int i = 0;

            for (EnumPlayerModelParts enumplayermodelparts : this.setModelParts) {
                i |= enumplayermodelparts.getPartMask();
            }

            this.mc.thePlayer.sendQueue.addToSendQueue(new C15PacketClientSettings(this.language, this.renderDistanceChunks, this.chatVisibility, this.chatColours, i));
        }
    }

    public Set<EnumPlayerModelParts> getModelParts() {
        return ImmutableSet.copyOf(this.setModelParts);
    }

    public void setModelPartEnabled(EnumPlayerModelParts modelPart, boolean enable) {
        if (enable) {
            this.setModelParts.add(modelPart);
        } else {
            this.setModelParts.remove(modelPart);
        }

        this.sendSettingsToServer();
    }

    public void switchModelPartEnabled(EnumPlayerModelParts modelPart) {
        if (!this.getModelParts().contains(modelPart)) {
            this.setModelParts.add(modelPart);
        } else {
            this.setModelParts.remove(modelPart);
        }

        this.sendSettingsToServer();
    }

    /**
     * Return true if the clouds should be rendered
     */
    public int shouldRenderClouds() {
        return this.renderDistanceChunks >= 4 ? this.clouds : 0;
    }

    /**
     * Return true if the client connect to a server using the native transport system
     */
    public boolean isUsingNativeTransport() {
        return this.useNativeTransport;
    }

    public static enum Options {
        INVERT_MOUSE("options.invertMouse", false, true),
        SENSITIVITY("options.sensitivity", true, false),
        FOV("options.fov", true, false, 30.0F, 110.0F, 1.0F),
        GAMMA("options.gamma", true, false),
        SATURATION("options.saturation", true, false),
        RENDER_DISTANCE("options.renderDistance", true, false, 2.0F, 16.0F, 1.0F),
        VIEW_BOBBING("options.viewBobbing", false, true),
        ANAGLYPH("options.anaglyph", false, true),
        FRAMERATE_LIMIT("options.framerateLimit", true, false, 10.0F, 260.0F, 10.0F),
        FBO_ENABLE("options.fboEnable", false, true),
        RENDER_CLOUDS("options.renderClouds", false, false),
        GRAPHICS("options.graphics", false, false),
        AMBIENT_OCCLUSION("options.ao", false, false),
        GUI_SCALE("options.guiScale", false, false),
        PARTICLES("options.particles", false, false),
        CHAT_VISIBILITY("options.chat.visibility", false, false),
        CHAT_COLOR("options.chat.color", false, true),
        CHAT_LINKS("options.chat.links", false, true),
        CHAT_OPACITY("options.chat.opacity", true, false),
        CHAT_LINKS_PROMPT("options.chat.links.prompt", false, true),
        SNOOPER_ENABLED("options.snooper", false, true),
        USE_FULLSCREEN("options.fullscreen", false, true),
        ENABLE_VSYNC("options.vsync", false, true),
        USE_VBO("options.vbo", false, true),
        TOUCHSCREEN("options.touchscreen", false, true),
        CHAT_SCALE("options.chat.scale", true, false),
        CHAT_WIDTH("options.chat.width", true, false),
        CHAT_HEIGHT_FOCUSED("options.chat.height.focused", true, false),
        CHAT_HEIGHT_UNFOCUSED("options.chat.height.unfocused", true, false),
        MIPMAP_LEVELS("options.mipmapLevels", true, false, 0.0F, 4.0F, 1.0F),
        FORCE_UNICODE_FONT("options.forceUnicodeFont", false, true),
        BLOCK_ALTERNATIVES("options.blockAlternatives", false, true),
        REDUCED_DEBUG_INFO("options.reducedDebugInfo", false, true),
        ENTITY_SHADOWS("options.entityShadows", false, true);

        private final boolean enumFloat;
        private final boolean enumBoolean;
        private final String enumString;
        private final float valueStep;
        private float valueMin;
        private float valueMax;

        public static GameSettings.Options getEnumOptions(int ordinal) {
            for (GameSettings.Options gamesettings$options : values()) {
                if (gamesettings$options.returnEnumOrdinal() == ordinal) {
                    return gamesettings$options;
                }
            }

            return null;
        }

        private Options(String str, boolean isFloat, boolean isBoolean) {
            this(str, isFloat, isBoolean, 0.0F, 1.0F, 0.0F);
        }

        private Options(String str, boolean isFloat, boolean isBoolean, float valMin, float valMax, float valStep) {
            this.enumString = str;
            this.enumFloat = isFloat;
            this.enumBoolean = isBoolean;
            this.valueMin = valMin;
            this.valueMax = valMax;
            this.valueStep = valStep;
        }

        public boolean getEnumFloat() {
            return this.enumFloat;
        }

        public boolean getEnumBoolean() {
            return this.enumBoolean;
        }

        public int returnEnumOrdinal() {
            return this.ordinal();
        }

        public String getEnumString() {
            return this.enumString;
        }

        public float getValueMax() {
            return this.valueMax;
        }

        public void setValueMax(float value) {
            this.valueMax = value;
        }

        public float normalizeValue(float value) {
            return MathHelper.clamp_float((this.snapToStepClamp(value) - this.valueMin) / (this.valueMax - this.valueMin), 0.0F, 1.0F);
        }

        public float denormalizeValue(float value) {
            return this.snapToStepClamp(this.valueMin + (this.valueMax - this.valueMin) * MathHelper.clamp_float(value, 0.0F, 1.0F));
        }

        public float snapToStepClamp(float value) {
            value = this.snapToStep(value);
            return MathHelper.clamp_float(value, this.valueMin, this.valueMax);
        }

        protected float snapToStep(float value) {
            if (this.valueStep > 0.0F) {
                value = this.valueStep * (float) Math.round(value / this.valueStep);
            }

            return value;
        }
    }
}
