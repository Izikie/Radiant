package net.minecraft.client.resources;

import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;
import net.minecraft.client.resources.data.IMetadataSerializer;
import net.minecraft.util.ResourceLocation;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.*;

public class SimpleReloadableResourceManager implements IReloadableResourceManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleReloadableResourceManager.class);
    private static final Joiner JOINER_RESOURCE_PACKS = Joiner.on(", ");
    private final Map<String, FallbackResourceManager> domainResourceManagers = new HashMap<>();
    private final List<IResourceManagerReloadListener> reloadListeners = new ArrayList<>();
    private final Set<String> setResourceDomains = new LinkedHashSet<>();
    private final IMetadataSerializer rmMetadataSerializer;

    public SimpleReloadableResourceManager(IMetadataSerializer rmMetadataSerializerIn) {
        this.rmMetadataSerializer = rmMetadataSerializerIn;
    }

    public void reloadResourcePack(IResourcePack resourcePack) {
        for (String s : resourcePack.getResourceDomains()) {
            this.setResourceDomains.add(s);
            FallbackResourceManager fallbackresourcemanager = this.domainResourceManagers.get(s);

            if (fallbackresourcemanager == null) {
                fallbackresourcemanager = new FallbackResourceManager(this.rmMetadataSerializer);
                this.domainResourceManagers.put(s, fallbackresourcemanager);
            }

            fallbackresourcemanager.addResourcePack(resourcePack);
        }
    }

    @Override
    public Set<String> getResourceDomains() {
        return this.setResourceDomains;
    }

    @Override
    public IResource getResource(ResourceLocation location) throws IOException {
        IResourceManager iresourcemanager = this.domainResourceManagers.get(location.getResourceDomain());

        if (iresourcemanager != null) {
            return iresourcemanager.getResource(location);
        } else {
            throw new FileNotFoundException(location.toString());
        }
    }

    @Override
    public List<IResource> getAllResources(ResourceLocation location) throws IOException {
        IResourceManager iresourcemanager = this.domainResourceManagers.get(location.getResourceDomain());

        if (iresourcemanager != null) {
            return iresourcemanager.getAllResources(location);
        } else {
            throw new FileNotFoundException(location.toString());
        }
    }

    private void clearResources() {
        this.domainResourceManagers.clear();
        this.setResourceDomains.clear();
    }

    @Override
    public void reloadResources(List<IResourcePack> resourcesPacksList) {
        this.clearResources();
        LOGGER.info("Reloading ResourceManager: {}", JOINER_RESOURCE_PACKS.join(Iterables.transform(resourcesPacksList, IResourcePack::getPackName)));

        for (IResourcePack iresourcepack : resourcesPacksList) {
            this.reloadResourcePack(iresourcepack);
        }

        this.notifyReloadListeners();
    }

    @Override
    public void registerReloadListener(IResourceManagerReloadListener reloadListener) {
        this.reloadListeners.add(reloadListener);
        reloadListener.onResourceManagerReload(this);
    }

    private void notifyReloadListeners() {
        for (IResourceManagerReloadListener iresourcemanagerreloadlistener : this.reloadListeners) {
            iresourcemanagerreloadlistener.onResourceManagerReload(this);
        }
    }
}
