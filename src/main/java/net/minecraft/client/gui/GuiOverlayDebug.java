package net.minecraft.client.gui;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Map.Entry;

import net.minecraft.block.Block;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.ClientBrandRetriever;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.FrameTimer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.EnumSkyBlock;
import net.minecraft.world.WorldType;
import net.minecraft.world.chunk.Chunk;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.GL11;

public class GuiOverlayDebug extends Gui {
    private final Minecraft mc;
    private final FontRenderer fontRenderer;

    public GuiOverlayDebug(Minecraft mc) {
        this.mc = mc;
        this.fontRenderer = mc.fontRendererObj;
    }

    public void renderDebugInfo(ScaledResolution scaledResolutionIn) {
        GlStateManager.pushMatrix();
        this.renderDebugInfoLeft();
        this.renderDebugInfoRight(scaledResolutionIn);
        GlStateManager.popMatrix();

        if (this.mc.gameSettings.showLagometer) {
            this.renderLagometer();
        }
    }

    protected void renderDebugInfoLeft() {
        List<String> list = this.getDebugInfoLeft();
        int fontHeight = this.fontRenderer.FONT_HEIGHT;

        for (int i = 0; i < list.size(); ++i) {
            String line = list.get(i);

            if (!Strings.isNullOrEmpty(line)) {
                int stringWidth = this.fontRenderer.getStringWidth(line);
                int yPos = 2 + fontHeight * i;
                drawRect(1, yPos - 1, 2 + stringWidth + 1, yPos + fontHeight - 1, -1873784752);
                this.fontRenderer.drawString(line, 2, yPos, -1);
            }
        }
    }

    protected void renderDebugInfoRight(ScaledResolution scaledRes) {
        List<String> list = this.getDebugInfoRight();
        int fontHeight = this.fontRenderer.FONT_HEIGHT;

        for (int i = 0; i < list.size(); ++i) {
            String s = list.get(i);

            if (!Strings.isNullOrEmpty(s)) {
                int stringWidth = this.fontRenderer.getStringWidth(s);
                int xPos = scaledRes.getScaledWidth() - 2 - stringWidth;
                int yPos = 2 + fontHeight * i;
                drawRect(xPos - 1, yPos - 1, xPos + stringWidth + 1, yPos + fontHeight - 1, -1873784752);
                this.fontRenderer.drawString(s, xPos, yPos, -1);
            }
        }
    }

