package net.optifine.entity.model;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.tileentity.TileEntity;
import net.optifine.Config;
import net.minecraft.util.ResourceLocation;
import net.optifine.Log;
import net.optifine.entity.model.anim.ModelResolver;
import net.optifine.entity.model.anim.ModelUpdater;

import java.io.IOException;
import java.util.*;

public class CustomEntityModels {
	private static boolean active = false;
	private static Map<Class, Render> originalEntityRenderMap = null;
	private static Map<Class<? extends TileEntity>, TileEntitySpecialRenderer<?>> originalTileEntityRenderMap = null;

	public static void update() {
		Map<Class, Render> map = getEntityRenderMap();
		Map<Class<? extends TileEntity>, TileEntitySpecialRenderer<?>> map1 = getTileEntityRenderMap();

		if (map == null) {
			Log.error("Entity render map not found, custom entity models are DISABLED.");
		} else if (map1 == null) {
			Log.error("Tile entity render map not found, custom entity models are DISABLED.");
		} else {
			active = false;
			map.clear();
			map1.clear();
			map.putAll(originalEntityRenderMap);
			map1.putAll(originalTileEntityRenderMap);

			if (Config.isCustomEntityModels()) {
				ResourceLocation[] aresourcelocation = getModelLocations();

				for (ResourceLocation resourcelocation : aresourcelocation) {
					Log.info("CustomEntityModel: " + resourcelocation.getResourcePath());
					IEntityRenderer ientityrenderer = parseEntityRender(resourcelocation);

					if (ientityrenderer != null) {
						Class oclass = ientityrenderer.getEntityClass();

						if (oclass != null) {
							if (ientityrenderer instanceof Render render) {
								map.put(oclass, render);
							} else if (ientityrenderer instanceof TileEntitySpecialRenderer tileEntitySpecialRenderer) {
								map1.put(oclass, tileEntitySpecialRenderer);
							} else {
								Log.error("Unknown renderer type: " + ientityrenderer.getClass().getName());
							}

							active = true;
						}
					}
				}
			}
		}
	}

	private static Map<Class, Render> getEntityRenderMap() {
		RenderManager rendermanager = Minecraft.get().getRenderManager();
		Map<Class, Render> map = rendermanager.getEntityRenderMap();

		if (map == null) {
			return null;
		} else {
			if (originalEntityRenderMap == null) {
				originalEntityRenderMap = new HashMap<>(map);
			}

			return map;
		}
	}

	private static Map<Class<? extends TileEntity>, TileEntitySpecialRenderer<?>> getTileEntityRenderMap() {
		Map<Class<? extends TileEntity>, TileEntitySpecialRenderer<?>> map = TileEntityRendererDispatcher.INSTANCE.mapSpecialRenderers;

		if (originalTileEntityRenderMap == null) {
			originalTileEntityRenderMap = new HashMap<>(map);
		}

		return map;
	}

	private static ResourceLocation[] getModelLocations() {
		String s = "optifine/cem/";
		String s1 = ".jem";
		List<ResourceLocation> list = new ArrayList<>();
		String[] astring = CustomModelRegistry.getModelNames();

		for (String s2 : astring) {
			String s3 = s + s2 + s1;
			ResourceLocation resourcelocation = new ResourceLocation(s3);

			if (Config.hasResource(resourcelocation)) {
				list.add(resourcelocation);
			}
		}

		return list.toArray(new ResourceLocation[0]);
	}

	private static IEntityRenderer parseEntityRender(ResourceLocation location) {
		try {
			JsonObject jsonobject = CustomEntityModelParser.loadJson(location);
			return parseEntityRender(jsonobject, location.getResourcePath());
		} catch (IOException exception) {
			Log.error(exception.getClass().getName() + ": " + exception.getMessage());
			return null;
		} catch (JsonParseException exception) {
			Log.error(exception.getClass().getName() + ": " + exception.getMessage());
			return null;
		} catch (Exception exception) {
			exception.printStackTrace();
			return null;
		}
	}

