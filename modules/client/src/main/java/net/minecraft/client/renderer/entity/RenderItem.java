package net.minecraft.client.renderer.entity;

import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.block.model.ItemTransformVec3f;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.renderer.tileentity.TileEntityItemStackRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.client.resources.model.ModelManager;
import net.minecraft.client.resources.model.ModelResourceLocation;
import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.crash.ReportedException;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.*;
import net.optifine.Config;
import net.minecraft.util.*;
import net.minecraft.util.chat.Formatting;
import net.minecraft.util.math.Vec3i;
import net.optifine.CustomColors;
import net.optifine.CustomItems;
import net.optifine.shaders.Shaders;
import net.optifine.shaders.ShadersRender;
import org.lwjgl.opengl.GL11;

import java.util.List;

public class RenderItem implements IResourceManagerReloadListener {
    private static final ResourceLocation RES_ITEM_GLINT = new ResourceLocation("textures/misc/enchanted_item_glint.png");
    private boolean notRenderingEffectsInGUI = true;
    public float zLevel;
    private final ItemModelMesher itemModelMesher;
    private final TextureManager textureManager;
    private ModelResourceLocation modelLocation = null;
    private boolean renderItemGui = false;
    public final ModelManager modelManager;
    private boolean renderModelHasEmissive = false;
    private boolean renderModelEmissive = false;

    public RenderItem(TextureManager textureManager, ModelManager modelManager) {
        this.textureManager = textureManager;
        this.modelManager = modelManager;
        this.itemModelMesher = new ItemModelMesher(modelManager);

        this.registerItems();
    }

    public void isNotRenderingEffectsInGUI(boolean isNot) {
        this.notRenderingEffectsInGUI = isNot;
    }

    public ItemModelMesher getItemModelMesher() {
        return this.itemModelMesher;
    }

    protected void registerItem(Item itm, int subType, String identifier) {
        this.itemModelMesher.register(itm, subType, new ModelResourceLocation(identifier, "inventory"));
    }

    protected void registerBlock(Block blk, int subType, String identifier) {
        this.registerItem(Item.getItemFromBlock(blk), subType, identifier);
    }

    private void registerBlock(Block blk, String identifier) {
        this.registerBlock(blk, 0, identifier);
    }

    private void registerItem(Item itm, String identifier) {
        this.registerItem(itm, 0, identifier);
    }

    private void renderModel(IBakedModel model, ItemStack stack) {
        this.renderModel(model, -1, stack);
    }

    public void renderModel(IBakedModel model, int color) {
        this.renderModel(model, color, null);
    }

    private void renderModel(IBakedModel model, int color, ItemStack stack) {
        Tessellator tessellator = Tessellator.get();
        WorldRenderer worldrenderer = tessellator.getWorldRenderer();
        boolean flag = Minecraft.get().getTextureMapBlocks().isTextureBound();
        boolean flag1 = Config.isMultiTexture() && flag;

        if (flag1) {
            worldrenderer.setBlockLayer(RenderLayer.SOLID);
        }

        worldrenderer.begin(7, DefaultVertexFormats.ITEM);

        for (Direction enumfacing : Direction.VALUES) {
            this.renderQuads(worldrenderer, model.getFaceQuads(enumfacing), color, stack);
        }

        this.renderQuads(worldrenderer, model.getGeneralQuads(), color, stack);
        tessellator.draw();

        if (flag1) {
            worldrenderer.setBlockLayer(null);
            GlStateManager.bindCurrentTexture();
        }
    }

    public void renderItem(ItemStack stack, IBakedModel model) {
        if (stack != null) {
            GlStateManager.pushMatrix();
            GlStateManager.scale(0.5F, 0.5F, 0.5F);

            if (model.isBuiltInRenderer()) {
                GlStateManager.rotate(180.0F, 0.0F, 1.0F, 0.0F);
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);
                GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
                GlStateManager.enableRescaleNormal();
                TileEntityItemStackRenderer.INSTANCE.renderByItem(stack);
            } else {
                GlStateManager.translate(-0.5F, -0.5F, -0.5F);

                if (Config.isCustomItems()) {
                    model = CustomItems.getCustomItemModel(stack, model, this.modelLocation, false);
                }

                this.renderModelHasEmissive = false;
                this.renderModel(model, stack);

                if (this.renderModelHasEmissive) {
                    float f = OpenGlHelper.lastBrightnessX;
                    float f1 = OpenGlHelper.lastBrightnessY;
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, f1);
                    this.renderModelEmissive = true;
                    this.renderModel(model, stack);
                    this.renderModelEmissive = false;
                    OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, f, f1);
                }