    protected List<String> getDebugInfoLeft() {
        BlockPos blockpos = new BlockPos(this.mc.getRenderViewEntity().posX, this.mc.getRenderViewEntity().getEntityBoundingBox().minY, this.mc.getRenderViewEntity().posZ);

        List<String> list = Lists.newArrayList(
                "Minecraft 1.8.9 (" + this.mc.getVersion() + "/" + ClientBrandRetriever.getClientModName() + ")",
                this.mc.debug,
                this.mc.renderGlobal.getDebugInfoRenders(),
                this.mc.renderGlobal.getDebugInfoEntities(),
                "P: " + this.mc.effectRenderer.getStatistics() + ". T: " + this.mc.theWorld.getDebugLoadedEntities(),
                this.mc.theWorld.getProviderName(),
                ""
        );

        if (this.isReducedDebug()) {
            list.add(String.format("Chunk-relative: %d %d %d", blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15));
        } else {
            Entity entity = this.mc.getRenderViewEntity();
            EnumFacing enumFacing = entity.getHorizontalFacing();
            String direction = switch (enumFacing) {
                case NORTH -> "Towards -Z";
                case SOUTH -> "Towards +Z";
                case WEST -> "Towards -X";
                case EAST -> "Towards +X";
                default -> "Invalid";
            };

            list.add(String.format("XYZ: %.3f / %.3f / %.3f", this.mc.getRenderViewEntity().posX,this.mc.getRenderViewEntity().getEntityBoundingBox().minY, this.mc.getRenderViewEntity().posZ));
            list.add(String.format("Block: %d %d %d", blockpos.getX(), blockpos.getY(), blockpos.getZ()));
            list.add(String.format("Chunk: %d %d %d in %d %d %d", blockpos.getX() & 15, blockpos.getY() & 15, blockpos.getZ() & 15, blockpos.getX() >> 4, blockpos.getY() >> 4, blockpos.getZ() >> 4));
            list.add(String.format("Facing: %s (%s) (%.1f / %.1f)", enumFacing, direction, MathHelper.wrapAngleTo180_float(entity.rotationYaw), MathHelper.wrapAngleTo180_float(entity.rotationPitch)));

            if (this.mc.theWorld != null && this.mc.theWorld.isBlockLoaded(blockpos)) {
                Chunk chunk = this.mc.theWorld.getChunkFromBlockCoords(blockpos);
                list.add("Biome: " + chunk.getBiome(blockpos, this.mc.theWorld.getWorldChunkManager()).biomeName);
                list.add("Light: " + chunk.getLightSubtracted(blockpos, 0) + " (" + chunk.getLightFor(EnumSkyBlock.SKY, blockpos) + " sky, " + chunk.getLightFor(EnumSkyBlock.BLOCK, blockpos) + " block)");
                DifficultyInstance difficultyInstance = this.mc.theWorld.getDifficultyForLocation(blockpos);

                if (this.mc.isIntegratedServerRunning() && this.mc.getIntegratedServer() != null) {
                    EntityPlayerMP entityPlayerMP = this.mc.getIntegratedServer().getConfigurationManager().getPlayerByUUID(this.mc.thePlayer.getUniqueID());

                    if (entityPlayerMP != null) {
                        difficultyInstance = entityPlayerMP.worldObj.getDifficultyForLocation(new BlockPos(entityPlayerMP));
                    }
                }

                list.add(String.format("Local Difficulty: %.2f (Day %d)", difficultyInstance.getAdditionalDifficulty(), this.mc.theWorld.getWorldTime() / 24000L));
            }

            if (this.mc.entityRenderer != null && this.mc.entityRenderer.isShaderActive()) {
                list.add("Shader: " + this.mc.entityRenderer.getShaderGroup().getShaderGroupName());
            }

            if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && this.mc.objectMouseOver.getBlockPos() != null) {
                BlockPos blockPos = this.mc.objectMouseOver.getBlockPos();
                list.add(String.format("Looking at: %d %d %d", blockPos.getX(), blockPos.getY(), blockPos.getZ()));
            }

        } return list;
    }

    protected List<String> getDebugInfoRight() {
        long maxMemory = Runtime.getRuntime().maxMemory();
        long totalMemory = Runtime.getRuntime().totalMemory();
        long freeMemory = Runtime.getRuntime().freeMemory();
        long memoryDif = totalMemory - freeMemory;
        List<String> list = Lists.newArrayList(
                String.format("Java: %s %dbit", System.getProperty("java.version"), this.mc.isJava64bit() ? 64 : 32),
                String.format("Mem: % 2d%% %03d/%03dMB", memoryDif * 100L / maxMemory, bytesToMb(memoryDif), bytesToMb(maxMemory)),
                String.format("Allocated: % 2d%% %03dMB", totalMemory * 100L / maxMemory, bytesToMb(totalMemory)),
                "",
                String.format("CPU: %s", OpenGlHelper.getCpu()),
                "",
                String.format("Display: %dx%d (%s)", Display.getWidth(), Display.getHeight(), GL11.glGetString(GL11.GL_VENDOR)),
                GL11.glGetString(GL11.GL_RENDERER), GL11.glGetString(GL11.GL_VERSION));

        if (!this.isReducedDebug()) {
            if (this.mc.objectMouseOver != null && this.mc.objectMouseOver.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && this.mc.objectMouseOver.getBlockPos() != null) {
                BlockPos blockPos = this.mc.objectMouseOver.getBlockPos();
                IBlockState iBlockState = this.mc.theWorld.getBlockState(blockPos);

                if (this.mc.theWorld.getWorldType() != WorldType.DEBUG_WORLD) {
                    iBlockState = iBlockState.getBlock().getActualState(iBlockState, this.mc.theWorld, blockPos);
                }

                list.add("");
                list.add(String.valueOf(Block.blockRegistry.getNameForObject(iBlockState.getBlock())));

                for (Entry<IProperty, Comparable> entry : iBlockState.getProperties().entrySet()) {
                    String string = entry.getValue().toString();

                    if (entry.getValue() == Boolean.TRUE) {
                        string = EnumChatFormatting.GREEN + string;
                    } else if (entry.getValue() == Boolean.FALSE) {
                        string = EnumChatFormatting.RED + string;
                    }

                    list.add(entry.getKey().getName() + ": " + string);
                }
            }

        }
        return list;
    }

    private boolean isReducedDebug() {
        return this.mc.thePlayer.hasReducedDebug() || this.mc.gameSettings.reducedDebugInfo;
    }

    private void renderLagometer() {
        GlStateManager.disableDepth();
        FrameTimer frameTimer = this.mc.getFrameTimer();
        int lastIndex = frameTimer.getLastIndex();
        int index = frameTimer.getIndex();
        long[] frames = frameTimer.getFrames();
        ScaledResolution scaledResolution = new ScaledResolution(this.mc);
        int k = lastIndex;
        int l = 0;
        drawRect(0, scaledResolution.getScaledHeight() - 60, 240, scaledResolution.getScaledHeight(), -1873784752);

        while (k != index) {
            int i1 = frameTimer.getLagometerValue(frames[k], 30);
            int j1 = this.getFrameColor(MathHelper.clamp_int(i1, 0, 60), 0, 30, 60);
            this.drawVerticalLine(l, scaledResolution.getScaledHeight(), scaledResolution.getScaledHeight() - i1, j1);
            ++l;
            k = frameTimer.parseIndex(k + 1);
        }

        drawRect(1, scaledResolution.getScaledHeight() - 30 + 1, 14, scaledResolution.getScaledHeight() - 30 + 10, -1873784752);
        this.fontRenderer.drawString("60", 2, scaledResolution.getScaledHeight() - 30 + 2, 14737632);
        this.drawHorizontalLine(0, 239, scaledResolution.getScaledHeight() - 30, -1);
        drawRect(1, scaledResolution.getScaledHeight() - 60 + 1, 14, scaledResolution.getScaledHeight() - 60 + 10, -1873784752);
        this.fontRenderer.drawString("30", 2, scaledResolution.getScaledHeight() - 60 + 2, 14737632);
        this.drawHorizontalLine(0, 239, scaledResolution.getScaledHeight() - 60, -1);
        this.drawHorizontalLine(0, 239, scaledResolution.getScaledHeight() - 1, -1);
        this.drawVerticalLine(0, scaledResolution.getScaledHeight() - 60, scaledResolution.getScaledHeight(), -1);
        this.drawVerticalLine(239, scaledResolution.getScaledHeight() - 60, scaledResolution.getScaledHeight(), -1);

        if (this.mc.gameSettings.limitFramerate <= 120) {
            this.drawHorizontalLine(0, 239, scaledResolution.getScaledHeight() - 60 + this.mc.gameSettings.limitFramerate / 2, -16711681);
        }

        GlStateManager.enableDepth();
    }

    private int getFrameColor(int p_181552_1_, int p_181552_2_, int p_181552_3_, int p_181552_4_) {
        return p_181552_1_ < p_181552_3_ ? this.blendColors(-16711936, -256, (float) p_181552_1_ / (float) p_181552_3_) : this.blendColors(-256, -65536, (float) (p_181552_1_ - p_181552_3_) / (float) (p_181552_4_ - p_181552_3_));
    }

    private int blendColors(int p_181553_1_, int p_181553_2_, float p_181553_3_) {
        int i = p_181553_1_ >> 24 & 255;
        int j = p_181553_1_ >> 16 & 255;
        int k = p_181553_1_ >> 8 & 255;
        int l = p_181553_1_ & 255;
        int i1 = p_181553_2_ >> 24 & 255;
        int j1 = p_181553_2_ >> 16 & 255;
        int k1 = p_181553_2_ >> 8 & 255;
        int l1 = p_181553_2_ & 255;
        int i2 = MathHelper.clamp_int((int) ((float) i + (float) (i1 - i) * p_181553_3_), 0, 255);
        int j2 = MathHelper.clamp_int((int) ((float) j + (float) (j1 - j) * p_181553_3_), 0, 255);
        int k2 = MathHelper.clamp_int((int) ((float) k + (float) (k1 - k) * p_181553_3_), 0, 255);
        int l2 = MathHelper.clamp_int((int) ((float) l + (float) (l1 - l) * p_181553_3_), 0, 255);
        return i2 << 24 | j2 << 16 | k2 << 8 | l2;
    }

    private static long bytesToMb(long bytes) {
        return bytes / 1024L / 1024L;
    }
}