	private static IEntityRenderer parseEntityRender(JsonObject obj, String path) {
		CustomEntityRenderer customentityrenderer = CustomEntityModelParser.parseEntityRender(obj, path);
		String s = customentityrenderer.name();
		ModelAdapter modeladapter = CustomModelRegistry.getModelAdapter(s);
		checkNull(modeladapter, "Entity not found: " + s);
		Class oclass = modeladapter.getEntityClass();
		checkNull(oclass, "Entity class not found: " + s);
		IEntityRenderer ientityrenderer = makeEntityRender(modeladapter, customentityrenderer);

		if (ientityrenderer == null) {
			return null;
		} else {
			ientityrenderer.setEntityClass(oclass);
			return ientityrenderer;
		}
	}

	private static IEntityRenderer makeEntityRender(ModelAdapter modelAdapter, CustomEntityRenderer cer) {
		ResourceLocation resourcelocation = cer.textureLocation();
		CustomModelRenderer[] acustommodelrenderer = cer.customModelRenderers();
		float f = cer.shadowSize();

		if (f < 0.0F) {
			f = modelAdapter.getShadowSize();
		}

		ModelBase modelbase = modelAdapter.makeModel();

		if (modelbase == null) {
			return null;
		} else {
			ModelResolver modelresolver = new ModelResolver(modelAdapter, modelbase, acustommodelrenderer);

			if (!modifyModel(modelAdapter, modelbase, acustommodelrenderer, modelresolver)) {
				return null;
			} else {
				IEntityRenderer ientityrenderer = modelAdapter.makeEntityRender(modelbase, f);

				if (ientityrenderer == null) {
					throw new JsonParseException("Entity renderer is null, model: " + modelAdapter.getName() + ", adapter: " + modelAdapter.getClass().getName());
				} else {
					if (resourcelocation != null) {
						ientityrenderer.setLocationTextureCustom(resourcelocation);
					}

					return ientityrenderer;
				}
			}
		}
	}

	private static boolean modifyModel(ModelAdapter modelAdapter, ModelBase model, CustomModelRenderer[] modelRenderers, ModelResolver mr) {
		for (CustomModelRenderer custommodelrenderer : modelRenderers) {
			if (!modifyModel(modelAdapter, model, custommodelrenderer, mr)) {
				return false;
			}
		}

		return true;
	}

	private static boolean modifyModel(ModelAdapter modelAdapter, ModelBase model, CustomModelRenderer customModelRenderer, ModelResolver modelResolver) {
		String s = customModelRenderer.modelPart();
		ModelRenderer modelrenderer = modelAdapter.getModelRenderer(model, s);

		if (modelrenderer == null) {
			Log.error("Model part not found: " + s + ", model: " + model);
			return false;
		} else {
			if (!customModelRenderer.attach()) {
				if (modelrenderer.cubeList != null) {
					modelrenderer.cubeList.clear();
				}

				if (modelrenderer.spriteList != null) {
					modelrenderer.spriteList.clear();
				}

				if (modelrenderer.childModels != null) {
					ModelRenderer[] amodelrenderer = modelAdapter.getModelRenderers(model);
					Set<ModelRenderer> set = Collections.<ModelRenderer>newSetFromMap(new IdentityHashMap<>());
					set.addAll(Arrays.asList(amodelrenderer));
					List<ModelRenderer> list = modelrenderer.childModels;
					Iterator<ModelRenderer> iterator = list.iterator();

					while (iterator.hasNext()) {
						ModelRenderer modelrenderer1 = (ModelRenderer) iterator.next();

						if (!set.contains(modelrenderer1)) {
							iterator.remove();
						}
					}
				}
			}

			modelrenderer.addChild(customModelRenderer.modelRenderer());
			ModelUpdater modelupdater = customModelRenderer.modelUpdater();

			if (modelupdater != null) {
				modelResolver.setThisModelRenderer(customModelRenderer.modelRenderer());
				modelResolver.setPartModelRenderer(modelrenderer);

				if (!modelupdater.initialize(modelResolver)) {
					return false;
				}

				customModelRenderer.modelRenderer().setModelUpdater(modelupdater);
			}

			return true;
		}
	}

	private static void checkNull(Object obj, String msg) {
		if (obj == null) {
			throw new JsonParseException(msg);
		}
	}

	public static boolean isActive() {
		return active;
	}
}
