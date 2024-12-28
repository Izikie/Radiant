package net.minecraft.client.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.src.Config;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.optifine.SmartAnimations;
import net.optifine.TextureAnimations;
import net.optifine.util.MemoryMonitor;
import net.optifine.util.NativeMemory;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class GuiOverlayDebug extends Gui {
    private final Minecraft mc;
    private final FontRenderer fontRenderer;
    private String debugOF = null;
    private List<String> debugInfoLeft = null;
    private List<String> debugInfoRight = null;
    private long updateInfoLeftTimeMs = 0L;
    private long updateInfoRightTimeMs = 0L;

    public GuiOverlayDebug(Minecraft mc) {
        this.mc = mc;
        this.fontRenderer = mc.fontRendererObj;
    }

    public void renderDebugInfo(ScaledResolution scaledResolutionIn) {
        GlStateManager.pushMatrix();
        renderDebugInfoLeft();
        renderDebugInfoRight(scaledResolutionIn);
        GlStateManager.popMatrix();
    }

    private boolean isReducedDebug() {
        return mc.thePlayer.hasReducedDebug() || mc.gameSettings.reducedDebugInfo;
    }

    protected void renderDebugInfoLeft() {
        List<String> list = debugInfoLeft;

        if (list == null || System.currentTimeMillis() > updateInfoLeftTimeMs) {
            list = getDebugInfoLeft();
            debugInfoLeft = list;
            updateInfoLeftTimeMs = System.currentTimeMillis() + 100L;
        }

        for (int i = 0; i < list.size(); ++i) {
            String line = list.get(i);

            if (!Strings.isNullOrEmpty(line)) {
                int fontHeight = fontRenderer.FONT_HEIGHT;
                int yPos = 2 + fontHeight * i;
                fontRenderer.drawStringWithShadow(line, 2, yPos, -1);
            }
        }
    }

    protected void renderDebugInfoRight(ScaledResolution scaledRes) {
        List<String> list = debugInfoRight;

        if (list == null || System.currentTimeMillis() > updateInfoRightTimeMs) {
            list = getDebugInfoRight();
            debugInfoRight = list;
            updateInfoRightTimeMs = System.currentTimeMillis() + 100L;
        }

        for (int i = 0; i < list.size(); ++i) {
            String line = list.get(i);

            if (!Strings.isNullOrEmpty(line)) {
                int lineWidth = fontRenderer.getStringWidth(line);
                int xPos = scaledRes.getScaledWidth() - 2 - lineWidth;
                int yPos = 2 + fontRenderer.FONT_HEIGHT * i;
                fontRenderer.drawStringWithShadow(line, xPos, yPos, -1);
            }
        }
    }

    @SuppressWarnings("incomplete-switch")
    protected List<String> getDebugInfoLeft() {
        BlockPos blockPos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);

        if (mc.debug != debugOF) {
            StringBuffer stringbuffer = new StringBuffer(mc.debug);
            int minFPS = Config.getFpsMin();
            int currentFPS = mc.debug.indexOf(" fps ");

            if (currentFPS >= 0) {
                stringbuffer.insert(currentFPS, "/" + minFPS);
            }

            if (Config.isSmoothFps()) {
                stringbuffer.append(" sf");
            }

            if (Config.isFastRender()) {
                stringbuffer.append(" fr");
            }

            if (Config.isAnisotropicFiltering()) {
                stringbuffer.append(" af");
            }

            if (Config.isAntialiasing()) {
                stringbuffer.append(" aa");
            }

            if (Config.isRenderRegions()) {
                stringbuffer.append(" reg");
            }

            if (Config.isShaders()) {
                stringbuffer.append(" sh");
            }

            mc.debug = stringbuffer.toString();
            debugOF = mc.debug;
        }

        StringBuilder textureAnimBuilder = new StringBuilder();
        TextureMap textureMap = Config.getTextureMap();
        textureAnimBuilder.append(", A: ");

        if (SmartAnimations.isActive()) {
            textureAnimBuilder.append(textureMap.getCountAnimationsActive() + TextureAnimations.getCountAnimationsActive());
            textureAnimBuilder.append("/");
        }

        textureAnimBuilder.append(textureMap.getCountAnimations() + TextureAnimations.getCountAnimations());
        String textureAnimInfo = textureAnimBuilder.toString();

        List<String> debugInfo = Lists.newArrayList(
            String.format("Minecraft 1.8.9 (%s)", mc.getVersion()),
                mc.debug,
                mc.renderGlobal.getDebugInfoRenders(),
                mc.renderGlobal.getDebugInfoEntities(),
                "P: " + mc.effectRenderer.getStatistics() + ". T: " + mc.theWorld.getDebugLoadedEntities() + textureAnimInfo,
                mc.theWorld.getProviderName(),
                "",
                String.format("Chunk-relative: %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15)
        );

        if (isReducedDebug()) {
            debugInfo.add(String.format("Chunk-relative: %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15));
        } else {
            Entity entity = mc.getRenderViewEntity();
            EnumFacing enumFacing = entity.getHorizontalFacing();
            String direction = switch (enumFacing) {
                case NORTH -> "Towards -Z";
                case SOUTH -> "Towards +Z";
                case WEST -> "Towards -X";
                case EAST -> "Towards +X";
                default -> "Invalid";
            };

            debugInfo.add(String.format("XYZ: %.3f, %.3f, %.3f", mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ));
            debugInfo.add(String.format("Block: %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            debugInfo.add(String.format("Chunk: %d %d %d in %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15, blockPos.getX() >> 4, blockPos.getY() >> 4, blockPos.getZ() >> 4));
            debugInfo.add(String.format("Facing: %s (%s) (%.1f / %.1f)", enumFacing.getName(), direction, MathHelper.wrapAngleTo180_float(entity.rotationYaw), MathHelper.wrapAngleTo180_float(entity.rotationPitch)));

            if (mc.theWorld != null && mc.theWorld.isBlockLoaded(blockPos)) {
                Chunk chunk = mc.theWorld.getChunkFromBlockCoords(blockPos);
                debugInfo.add("Biome: " + chunk.getBiome(blockPos, mc.theWorld.getWorldChunkManager()).biomeName);
                debugInfo.add("Light: " + chunk.getLightSubtracted(blockPos, 0) + " (" + chunk.getLightFor(EnumSkyBlock.SKY, blockPos) + " sky, " + chunk.getLightFor(EnumSkyBlock.BLOCK, blockPos) + " block)");
                DifficultyInstance localDifficulty = mc.theWorld.getDifficultyForLocation(blockPos);

                if (mc.isIntegratedServerRunning() && mc.getIntegratedServer() != null) {
                    EntityPlayerMP entityPlayerMP = mc.getIntegratedServer().getConfigurationManager().getPlayerByUUID(mc.thePlayer.getUniqueID());

                    if (entityPlayerMP != null) {
                        DifficultyInstance difficultyAsync = mc.getIntegratedServer().getDifficultyAsync(entityPlayerMP.worldObj, new BlockPos(entityPlayerMP));

                        if (difficultyAsync != null) {
                            localDifficulty = difficultyAsync;
                        }
                    }
                }

                debugInfo.add(String.format("Local Difficulty: %.2f (Day %d)", localDifficulty.getAdditionalDifficulty(), mc.theWorld.getWorldTime() / 24000L));
            }

            if (mc.entityRenderer != null && mc.entityRenderer.isShaderActive()) {
                debugInfo.add("Shader: " + mc.entityRenderer.getShaderGroup().getShaderGroupName());
            }

            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.objectMouseOver.getBlockPos() != null) {
                BlockPos lookPos = mc.objectMouseOver.getBlockPos();
                debugInfo.add(String.format("Looking at: %d %d %d", lookPos.getX(), lookPos.getY(), lookPos.getZ()));
            }
        }

        return debugInfo;
    }

    protected List<String> getDebugInfoRight() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long usedMemory = totalMemory - freeMemory;

        List<String> list = Lists.newArrayList(
                String.format("Java: %s %dbit", System.getProperty("java.version"), mc.isJava64bit() ? 64 : 32),
                String.format("Mem: % 2d%% %03d/%03dMB", usedMemory * 100L / maxMemory, bytesToMb(usedMemory), bytesToMb(maxMemory)),
                String.format("Allocated: % 2d%% %03dMB", totalMemory * 100L / maxMemory, bytesToMb(totalMemory)),
                "",
                String.format("CPU: %s", OpenGlHelper.getCpu()),
                "",
                String.format("Display: %dx%d (%s)", Display.getWidth(), Display.getHeight(), GL11.glGetString(GL11.GL_VENDOR)),
                GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION));

        long bufferAllocated = bytesToMb(NativeMemory.getBufferAllocated());
        long bufferMax = bytesToMb(NativeMemory.getBufferMaximum());
        String nativeMemory = "Native: " + bufferAllocated + "/" + bufferMax + "MB";
        list.add(4, nativeMemory);
        list.set(5, "GC: " + MemoryMonitor.getAllocationRateMb() + "MB/s");

        if (!isReducedDebug()) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.objectMouseOver.getBlockPos() != null) {
                BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                IBlockState iBlockState = mc.theWorld.getBlockState(blockPos);

                if (mc.theWorld.getWorldType() != WorldType.DEBUG_WORLD) {
                    iBlockState = iBlockState.getBlock().getActualState(iBlockState, mc.theWorld, blockPos);
                }

                list.add("");
                list.add(String.valueOf(Block.blockRegistry.getNameForObject(iBlockState.getBlock())));

                for (Entry<IProperty, Comparable> entry : iBlockState.getProperties().entrySet()) {
                    String value = entry.getValue().toString();

                    if (entry.getValue() == Boolean.TRUE) {
                        value = EnumChatFormatting.GREEN + value;
                    } else if (entry.getValue() == Boolean.FALSE) {
                        value = EnumChatFormatting.RED + value;
                    }

                    list.add(entry.getKey().getName() + ": " + value);
                }
            }

        }
        return list;
    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }
}
