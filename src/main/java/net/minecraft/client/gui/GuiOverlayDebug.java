package net.minecraft.client.gui;

import com.google.common.base.Strings;
import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.src.Config;
import net.minecraft.util.*;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.LightType;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import net.optifine.SmartAnimations;
import net.optifine.TextureAnimations;
import net.optifine.util.MemoryMonitor;
import net.radiant.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class GuiOverlayDebug extends Gui {
    private final Minecraft mc;
    private final FontRenderer fontRenderer;
    private String debugOF = null;

    private List<String> debugInfoLeft = null;
    private List<String> debugInfoRight = null;
    private long updateTimeMS = 0L;

    public GuiOverlayDebug(Minecraft mc) {
        this.mc = mc;
        this.fontRenderer = mc.fontRendererObj;
    }

    public void renderDebugInfo(ScaledResolution scaledResolutionIn) {
        GlStateManager.pushMatrix();

        if (debugInfoLeft == null || debugInfoRight == null || System.currentTimeMillis() > updateTimeMS) {
            debugInfoLeft = getDebugInfoLeft();
            debugInfoRight = getDebugInfoRight();
            updateTimeMS = System.currentTimeMillis() + 150L;
        }

        renderDebugInfoLeft();
        renderDebugInfoRight(scaledResolutionIn);
        GlStateManager.popMatrix();
    }

    protected void renderDebugInfoLeft() {
        List<String> list = debugInfoLeft;

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

    private boolean isReducedDebug() {
        return mc.player.hasReducedDebug() || mc.gameSettings.reducedDebugInfo;
    }

    @SuppressWarnings("incomplete-switch")
    protected List<String> getDebugInfoLeft() {
        BlockPos blockPos = new BlockPos(mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ);

        if (mc.debug != debugOF) {
            StringBuilder builder = new StringBuilder(mc.debug);
            int minFPS = Config.getFpsMin();
            int currentFPS = mc.debug.indexOf(" fps ");

            if (currentFPS >= 0) {
                builder.insert(currentFPS, "/" + minFPS);
            }

            if (Config.isSmoothFps()) {
                builder.append(" sf");
            }

            if (Config.isFastRender()) {
                builder.append(" fr");
            }

            if (Config.isAnisotropicFiltering()) {
                builder.append(" af");
            }

            if (Config.isAntialiasing()) {
                builder.append(" aa");
            }

            if (Config.isRenderRegions()) {
                builder.append(" reg");
            }

            if (Config.isShaders()) {
                builder.append(" sh");
            }

            mc.debug = builder.toString();
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

        List<String> debugInfo = new ArrayList<>(List.of(
                String.format("Minecraft 1.8.9 (%s)", mc.getVersion()),
                mc.debug,
                mc.renderGlobal.getDebugInfoRenders(),
                mc.renderGlobal.getDebugInfoEntities(),
                "P: " + mc.effectRenderer.getStatistics() + ". T: " + mc.world.getDebugLoadedEntities() + textureAnimInfo,
                mc.world.getProviderName(),
                ""
        ));

        if (isReducedDebug()) {
            debugInfo.add(String.format("Chunk-relative: %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15));
        } else {
            Entity entity = mc.getRenderViewEntity();
            Direction direction = entity.getHorizontalFacing();
            String facing = switch (direction) {
                case NORTH -> "Towards -Z";
                case SOUTH -> "Towards +Z";
                case WEST -> "Towards -X";
                case EAST -> "Towards +X";
                default -> "Invalid";
            };

            debugInfo.add(String.format("XYZ: %.3f / %.3f / %.3f", mc.getRenderViewEntity().posX, mc.getRenderViewEntity().getEntityBoundingBox().minY, mc.getRenderViewEntity().posZ));
            debugInfo.add(String.format("Block: %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            debugInfo.add(String.format("Chunk: %d %d %d in %d %d %d", blockPos.getX() & 15, blockPos.getY() & 15, blockPos.getZ() & 15, blockPos.getX() >> 4, blockPos.getY() >> 4, blockPos.getZ() >> 4));
            debugInfo.add(String.format("Facing: %s (%s) (%.1f / %.1f)", direction.getName(), facing, MathHelper.wrapAngle(entity.rotationYaw), MathHelper.wrapAngle(entity.rotationPitch)));

            if (mc.world != null && mc.world.isBlockLoaded(blockPos)) {
                Chunk chunk = mc.world.getChunkFromBlockCoords(blockPos);
                debugInfo.add("Biome: " + chunk.getBiome(blockPos, mc.world.getWorldChunkManager()).biomeName);
                debugInfo.add("Light: " + chunk.getLightSubtracted(blockPos, 0) + " (" + chunk.getLightFor(LightType.SKY, blockPos) + " sky, " + chunk.getLightFor(LightType.BLOCK, blockPos) + " block)");
                DifficultyInstance localDifficulty = mc.world.getDifficultyForLocation(blockPos);

                if (mc.isIntegratedServerRunning() && mc.getIntegratedServer() != null) {
                    EntityPlayerMP entityPlayerMP = mc.getIntegratedServer().getConfigurationManager().getPlayerByUUID(mc.player.getUniqueID());

                    if (entityPlayerMP != null) {
                        DifficultyInstance difficultyAsync = mc.getIntegratedServer().getDifficultyAsync(entityPlayerMP.worldObj, new BlockPos(entityPlayerMP));

                        if (difficultyAsync != null) {
                            localDifficulty = difficultyAsync;
                        }
                    }
                }

                debugInfo.add(String.format("Local Difficulty: %.2f (Day %d)", localDifficulty.getAdditionalDifficulty(), mc.world.getWorldTime() / 24000L));
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

        List<String> list = new ArrayList<>(List.of(
                String.format("Java: %s", System.getProperty("java.version")),
                String.format("Mem: % 2d%% %03d/%03dMB", usedMemory * 100L / maxMemory, bytesToMb(usedMemory), bytesToMb(maxMemory)),
                String.format("Allocated: % 2d%% %03dMB", totalMemory * 100L / maxMemory, bytesToMb(totalMemory)),
                String.format("GC: %dMB/s", MemoryMonitor.getAllocationRateMb()),
                "",
                String.format("Display: %dx%d (%s)", Display.getWidth(), Display.getHeight(), GL11.glGetString(GL11.GL_VENDOR)),
                GL11.glGetString(GL11.GL_RENDERER),
                GL11.glGetString(GL11.GL_VERSION)));

        if (!isReducedDebug()) {
            if (mc.objectMouseOver != null && mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && mc.objectMouseOver.getBlockPos() != null) {
                BlockPos blockPos = mc.objectMouseOver.getBlockPos();
                IBlockState blockState = mc.world.getBlockState(blockPos);

                if (mc.world.getWorldType() != WorldType.DEBUG_WORLD) {
                    blockState = blockState.getBlock().getActualState(blockState, mc.world, blockPos);
                }

                list.add("");
                list.add(String.valueOf(Block.blockRegistry.getNameForObject(blockState.getBlock())));

                for (Entry<IProperty<?>, Comparable<?>> entry : blockState.getProperties().entrySet()) {
                    String value = entry.getValue().toString();

                    if (entry.getValue() == Boolean.TRUE) {
                        value = Formatting.GREEN + value;
                    } else if (entry.getValue() == Boolean.FALSE) {
                        value = Formatting.RED + value;
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