                if (stack.hasEffect() && (!Config.isCustomItems() || !CustomItems.renderCustomEffect(this, stack, model))) {
                    this.renderEffect(model);
                }
            }

            GlStateManager.popMatrix();
        }
    }

    private void renderEffect(IBakedModel model) {
        if (!Config.isCustomItems() || CustomItems.isUseGlint()) {
            if (!Config.isShaders() || !Shaders.isShadowPass) {
                GlStateManager.depthMask(false);
                GlStateManager.depthFunc(GL11.GL_EQUAL);
                GlStateManager.disableLighting();
                GlStateManager.blendFunc(GL11.GL_SRC_COLOR, GL11.GL_ONE);
                this.textureManager.bindTexture(RES_ITEM_GLINT);

                if (Config.isShaders() && !this.renderItemGui) {
                    ShadersRender.renderEnchantedGlintBegin();
                }

                GlStateManager.matrixMode(GL11.GL_TEXTURE);
                GlStateManager.pushMatrix();
                GlStateManager.scale(8.0F, 8.0F, 8.0F);
                float f = (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
                GlStateManager.translate(f, 0.0F, 0.0F);
                GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
                this.renderModel(model, -8372020);
                GlStateManager.popMatrix();
                GlStateManager.pushMatrix();
                GlStateManager.scale(8.0F, 8.0F, 8.0F);
                float f1 = (Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
                GlStateManager.translate(-f1, 0.0F, 0.0F);
                GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
                this.renderModel(model, -8372020);
                GlStateManager.popMatrix();
                GlStateManager.matrixMode(GL11.GL_MODELVIEW);
                GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
                GlStateManager.enableLighting();
                GlStateManager.depthFunc(GL11.GL_LEQUAL);
                GlStateManager.depthMask(true);
                this.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);

                if (Config.isShaders() && !this.renderItemGui) {
                    ShadersRender.renderEnchantedGlintEnd();
                }
            }
        }
    }

    private void putQuadNormal(WorldRenderer renderer, BakedQuad quad) {
        Vec3i vec3i = quad.getFace().getDirectionVec();
        renderer.putNormal(vec3i.getX(), vec3i.getY(), vec3i.getZ());
    }

    private void renderQuad(WorldRenderer renderer, BakedQuad quad, int color) {
        if (this.renderModelEmissive) {
            if (quad.getQuadEmissive() == null) {
                return;
            }

            quad = quad.getQuadEmissive();
        } else if (quad.getQuadEmissive() != null) {
            this.renderModelHasEmissive = true;
        }

        if (renderer.isMultiTexture()) {
            renderer.addVertexData(quad.getVertexDataSingle());
        } else {
            renderer.addVertexData(quad.getVertexData());
        }

        renderer.putSprite(quad.getSprite());

        renderer.putColor4(color);

        this.putQuadNormal(renderer, quad);
    }

    private void renderQuads(WorldRenderer renderer, List<BakedQuad> quads, int color, ItemStack stack) {
        boolean flag = color == -1 && stack != null;
        int i = 0;

        for (int j = quads.size(); i < j; ++i) {
            BakedQuad bakedquad = quads.get(i);
            int k = color;

            if (flag && bakedquad.hasTintIndex()) {
                k = stack.getItem().getColorFromItemStack(stack, bakedquad.getTintIndex());

                if (Config.isCustomColors()) {
                    k = CustomColors.getColorFromItemStack(stack, bakedquad.getTintIndex(), k);
                }

                k = k | -16777216;
            }

            this.renderQuad(renderer, bakedquad, k);
        }
    }

    public boolean shouldRenderItemIn3D(ItemStack stack) {
        IBakedModel ibakedmodel = this.itemModelMesher.getItemModel(stack);
        return ibakedmodel != null && ibakedmodel.isGui3d();
    }

    private void preTransform(ItemStack stack) {
        IBakedModel ibakedmodel = this.itemModelMesher.getItemModel(stack);
        Item item = stack.getItem();

        if (item != null) {
            boolean flag = ibakedmodel.isGui3d();

            if (!flag) {
                GlStateManager.scale(2.0F, 2.0F, 2.0F);
            }

            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    public void renderItem(ItemStack stack, ItemCameraTransforms.TransformType cameraTransformType) {
        if (stack != null) {
            IBakedModel ibakedmodel = this.itemModelMesher.getItemModel(stack);
            this.renderItemModelTransform(stack, ibakedmodel, cameraTransformType);
        }
    }

    public void renderItemModelForEntity(ItemStack stack, EntityLivingBase entityToRenderFor, ItemCameraTransforms.TransformType cameraTransformType) {
        if (stack != null && entityToRenderFor != null) {
            IBakedModel ibakedmodel = this.itemModelMesher.getItemModel(stack);

            if (entityToRenderFor instanceof EntityPlayer entityplayer) {
                Item item = stack.getItem();
                ModelResourceLocation modelresourcelocation = null;

                if (item == Items.FISHING_ROD && entityplayer.fishEntity != null) {
                    modelresourcelocation = new ModelResourceLocation("fishing_rod_cast", "inventory");
                } else if (item == Items.BOW && entityplayer.getItemInUse() != null) {
                    int i = stack.getMaxItemUseDuration() - entityplayer.getItemInUseCount();

                    if (i >= 18) {
                        modelresourcelocation = new ModelResourceLocation("bow_pulling_2", "inventory");
                    } else if (i > 13) {
                        modelresourcelocation = new ModelResourceLocation("bow_pulling_1", "inventory");
                    } else if (i > 0) {
                        modelresourcelocation = new ModelResourceLocation("bow_pulling_0", "inventory");
                    }
                }

                if (modelresourcelocation != null) {
                    ibakedmodel = this.itemModelMesher.getModelManager().getModel(modelresourcelocation);
                    this.modelLocation = modelresourcelocation;
                }
            }

            this.renderItemModelTransform(stack, ibakedmodel, cameraTransformType);
            this.modelLocation = null;
        }
    }

    protected void renderItemModelTransform(ItemStack stack, IBakedModel model, ItemCameraTransforms.TransformType cameraTransformType) {
        this.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        this.preTransform(stack);
        GlStateManager.enableRescaleNormal();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.pushMatrix();

        ItemCameraTransforms itemcameratransforms = model.getItemCameraTransforms();
        itemcameratransforms.applyTransform(cameraTransformType);

        if (this.isThereOneNegativeScale(itemcameratransforms.getTransform(cameraTransformType))) {
            GlStateManager.cullFace(1028);
        }

        this.renderItem(stack, model);
        GlStateManager.cullFace(1029);
        GlStateManager.popMatrix();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableBlend();
        this.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
    }

    private boolean isThereOneNegativeScale(ItemTransformVec3f itemTranformVec) {
        return itemTranformVec.scale().x < 0.0F ^ itemTranformVec.scale().y < 0.0F ^ itemTranformVec.scale().z < 0.0F;
    }

    public void renderItemIntoGUI(ItemStack stack, int x, int y) {
        this.renderItemGui = true;
        IBakedModel ibakedmodel = this.itemModelMesher.getItemModel(stack);
        GlStateManager.pushMatrix();
        this.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).setBlurMipmap(false, false);
        GlStateManager.enableRescaleNormal();
        GlStateManager.enableAlpha();
        GlStateManager.alphaFunc(GL11.GL_GREATER, 0.1F);
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        this.setupGuiTransform(x, y, ibakedmodel.isGui3d());

        ibakedmodel.getItemCameraTransforms().applyTransform(ItemCameraTransforms.TransformType.GUI);

        this.renderItem(stack, ibakedmodel);
        GlStateManager.disableAlpha();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableLighting();
        GlStateManager.popMatrix();
        this.textureManager.bindTexture(TextureMap.LOCATION_BLOCKS_TEXTURE);
        this.textureManager.getTexture(TextureMap.LOCATION_BLOCKS_TEXTURE).restoreLastBlurMipmap();
        this.renderItemGui = false;
    }

    private void setupGuiTransform(int xPosition, int yPosition, boolean isGui3d) {
        GlStateManager.translate(xPosition, yPosition, 100.0F + this.zLevel);
        GlStateManager.translate(8.0F, 8.0F, 0.0F);
        GlStateManager.scale(1.0F, 1.0F, -1.0F);
        GlStateManager.scale(0.5F, 0.5F, 0.5F);

        if (isGui3d) {
            GlStateManager.scale(40.0F, 40.0F, 40.0F);
            GlStateManager.rotate(210.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.rotate(-135.0F, 0.0F, 1.0F, 0.0F);
            GlStateManager.enableLighting();
        } else {
            GlStateManager.scale(64.0F, 64.0F, 64.0F);
            GlStateManager.rotate(180.0F, 1.0F, 0.0F, 0.0F);
            GlStateManager.disableLighting();
        }
    }

    public void renderItemAndEffectIntoGUI(final ItemStack stack, int xPosition, int yPosition) {
        if (stack != null && stack.getItem() != null) {
            this.zLevel += 50.0F;

            try {
                this.renderItemIntoGUI(stack, xPosition, yPosition);
            } catch (Throwable throwable) {
                CrashReport report = CrashReport.makeCrashReport(throwable, "Rendering item");
                CrashReportCategory category = report.makeCategory("Item being rendered");
                category.addCrashSectionCallable("Item Type", () -> String.valueOf(stack.getItem()));
                category.addCrashSectionCallable("Item Aux", () -> String.valueOf(stack.getMetadata()));
                category.addCrashSectionCallable("Item NBT", () -> String.valueOf(stack.getTagCompound()));
                category.addCrashSectionCallable("Item Foil", () -> String.valueOf(stack.hasEffect()));
                throw new ReportedException(report);
            }

            this.zLevel -= 50.0F;
        }
    }

    public void renderItemOverlays(FontRenderer fr, ItemStack stack, int xPosition, int yPosition) {
        this.renderItemOverlayIntoGUI(fr, stack, xPosition, yPosition, null);
    }

    public void renderItemOverlayIntoGUI(FontRenderer fr, ItemStack stack, int xPosition, int yPosition, String text) {
        if (stack != null) {
            if (stack.stackSize != 1 || text != null) {
                String s = text == null ? String.valueOf(stack.stackSize) : text;

                if (text == null && stack.stackSize < 1) {
                    s = Formatting.RED + String.valueOf(stack.stackSize);
                }

                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableBlend();
                fr.drawStringWithShadow(s, (xPosition + 19 - 2 - fr.getStringWidth(s)), (yPosition + 6 + 3), 16777215);
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
                GlStateManager.enableBlend();
            }

            if (stack.isItemDamaged()) {
                int j1 = (int) Math.round(13.0D - stack.getItemDamage() * 13.0D / stack.getMaxDamage());
                int i = (int) Math.round(255.0D - stack.getItemDamage() * 255.0D / stack.getMaxDamage());

                GlStateManager.disableLighting();
                GlStateManager.disableDepth();
                GlStateManager.disableTexture2D();
                GlStateManager.disableAlpha();
                GlStateManager.disableBlend();
                Tessellator tessellator = Tessellator.get();
                WorldRenderer worldrenderer = tessellator.getWorldRenderer();
                this.draw(worldrenderer, xPosition + 2, yPosition + 13, 13, 2, 0, 0, 0, 255);
                this.draw(worldrenderer, xPosition + 2, yPosition + 13, 12, 1, (255 - i) / 4, 64, 0, 255);
                int j = 255 - i;
                int k = i;
                int l = 0;

                if (Config.isCustomColors()) {
                    int i1 = CustomColors.getDurabilityColor(i);

                    if (i1 >= 0) {
                        j = i1 >> 16 & 255;
                        k = i1 >> 8 & 255;
                        l = i1 & 255;
                    }
                }

                this.draw(worldrenderer, xPosition + 2, yPosition + 13, j1, 1, j, k, l, 255);
                GlStateManager.enableBlend();
                GlStateManager.enableAlpha();
                GlStateManager.enableTexture2D();
                GlStateManager.enableLighting();
                GlStateManager.enableDepth();
            }
        }
    }

    private void draw(WorldRenderer renderer, int x, int y, int width, int height, int red, int green, int blue, int alpha) {
        renderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        renderer.pos((x), (y), 0.0D).color(red, green, blue, alpha).endVertex();
        renderer.pos((x), (y + height), 0.0D).color(red, green, blue, alpha).endVertex();
        renderer.pos((x + width), (y + height), 0.0D).color(red, green, blue, alpha).endVertex();
        renderer.pos((x + width), (y), 0.0D).color(red, green, blue, alpha).endVertex();
        Tessellator.get().draw();
    }

    private void registerItems() {
        this.registerBlock(Blocks.ANVIL, "anvil_intact");
        this.registerBlock(Blocks.ANVIL, 1, "anvil_slightly_damaged");
        this.registerBlock(Blocks.ANVIL, 2, "anvil_very_damaged");
        this.registerBlock(Blocks.CARPET, DyeColor.BLACK.getMetadata(), "black_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.BLUE.getMetadata(), "blue_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.BROWN.getMetadata(), "brown_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.CYAN.getMetadata(), "cyan_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.GRAY.getMetadata(), "gray_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.GREEN.getMetadata(), "green_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.LIGHT_BLUE.getMetadata(), "light_blue_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.LIME.getMetadata(), "lime_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.MAGENTA.getMetadata(), "magenta_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.ORANGE.getMetadata(), "orange_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.PINK.getMetadata(), "pink_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.PURPLE.getMetadata(), "purple_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.RED.getMetadata(), "red_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.SILVER.getMetadata(), "silver_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.WHITE.getMetadata(), "white_carpet");
        this.registerBlock(Blocks.CARPET, DyeColor.YELLOW.getMetadata(), "yellow_carpet");
        this.registerBlock(Blocks.COBBLESTONE_WALL, BlockWall.WallType.MOSSY.getMetadata(), "mossy_cobblestone_wall");
        this.registerBlock(Blocks.COBBLESTONE_WALL, BlockWall.WallType.NORMAL.getMetadata(), "cobblestone_wall");
        this.registerBlock(Blocks.DIRT, BlockDirt.DirtType.COARSE_DIRT.getMetadata(), "coarse_dirt");
        this.registerBlock(Blocks.DIRT, BlockDirt.DirtType.DIRT.getMetadata(), "dirt");
        this.registerBlock(Blocks.DIRT, BlockDirt.DirtType.PODZOL.getMetadata(), "podzol");
        this.registerBlock(Blocks.DOUBLE_PLANT, BlockDoublePlant.EnumPlantType.FERN.getMeta(), "double_fern");
        this.registerBlock(Blocks.DOUBLE_PLANT, BlockDoublePlant.EnumPlantType.GRASS.getMeta(), "double_grass");
        this.registerBlock(Blocks.DOUBLE_PLANT, BlockDoublePlant.EnumPlantType.PAEONIA.getMeta(), "paeonia");
        this.registerBlock(Blocks.DOUBLE_PLANT, BlockDoublePlant.EnumPlantType.ROSE.getMeta(), "double_rose");
        this.registerBlock(Blocks.DOUBLE_PLANT, BlockDoublePlant.EnumPlantType.SUNFLOWER.getMeta(), "sunflower");
        this.registerBlock(Blocks.DOUBLE_PLANT, BlockDoublePlant.EnumPlantType.SYRINGA.getMeta(), "syringa");
        this.registerBlock(Blocks.LEAVES, BlockPlanks.WoodType.BIRCH.getMetadata(), "birch_leaves");
        this.registerBlock(Blocks.LEAVES, BlockPlanks.WoodType.JUNGLE.getMetadata(), "jungle_leaves");
        this.registerBlock(Blocks.LEAVES, BlockPlanks.WoodType.OAK.getMetadata(), "oak_leaves");
        this.registerBlock(Blocks.LEAVES, BlockPlanks.WoodType.SPRUCE.getMetadata(), "spruce_leaves");
        this.registerBlock(Blocks.LEAVES_2, BlockPlanks.WoodType.ACACIA.getMetadata() - 4, "acacia_leaves");
        this.registerBlock(Blocks.LEAVES_2, BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4, "dark_oak_leaves");
        this.registerBlock(Blocks.LOG, BlockPlanks.WoodType.BIRCH.getMetadata(), "birch_log");
        this.registerBlock(Blocks.LOG, BlockPlanks.WoodType.JUNGLE.getMetadata(), "jungle_log");
        this.registerBlock(Blocks.LOG, BlockPlanks.WoodType.OAK.getMetadata(), "oak_log");
        this.registerBlock(Blocks.LOG, BlockPlanks.WoodType.SPRUCE.getMetadata(), "spruce_log");
        this.registerBlock(Blocks.LOG_2, BlockPlanks.WoodType.ACACIA.getMetadata() - 4, "acacia_log");
        this.registerBlock(Blocks.LOG_2, BlockPlanks.WoodType.DARK_OAK.getMetadata() - 4, "dark_oak_log");
        this.registerBlock(Blocks.MONSTER_EGG, BlockSilverfish.EnumType.CHISELED_STONEBRICK.getMetadata(), "chiseled_brick_monster_egg");
        this.registerBlock(Blocks.MONSTER_EGG, BlockSilverfish.EnumType.COBBLESTONE.getMetadata(), "cobblestone_monster_egg");
        this.registerBlock(Blocks.MONSTER_EGG, BlockSilverfish.EnumType.CRACKED_STONEBRICK.getMetadata(), "cracked_brick_monster_egg");
        this.registerBlock(Blocks.MONSTER_EGG, BlockSilverfish.EnumType.MOSSY_STONEBRICK.getMetadata(), "mossy_brick_monster_egg");
        this.registerBlock(Blocks.MONSTER_EGG, BlockSilverfish.EnumType.STONE.getMetadata(), "stone_monster_egg");
        this.registerBlock(Blocks.MONSTER_EGG, BlockSilverfish.EnumType.STONEBRICK.getMetadata(), "stone_brick_monster_egg");
        this.registerBlock(Blocks.PLANKS, BlockPlanks.WoodType.ACACIA.getMetadata(), "acacia_planks");
        this.registerBlock(Blocks.PLANKS, BlockPlanks.WoodType.BIRCH.getMetadata(), "birch_planks");
        this.registerBlock(Blocks.PLANKS, BlockPlanks.WoodType.DARK_OAK.getMetadata(), "dark_oak_planks");
        this.registerBlock(Blocks.PLANKS, BlockPlanks.WoodType.JUNGLE.getMetadata(), "jungle_planks");
        this.registerBlock(Blocks.PLANKS, BlockPlanks.WoodType.OAK.getMetadata(), "oak_planks");
        this.registerBlock(Blocks.PLANKS, BlockPlanks.WoodType.SPRUCE.getMetadata(), "spruce_planks");
        this.registerBlock(Blocks.PRISMARINE, BlockPrismarine.PrismarineType.BRICKS.getMetadata(), "prismarine_bricks");
        this.registerBlock(Blocks.PRISMARINE, BlockPrismarine.PrismarineType.DARK.getMetadata(), "dark_prismarine");
        this.registerBlock(Blocks.PRISMARINE, BlockPrismarine.PrismarineType.ROUGH.getMetadata(), "prismarine");
        this.registerBlock(Blocks.QUARTZ_BLOCK, BlockQuartz.QuartzType.CHISELED.getMetadata(), "chiseled_quartz_block");
        this.registerBlock(Blocks.QUARTZ_BLOCK, BlockQuartz.QuartzType.DEFAULT.getMetadata(), "quartz_block");
        this.registerBlock(Blocks.QUARTZ_BLOCK, BlockQuartz.QuartzType.LINES_Y.getMetadata(), "quartz_column");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.ALLIUM.getMeta(), "allium");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.BLUE_ORCHID.getMeta(), "blue_orchid");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.HOUSTONIA.getMeta(), "houstonia");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.ORANGE_TULIP.getMeta(), "orange_tulip");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.OXEYE_DAISY.getMeta(), "oxeye_daisy");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.PINK_TULIP.getMeta(), "pink_tulip");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.POPPY.getMeta(), "poppy");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.RED_TULIP.getMeta(), "red_tulip");
        this.registerBlock(Blocks.RED_FLOWER, BlockFlower.FlowerType.WHITE_TULIP.getMeta(), "white_tulip");
        this.registerBlock(Blocks.SAND, BlockSand.SandType.RED_SAND.getMetadata(), "red_sand");
        this.registerBlock(Blocks.SAND, BlockSand.SandType.SAND.getMetadata(), "sand");
        this.registerBlock(Blocks.SANDSTONE, BlockSandStone.SandStoneType.CHISELED.getMetadata(), "chiseled_sandstone");
        this.registerBlock(Blocks.SANDSTONE, BlockSandStone.SandStoneType.DEFAULT.getMetadata(), "sandstone");
        this.registerBlock(Blocks.SANDSTONE, BlockSandStone.SandStoneType.SMOOTH.getMetadata(), "smooth_sandstone");
        this.registerBlock(Blocks.RED_SANDSTONE, BlockRedSandstone.RedSandStoneType.CHISELED.getMetadata(), "chiseled_red_sandstone");
        this.registerBlock(Blocks.RED_SANDSTONE, BlockRedSandstone.RedSandStoneType.DEFAULT.getMetadata(), "red_sandstone");
        this.registerBlock(Blocks.RED_SANDSTONE, BlockRedSandstone.RedSandStoneType.SMOOTH.getMetadata(), "smooth_red_sandstone");
        this.registerBlock(Blocks.SAPLING, BlockPlanks.WoodType.ACACIA.getMetadata(), "acacia_sapling");
        this.registerBlock(Blocks.SAPLING, BlockPlanks.WoodType.BIRCH.getMetadata(), "birch_sapling");
        this.registerBlock(Blocks.SAPLING, BlockPlanks.WoodType.DARK_OAK.getMetadata(), "dark_oak_sapling");
        this.registerBlock(Blocks.SAPLING, BlockPlanks.WoodType.JUNGLE.getMetadata(), "jungle_sapling");
        this.registerBlock(Blocks.SAPLING, BlockPlanks.WoodType.OAK.getMetadata(), "oak_sapling");
        this.registerBlock(Blocks.SAPLING, BlockPlanks.WoodType.SPRUCE.getMetadata(), "spruce_sapling");
        this.registerBlock(Blocks.SPONGE, 0, "sponge");
        this.registerBlock(Blocks.SPONGE, 1, "sponge_wet");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.BLACK.getMetadata(), "black_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.BLUE.getMetadata(), "blue_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.BROWN.getMetadata(), "brown_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.CYAN.getMetadata(), "cyan_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.GRAY.getMetadata(), "gray_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.GREEN.getMetadata(), "green_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.LIGHT_BLUE.getMetadata(), "light_blue_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.LIME.getMetadata(), "lime_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.MAGENTA.getMetadata(), "magenta_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.ORANGE.getMetadata(), "orange_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.PINK.getMetadata(), "pink_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.PURPLE.getMetadata(), "purple_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.RED.getMetadata(), "red_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.SILVER.getMetadata(), "silver_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.WHITE.getMetadata(), "white_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS, DyeColor.YELLOW.getMetadata(), "yellow_stained_glass");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.BLACK.getMetadata(), "black_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.BLUE.getMetadata(), "blue_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.BROWN.getMetadata(), "brown_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.CYAN.getMetadata(), "cyan_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.GRAY.getMetadata(), "gray_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.GREEN.getMetadata(), "green_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.LIGHT_BLUE.getMetadata(), "light_blue_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.LIME.getMetadata(), "lime_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.MAGENTA.getMetadata(), "magenta_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.ORANGE.getMetadata(), "orange_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.PINK.getMetadata(), "pink_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.PURPLE.getMetadata(), "purple_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.RED.getMetadata(), "red_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.SILVER.getMetadata(), "silver_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.WHITE.getMetadata(), "white_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_GLASS_PANE, DyeColor.YELLOW.getMetadata(), "yellow_stained_glass_pane");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.BLACK.getMetadata(), "black_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.BLUE.getMetadata(), "blue_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.BROWN.getMetadata(), "brown_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.CYAN.getMetadata(), "cyan_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.GRAY.getMetadata(), "gray_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.GREEN.getMetadata(), "green_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.LIGHT_BLUE.getMetadata(), "light_blue_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.LIME.getMetadata(), "lime_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.MAGENTA.getMetadata(), "magenta_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.ORANGE.getMetadata(), "orange_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.PINK.getMetadata(), "pink_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.PURPLE.getMetadata(), "purple_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.RED.getMetadata(), "red_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.SILVER.getMetadata(), "silver_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.WHITE.getMetadata(), "white_stained_hardened_clay");
        this.registerBlock(Blocks.STAINED_HARDENED_CLAY, DyeColor.YELLOW.getMetadata(), "yellow_stained_hardened_clay");
        this.registerBlock(Blocks.STONE, BlockStone.StoneType.ANDESITE.getMetadata(), "andesite");
        this.registerBlock(Blocks.STONE, BlockStone.StoneType.ANDESITE_SMOOTH.getMetadata(), "andesite_smooth");
        this.registerBlock(Blocks.STONE, BlockStone.StoneType.DIORITE.getMetadata(), "diorite");
        this.registerBlock(Blocks.STONE, BlockStone.StoneType.DIORITE_SMOOTH.getMetadata(), "diorite_smooth");
        this.registerBlock(Blocks.STONE, BlockStone.StoneType.GRANITE.getMetadata(), "granite");
        this.registerBlock(Blocks.STONE, BlockStone.StoneType.GRANITE_SMOOTH.getMetadata(), "granite_smooth");
        this.registerBlock(Blocks.STONE, BlockStone.StoneType.STONE.getMetadata(), "stone");
        this.registerBlock(Blocks.STONEBRICK, BlockStoneBrick.EnumType.CRACKED.getMetadata(), "cracked_stonebrick");
        this.registerBlock(Blocks.STONEBRICK, BlockStoneBrick.EnumType.DEFAULT.getMetadata(), "stonebrick");
        this.registerBlock(Blocks.STONEBRICK, BlockStoneBrick.EnumType.CHISELED.getMetadata(), "chiseled_stonebrick");
        this.registerBlock(Blocks.STONEBRICK, BlockStoneBrick.EnumType.MOSSY.getMetadata(), "mossy_stonebrick");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.BRICK.getMetadata(), "brick_slab");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.COBBLESTONE.getMetadata(), "cobblestone_slab");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.WOOD.getMetadata(), "old_wood_slab");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.NETHERBRICK.getMetadata(), "nether_brick_slab");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.QUARTZ.getMetadata(), "quartz_slab");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.SAND.getMetadata(), "sandstone_slab");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.SMOOTHBRICK.getMetadata(), "stone_brick_slab");
        this.registerBlock(Blocks.STONE_SLAB, BlockStoneSlab.EnumType.STONE.getMetadata(), "stone_slab");
        this.registerBlock(Blocks.BLOCK_SLAB, BlockStoneSlabNew.EnumType.RED_SANDSTONE.getMetadata(), "red_sandstone_slab");
        this.registerBlock(Blocks.TALL_GRASS, BlockTallGrass.EnumType.DEAD_BUSH.getMeta(), "dead_bush");
        this.registerBlock(Blocks.TALL_GRASS, BlockTallGrass.EnumType.FERN.getMeta(), "fern");
        this.registerBlock(Blocks.TALL_GRASS, BlockTallGrass.EnumType.GRASS.getMeta(), "tall_grass");
        this.registerBlock(Blocks.WOODEN_SLAB, BlockPlanks.WoodType.ACACIA.getMetadata(), "acacia_slab");
        this.registerBlock(Blocks.WOODEN_SLAB, BlockPlanks.WoodType.BIRCH.getMetadata(), "birch_slab");
        this.registerBlock(Blocks.WOODEN_SLAB, BlockPlanks.WoodType.DARK_OAK.getMetadata(), "dark_oak_slab");
        this.registerBlock(Blocks.WOODEN_SLAB, BlockPlanks.WoodType.JUNGLE.getMetadata(), "jungle_slab");
        this.registerBlock(Blocks.WOODEN_SLAB, BlockPlanks.WoodType.OAK.getMetadata(), "oak_slab");
        this.registerBlock(Blocks.WOODEN_SLAB, BlockPlanks.WoodType.SPRUCE.getMetadata(), "spruce_slab");
        this.registerBlock(Blocks.WOOL, DyeColor.BLACK.getMetadata(), "black_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.BLUE.getMetadata(), "blue_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.BROWN.getMetadata(), "brown_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.CYAN.getMetadata(), "cyan_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.GRAY.getMetadata(), "gray_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.GREEN.getMetadata(), "green_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.LIGHT_BLUE.getMetadata(), "light_blue_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.LIME.getMetadata(), "lime_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.MAGENTA.getMetadata(), "magenta_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.ORANGE.getMetadata(), "orange_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.PINK.getMetadata(), "pink_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.PURPLE.getMetadata(), "purple_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.RED.getMetadata(), "red_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.SILVER.getMetadata(), "silver_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.WHITE.getMetadata(), "white_wool");
        this.registerBlock(Blocks.WOOL, DyeColor.YELLOW.getMetadata(), "yellow_wool");
        this.registerBlock(Blocks.ACACIA_STAIRS, "acacia_stairs");
        this.registerBlock(Blocks.ACTIVATOR_RAIL, "activator_rail");
        this.registerBlock(Blocks.BEACON, "beacon");
        this.registerBlock(Blocks.BEDROCK, "bedrock");
        this.registerBlock(Blocks.BIRCH_STAIRS, "birch_stairs");
        this.registerBlock(Blocks.BOOKSHELF, "bookshelf");
        this.registerBlock(Blocks.BRICK_BLOCK, "brick_block");
        this.registerBlock(Blocks.BRICK_BLOCK, "brick_block");
        this.registerBlock(Blocks.BRICK_STAIRS, "brick_stairs");
        this.registerBlock(Blocks.BROWN_MUSHROOM, "brown_mushroom");
        this.registerBlock(Blocks.CACTUS, "cactus");
        this.registerBlock(Blocks.CLAY, "clay");
        this.registerBlock(Blocks.COAL_BLOCK, "coal_block");
        this.registerBlock(Blocks.COAL_ORE, "coal_ore");
        this.registerBlock(Blocks.COBBLESTONE, "cobblestone");
        this.registerBlock(Blocks.CRAFTING_TABLE, "crafting_table");
        this.registerBlock(Blocks.DARK_OAK_STAIRS, "dark_oak_stairs");
        this.registerBlock(Blocks.DAYLIGHT_DETECTOR, "daylight_detector");
        this.registerBlock(Blocks.DEAD_BUSH, "dead_bush");
        this.registerBlock(Blocks.DETECTOR_RAIL, "detector_rail");
        this.registerBlock(Blocks.DIAMOND_BLOCK, "diamond_block");
        this.registerBlock(Blocks.DIAMOND_ORE, "diamond_ore");
        this.registerBlock(Blocks.DISPENSER, "dispenser");
        this.registerBlock(Blocks.DROPPER, "dropper");
        this.registerBlock(Blocks.EMERALD_BLOCK, "emerald_block");
        this.registerBlock(Blocks.EMERALD_ORE, "emerald_ore");
        this.registerBlock(Blocks.ENCHANTING_TABLE, "enchanting_table");
        this.registerBlock(Blocks.END_PORTAL_FRAME, "end_portal_frame");
        this.registerBlock(Blocks.END_STONE, "end_stone");
        this.registerBlock(Blocks.OAK_FENCE, "oak_fence");
        this.registerBlock(Blocks.SPRUCE_FENCE, "spruce_fence");
        this.registerBlock(Blocks.BIRCH_FENCE, "birch_fence");
        this.registerBlock(Blocks.JUNGLE_FENCE, "jungle_fence");
        this.registerBlock(Blocks.DARK_OAK_FENCE, "dark_oak_fence");
        this.registerBlock(Blocks.ACACIA_FENCE, "acacia_fence");
        this.registerBlock(Blocks.OAK_FENCE_GATE, "oak_fence_gate");
        this.registerBlock(Blocks.SPRUCE_FENCE_GATE, "spruce_fence_gate");
        this.registerBlock(Blocks.BIRCH_FENCE_GATE, "birch_fence_gate");
        this.registerBlock(Blocks.JUNGLE_FENCE_GATE, "jungle_fence_gate");
        this.registerBlock(Blocks.DARK_OAK_FENCE_GATE, "dark_oak_fence_gate");
        this.registerBlock(Blocks.ACACIA_FENCE_GATE, "acacia_fence_gate");
        this.registerBlock(Blocks.FURNACE, "furnace");
        this.registerBlock(Blocks.GLASS, "glass");
        this.registerBlock(Blocks.GLASS_PANE, "glass_pane");
        this.registerBlock(Blocks.GLOWSTONE, "glowstone");
        this.registerBlock(Blocks.GOLDEN_RAIL, "golden_rail");
        this.registerBlock(Blocks.GOLD_BLOCK, "gold_block");
        this.registerBlock(Blocks.GOLD_ORE, "gold_ore");
        this.registerBlock(Blocks.GRASS, "grass");
        this.registerBlock(Blocks.GRAVEL, "gravel");
        this.registerBlock(Blocks.HARDENED_CLAY, "hardened_clay");
        this.registerBlock(Blocks.HAY_BLOCK, "hay_block");
        this.registerBlock(Blocks.HEAVY_WEIGHTED_PRESSURE_PLATE, "heavy_weighted_pressure_plate");
        this.registerBlock(Blocks.HOPPER, "hopper");
        this.registerBlock(Blocks.ICE, "ice");
        this.registerBlock(Blocks.IRON_BARS, "iron_bars");
        this.registerBlock(Blocks.IRON_BLOCK, "iron_block");
        this.registerBlock(Blocks.IRON_ORE, "iron_ore");
        this.registerBlock(Blocks.IRON_TRAPDOOR, "iron_trapdoor");
        this.registerBlock(Blocks.JUKEBOX, "jukebox");
        this.registerBlock(Blocks.JUNGLE_STAIRS, "jungle_stairs");
        this.registerBlock(Blocks.LADDER, "ladder");
        this.registerBlock(Blocks.LAPIS_BLOCK, "lapis_block");
        this.registerBlock(Blocks.LAPIS_ORE, "lapis_ore");
        this.registerBlock(Blocks.LEVER, "lever");
        this.registerBlock(Blocks.LIGHT_WEIGHTED_PRESSURE_PLATE, "light_weighted_pressure_plate");
        this.registerBlock(Blocks.LIT_PUMPKIN, "lit_pumpkin");
        this.registerBlock(Blocks.MELON_BLOCK, "melon_block");
        this.registerBlock(Blocks.MOSSY_COBBLESTONE, "mossy_cobblestone");
        this.registerBlock(Blocks.MYCELIUM, "mycelium");
        this.registerBlock(Blocks.NETHERRACK, "netherrack");
        this.registerBlock(Blocks.NETHER_BRICK, "nether_brick");
        this.registerBlock(Blocks.NETHER_BRICK_FENCE, "nether_brick_fence");
        this.registerBlock(Blocks.NETHER_BRICK_STAIRS, "nether_brick_stairs");
        this.registerBlock(Blocks.NOTEBLOCK, "noteblock");
        this.registerBlock(Blocks.OAK_STAIRS, "oak_stairs");
        this.registerBlock(Blocks.OBSIDIAN, "obsidian");
        this.registerBlock(Blocks.PACKED_ICE, "packed_ice");
        this.registerBlock(Blocks.PISTON, "piston");
        this.registerBlock(Blocks.PUMPKIN, "pumpkin");
        this.registerBlock(Blocks.QUARTZ_ORE, "quartz_ore");
        this.registerBlock(Blocks.QUARTZ_STAIRS, "quartz_stairs");
        this.registerBlock(Blocks.RAIL, "rail");
        this.registerBlock(Blocks.REDSTONE_BLOCK, "redstone_block");
        this.registerBlock(Blocks.REDSTONE_LAMP, "redstone_lamp");
        this.registerBlock(Blocks.REDSTONE_ORE, "redstone_ore");
        this.registerBlock(Blocks.REDSTONE_TORCH, "redstone_torch");
        this.registerBlock(Blocks.RED_MUSHROOM, "red_mushroom");
        this.registerBlock(Blocks.SANDSTONE_STAIRS, "sandstone_stairs");
        this.registerBlock(Blocks.RED_SANDSTONE_STAIRS, "red_sandstone_stairs");
        this.registerBlock(Blocks.SEA_LANTERN, "sea_lantern");
        this.registerBlock(Blocks.SLIME_BLOCK, "slime");
        this.registerBlock(Blocks.SNOW, "snow");
        this.registerBlock(Blocks.SNOW_LAYER, "snow_layer");
        this.registerBlock(Blocks.SOUL_SAND, "soul_sand");
        this.registerBlock(Blocks.SPRUCE_STAIRS, "spruce_stairs");
        this.registerBlock(Blocks.STICKY_PISTON, "sticky_piston");
        this.registerBlock(Blocks.STONE_BRICK_STAIRS, "stone_brick_stairs");
        this.registerBlock(Blocks.STONE_BUTTON, "stone_button");
        this.registerBlock(Blocks.STONE_PRESSURE_PLATE, "stone_pressure_plate");
        this.registerBlock(Blocks.STONE_STAIRS, "stone_stairs");
        this.registerBlock(Blocks.TNT, "tnt");
        this.registerBlock(Blocks.TORCH, "torch");
        this.registerBlock(Blocks.TRAPDOOR, "trapdoor");
        this.registerBlock(Blocks.TRIPWIRE_HOOK, "tripwire_hook");
        this.registerBlock(Blocks.VINE, "vine");
        this.registerBlock(Blocks.WATERLILY, "waterlily");
        this.registerBlock(Blocks.WEB, "web");
        this.registerBlock(Blocks.WOODEN_BUTTON, "wooden_button");
        this.registerBlock(Blocks.WOODEN_PRESSURE_PLATE, "wooden_pressure_plate");
        this.registerBlock(Blocks.YELLOW_FLOWER, BlockFlower.FlowerType.DANDELION.getMeta(), "dandelion");
        this.registerBlock(Blocks.CHEST, "chest");
        this.registerBlock(Blocks.TRAPPED_CHEST, "trapped_chest");
        this.registerBlock(Blocks.ENDER_CHEST, "ender_chest");
        this.registerItem(Items.IRON_SHOVEL, "iron_shovel");
        this.registerItem(Items.IRON_PICKAXE, "iron_pickaxe");
        this.registerItem(Items.IRON_AXE, "iron_axe");
        this.registerItem(Items.FLINT_AND_STEEL, "flint_and_steel");
        this.registerItem(Items.APPLE, "apple");
        this.registerItem(Items.BOW, 0, "bow");
        this.registerItem(Items.BOW, 1, "bow_pulling_0");
        this.registerItem(Items.BOW, 2, "bow_pulling_1");
        this.registerItem(Items.BOW, 3, "bow_pulling_2");
        this.registerItem(Items.ARROW, "arrow");
        this.registerItem(Items.COAL, 0, "coal");
        this.registerItem(Items.COAL, 1, "charcoal");
        this.registerItem(Items.DIAMOND, "diamond");
        this.registerItem(Items.IRON_INGOT, "iron_ingot");
        this.registerItem(Items.GOLD_INGOT, "gold_ingot");
        this.registerItem(Items.IRON_SWORD, "iron_sword");
        this.registerItem(Items.WOODEN_SWORD, "wooden_sword");
        this.registerItem(Items.WOODEN_SHOVEL, "wooden_shovel");
        this.registerItem(Items.WOODEN_PICKAXE, "wooden_pickaxe");
        this.registerItem(Items.WOODEN_AXE, "wooden_axe");
        this.registerItem(Items.STONE_SWORD, "stone_sword");
        this.registerItem(Items.STONE_SHOVEL, "stone_shovel");
        this.registerItem(Items.STONE_PICKAXE, "stone_pickaxe");
        this.registerItem(Items.STONE_AXE, "stone_axe");
        this.registerItem(Items.DIAMOND_SWORD, "diamond_sword");
        this.registerItem(Items.DIAMOND_SHOVEL, "diamond_shovel");
        this.registerItem(Items.DIAMOND_PICKAXE, "diamond_pickaxe");
        this.registerItem(Items.DIAMOND_AXE, "diamond_axe");
        this.registerItem(Items.STICK, "stick");
        this.registerItem(Items.BOWL, "bowl");
        this.registerItem(Items.MUSHROOM_STEW, "mushroom_stew");
        this.registerItem(Items.GOLDEN_SWORD, "golden_sword");
        this.registerItem(Items.GOLDEN_SHOVEL, "golden_shovel");
        this.registerItem(Items.GOLDEN_PICKAXE, "golden_pickaxe");
        this.registerItem(Items.GOLDEN_AXE, "golden_axe");
        this.registerItem(Items.STRING, "string");
        this.registerItem(Items.FEATHER, "feather");
        this.registerItem(Items.GUNPOWDER, "gunpowder");
        this.registerItem(Items.WOODEN_HOE, "wooden_hoe");
        this.registerItem(Items.STONE_HOE, "stone_hoe");
        this.registerItem(Items.IRON_HOE, "iron_hoe");
        this.registerItem(Items.DIAMOND_HOE, "diamond_hoe");
        this.registerItem(Items.GOLDEN_HOE, "golden_hoe");
        this.registerItem(Items.WHEAT_SEEDS, "wheat_seeds");
        this.registerItem(Items.WHEAT, "wheat");
        this.registerItem(Items.BREAD, "bread");
        this.registerItem(Items.LEATHER_HELMET, "leather_helmet");
        this.registerItem(Items.LEATHER_CHESTPLATE, "leather_chestplate");
        this.registerItem(Items.LEATHER_LEGGINGS, "leather_leggings");
        this.registerItem(Items.LEATHER_BOOTS, "leather_boots");
        this.registerItem(Items.CHAINMAIL_HELMET, "chainmail_helmet");
        this.registerItem(Items.CHAINMAIL_CHESTPLATE, "chainmail_chestplate");
        this.registerItem(Items.CHAINMAIL_LEGGINGS, "chainmail_leggings");
        this.registerItem(Items.CHAINMAIL_BOOTS, "chainmail_boots");
        this.registerItem(Items.IRON_HELMET, "iron_helmet");
        this.registerItem(Items.IRON_CHESTPLATE, "iron_chestplate");
        this.registerItem(Items.IRON_LEGGINGS, "iron_leggings");
        this.registerItem(Items.IRON_BOOTS, "iron_boots");
        this.registerItem(Items.DIAMOND_HELMET, "diamond_helmet");
        this.registerItem(Items.DIAMOND_CHESTPLATE, "diamond_chestplate");
        this.registerItem(Items.DIAMOND_LEGGINGS, "diamond_leggings");
        this.registerItem(Items.DIAMOND_BOOTS, "diamond_boots");
        this.registerItem(Items.GOLDEN_HELMET, "golden_helmet");
        this.registerItem(Items.GOLDEN_CHESTPLATE, "golden_chestplate");
        this.registerItem(Items.GOLDEN_LEGGINGS, "golden_leggings");
        this.registerItem(Items.GOLDEN_BOOTS, "golden_boots");
        this.registerItem(Items.FLINT, "flint");
        this.registerItem(Items.PORKCHOP, "porkchop");
        this.registerItem(Items.COOKED_PORKCHOP, "cooked_porkchop");
        this.registerItem(Items.PAINTING, "painting");
        this.registerItem(Items.GOLDEN_APPLE, "golden_apple");
        this.registerItem(Items.GOLDEN_APPLE, 1, "golden_apple");
        this.registerItem(Items.SIGN, "sign");
        this.registerItem(Items.OAK_DOOR, "oak_door");
        this.registerItem(Items.SPRUCE_DOOR, "spruce_door");
        this.registerItem(Items.BIRCH_DOOR, "birch_door");
        this.registerItem(Items.JUNGLE_DOOR, "jungle_door");
        this.registerItem(Items.ACACIA_DOOR, "acacia_door");
        this.registerItem(Items.DARK_OAK_DOOR, "dark_oak_door");
        this.registerItem(Items.BUCKET, "bucket");
        this.registerItem(Items.WATER_BUCKET, "water_bucket");
        this.registerItem(Items.LAVA_BUCKET, "lava_bucket");
        this.registerItem(Items.MINECART, "minecart");
        this.registerItem(Items.SADDLE, "saddle");
        this.registerItem(Items.IRON_DOOR, "iron_door");
        this.registerItem(Items.REDSTONE, "redstone");
        this.registerItem(Items.SNOWBALL, "snowball");
        this.registerItem(Items.BOAT, "boat");
        this.registerItem(Items.LEATHER, "leather");
        this.registerItem(Items.MILK_BUCKET, "milk_bucket");
        this.registerItem(Items.BRICK, "brick");
        this.registerItem(Items.CLAY_BALL, "clay_ball");
        this.registerItem(Items.REEDS, "reeds");
        this.registerItem(Items.PAPER, "paper");
        this.registerItem(Items.BOOK, "book");
        this.registerItem(Items.SLIME_BALL, "slime_ball");
        this.registerItem(Items.CHEST_MINECART, "chest_minecart");
        this.registerItem(Items.FURNACE_MINECART, "furnace_minecart");
        this.registerItem(Items.EGG, "egg");
        this.registerItem(Items.COMPASS, "compass");
        this.registerItem(Items.FISHING_ROD, "fishing_rod");
        this.registerItem(Items.FISHING_ROD, 1, "fishing_rod_cast");
        this.registerItem(Items.CLOCK, "clock");
        this.registerItem(Items.GLOWSTONE_DUST, "glowstone_dust");
        this.registerItem(Items.FISH, ItemFishFood.FishType.COD.getMetadata(), "cod");
        this.registerItem(Items.FISH, ItemFishFood.FishType.SALMON.getMetadata(), "salmon");
        this.registerItem(Items.FISH, ItemFishFood.FishType.CLOWNFISH.getMetadata(), "clownfish");
        this.registerItem(Items.FISH, ItemFishFood.FishType.PUFFERFISH.getMetadata(), "pufferfish");
        this.registerItem(Items.COOKED_FISH, ItemFishFood.FishType.COD.getMetadata(), "cooked_cod");
        this.registerItem(Items.COOKED_FISH, ItemFishFood.FishType.SALMON.getMetadata(), "cooked_salmon");
        this.registerItem(Items.DYE, DyeColor.BLACK.getDyeDamage(), "dye_black");
        this.registerItem(Items.DYE, DyeColor.RED.getDyeDamage(), "dye_red");
        this.registerItem(Items.DYE, DyeColor.GREEN.getDyeDamage(), "dye_green");
        this.registerItem(Items.DYE, DyeColor.BROWN.getDyeDamage(), "dye_brown");
        this.registerItem(Items.DYE, DyeColor.BLUE.getDyeDamage(), "dye_blue");
        this.registerItem(Items.DYE, DyeColor.PURPLE.getDyeDamage(), "dye_purple");
        this.registerItem(Items.DYE, DyeColor.CYAN.getDyeDamage(), "dye_cyan");
        this.registerItem(Items.DYE, DyeColor.SILVER.getDyeDamage(), "dye_silver");
        this.registerItem(Items.DYE, DyeColor.GRAY.getDyeDamage(), "dye_gray");
        this.registerItem(Items.DYE, DyeColor.PINK.getDyeDamage(), "dye_pink");
        this.registerItem(Items.DYE, DyeColor.LIME.getDyeDamage(), "dye_lime");
        this.registerItem(Items.DYE, DyeColor.YELLOW.getDyeDamage(), "dye_yellow");
        this.registerItem(Items.DYE, DyeColor.LIGHT_BLUE.getDyeDamage(), "dye_light_blue");
        this.registerItem(Items.DYE, DyeColor.MAGENTA.getDyeDamage(), "dye_magenta");
        this.registerItem(Items.DYE, DyeColor.ORANGE.getDyeDamage(), "dye_orange");
        this.registerItem(Items.DYE, DyeColor.WHITE.getDyeDamage(), "dye_white");
        this.registerItem(Items.BONE, "bone");
        this.registerItem(Items.SUGAR, "sugar");
        this.registerItem(Items.CAKE, "cake");
        this.registerItem(Items.BED, "bed");
        this.registerItem(Items.REPEATER, "repeater");
        this.registerItem(Items.COOKIE, "cookie");
        this.registerItem(Items.SHEARS, "shears");
        this.registerItem(Items.MELON, "melon");
        this.registerItem(Items.PUMPKIN_SEEDS, "pumpkin_seeds");
        this.registerItem(Items.MELON_SEEDS, "melon_seeds");
        this.registerItem(Items.BEEF, "beef");
        this.registerItem(Items.COOKED_BEEF, "cooked_beef");
        this.registerItem(Items.CHICKEN, "chicken");
        this.registerItem(Items.COOKED_CHICKEN, "cooked_chicken");
        this.registerItem(Items.RABBIT, "rabbit");
        this.registerItem(Items.COOKED_RABBIT, "cooked_rabbit");
        this.registerItem(Items.MUTTON, "mutton");
        this.registerItem(Items.COOKED_MUTTON, "cooked_mutton");
        this.registerItem(Items.RABBIT_FOOT, "rabbit_foot");
        this.registerItem(Items.RABBIT_HIDE, "rabbit_hide");
        this.registerItem(Items.RABBIT_STEW, "rabbit_stew");
        this.registerItem(Items.ROTTEN_FLESH, "rotten_flesh");
        this.registerItem(Items.ENDER_PEARL, "ender_pearl");
        this.registerItem(Items.BLAZE_ROD, "blaze_rod");
        this.registerItem(Items.GHAST_TEAR, "ghast_tear");
        this.registerItem(Items.GOLD_NUGGET, "gold_nugget");
        this.registerItem(Items.NETHER_WART, "nether_wart");
        this.itemModelMesher.register(Items.POTION, stack -> ItemPotion.isSplash(stack.getMetadata()) ? new ModelResourceLocation("bottle_splash", "inventory") : new ModelResourceLocation("bottle_drinkable", "inventory"));
        this.registerItem(Items.GLASS_BOTTLE, "glass_bottle");
        this.registerItem(Items.SPIDER_EYE, "spider_eye");
        this.registerItem(Items.FERMENTED_SPIDER_EYE, "fermented_spider_eye");
        this.registerItem(Items.BLAZE_POWDER, "blaze_powder");
        this.registerItem(Items.MAGMA_CREAM, "magma_cream");
        this.registerItem(Items.BREWING_STAND, "brewing_stand");
        this.registerItem(Items.CAULDRON, "cauldron");
        this.registerItem(Items.ENDER_EYE, "ender_eye");
        this.registerItem(Items.SPECKLED_MELON, "speckled_melon");
        this.itemModelMesher.register(Items.SPAWN_EGG, stack -> new ModelResourceLocation("spawn_egg", "inventory"));
        this.registerItem(Items.EXPERIENCE_BOTTLE, "experience_bottle");
        this.registerItem(Items.FIRE_CHARGE, "fire_charge");
        this.registerItem(Items.WRITABLE_BOOK, "writable_book");
        this.registerItem(Items.EMERALD, "emerald");
        this.registerItem(Items.ITEM_FRAME, "item_frame");
        this.registerItem(Items.FLOWER_POT, "flower_pot");
        this.registerItem(Items.CARROT, "carrot");
        this.registerItem(Items.POTATO, "potato");
        this.registerItem(Items.BAKED_POTATO, "baked_potato");
        this.registerItem(Items.POISONOUS_POTATO, "poisonous_potato");
        this.registerItem(Items.MAP, "map");
        this.registerItem(Items.GOLDEN_CARROT, "golden_carrot");
        this.registerItem(Items.SKULL, 0, "skull_skeleton");
        this.registerItem(Items.SKULL, 1, "skull_wither");
        this.registerItem(Items.SKULL, 2, "skull_zombie");
        this.registerItem(Items.SKULL, 3, "skull_char");
        this.registerItem(Items.SKULL, 4, "skull_creeper");
        this.registerItem(Items.CARROT_ON_A_STICK, "carrot_on_a_stick");
        this.registerItem(Items.NETHER_STAR, "nether_star");
        this.registerItem(Items.PUMPKIN_PIE, "pumpkin_pie");
        this.registerItem(Items.FIREWORK_CHARGE, "firework_charge");
        this.registerItem(Items.COMPARATOR, "comparator");
        this.registerItem(Items.NETHERBRICK, "netherbrick");
        this.registerItem(Items.QUARTZ, "quartz");
        this.registerItem(Items.TNT_MINECART, "tnt_minecart");
        this.registerItem(Items.HOPPER_MINECART, "hopper_minecart");
        this.registerItem(Items.ARMOR_STAND, "armor_stand");
        this.registerItem(Items.IRON_HORSE_ARMOR, "iron_horse_armor");
        this.registerItem(Items.GOLDEN_HORSE_ARMOR, "golden_horse_armor");
        this.registerItem(Items.DIAMOND_HORSE_ARMOR, "diamond_horse_armor");
        this.registerItem(Items.LEAD, "lead");
        this.registerItem(Items.NAME_TAG, "name_tag");
        this.itemModelMesher.register(Items.BANNER, stack -> new ModelResourceLocation("banner", "inventory"));
        this.registerItem(Items.RECORD_13, "record_13");
        this.registerItem(Items.RECORD_CAT, "record_cat");
        this.registerItem(Items.RECORD_BLOCKS, "record_blocks");
        this.registerItem(Items.RECORD_CHIRP, "record_chirp");
        this.registerItem(Items.RECORD_FAR, "record_far");
        this.registerItem(Items.RECORD_MALL, "record_mall");
        this.registerItem(Items.RECORD_MELLOHI, "record_mellohi");
        this.registerItem(Items.RECORD_STAL, "record_stal");
        this.registerItem(Items.RECORD_STRAD, "record_strad");
        this.registerItem(Items.RECORD_WARD, "record_ward");
        this.registerItem(Items.RECORD_11, "record_11");
        this.registerItem(Items.RECORD_WAIT, "record_wait");
        this.registerItem(Items.PRISMARINE_SHARD, "prismarine_shard");
        this.registerItem(Items.PRISMARINE_CRYSTALS, "prismarine_crystals");
        this.itemModelMesher.register(Items.ENCHANTED_BOOK, stack -> new ModelResourceLocation("enchanted_book", "inventory"));
        this.itemModelMesher.register(Items.FILLED_MAP, stack -> new ModelResourceLocation("filled_map", "inventory"));
        this.registerBlock(Blocks.COMMAND_BLOCK, "command_block");
        this.registerItem(Items.FIREWORKS, "fireworks");
        this.registerItem(Items.COMMAND_BLOCK_MINECART, "command_block_minecart");
        this.registerBlock(Blocks.BARRIER, "barrier");
        this.registerBlock(Blocks.MOB_SPAWNER, "mob_spawner");
        this.registerItem(Items.WRITTEN_BOOK, "written_book");
        this.registerBlock(Blocks.BROWN_MUSHROOM_BLOCK, BlockHugeMushroom.EnumType.ALL_INSIDE.getMetadata(), "brown_mushroom_block");
        this.registerBlock(Blocks.RED_MUSHROOM_BLOCK, BlockHugeMushroom.EnumType.ALL_INSIDE.getMetadata(), "red_mushroom_block");
        this.registerBlock(Blocks.DRAGON_EGG, "dragon_egg");
    }

    @Override
    public void onResourceManagerReload(IResourceManager resourceManager) {
        this.itemModelMesher.rebuildCache();
    }
}
