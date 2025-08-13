package net.radiant.nativeimage;

import com.google.gson.Gson;
import com.mojang.authlib.yggdrasil.response.*;
import fr.litarvan.openauth.microsoft.model.request.MinecraftLoginRequest;
import fr.litarvan.openauth.microsoft.model.request.XSTSAuthorizationProperties;
import fr.litarvan.openauth.microsoft.model.request.XboxLiveLoginProperties;
import fr.litarvan.openauth.microsoft.model.request.XboxLoginRequest;
import fr.litarvan.openauth.microsoft.model.response.*;
import fr.litarvan.openauth.model.request.*;
import fr.litarvan.openauth.model.response.AuthResponse;
import io.netty.channel.AbstractChannel;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.local.LocalServerChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.network.NetworkState;
import net.minecraft.scoreboard.ScoreboardSaveData;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.village.VillageCollection;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;
import net.minecraft.world.WorldSettings;
import net.minecraft.world.WorldType;
import net.minecraft.world.gen.structure.MapGenStructureData;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraft.world.storage.MapData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class NativeImageExerciser {

    private static final Logger LOGGER = LoggerFactory.getLogger(NativeImageExerciser.class);

    private static int resourceCount;

    public static void exercise() {
        LOGGER.info("Exercising classes");

        loadAllResources();
        loadGsonClasses();
        loadAllEntityClasses();

        if (resourceCount < 256) {
            throw new RuntimeException("Not enough resources exercised. Are you running in the right directory?");
        }

        // These load packets reflectively on clinit, <3 final
        NetworkState.getById(0);
    }

    private static void loadGsonClasses() {
        int loaded = 0;

        List<Class<?>> classes = List.of(
            // Add any other classes from mods in here
        );

        List<Class<?>> initClasses = new ArrayList<>(classes);
        initClasses.addAll(List.of(
                Response.class,
                HasJoinedMinecraftServerResponse.class,
                MinecraftProfilePropertiesResponse.class,
                MinecraftTexturesPayload.class,
                ProfileSearchResultsResponse.class,
                RefreshResponse.class,
                User.class,

                MinecraftLoginRequest.class,
                XboxLiveLoginProperties.class,
                XboxLoginRequest.class,
                XSTSAuthorizationProperties.class,
                MicrosoftRefreshResponse.class,
                MinecraftLoginResponse.class,
                MinecraftProfile.class,
                MinecraftProfile.MinecraftSkin.class,
                MinecraftStoreResponse.class,
                MinecraftStoreResponse.StoreProduct.class,
                XboxLoginResponse.class,
                XboxLoginResponse.XboxLiveLoginResponseClaims.class,
                XboxLoginResponse.XboxLiveUserInfo.class,
                AuthRequest.class,
                InvalidateRequest.class,
                RefreshRequest.class,
                SignoutRequest.class,
                ValidateRequest.class,
                AuthResponse.class,
                RefreshResponse.class
        ));

        for (Class<?> clazz : initClasses) {
            try {
                Gson gson = new Gson();
                Object o = tryConstruct(clazz);
                gson.fromJson(gson.toJson(o), clazz);
                ++loaded;
            } catch (Throwable throwable) {
                throw new RuntimeException("Couldn't exercise: " + clazz.getName(), throwable);
            }
        }

        LOGGER.info("Marked {} classes for GSON serialization", loaded);
    }

    private static Object tryConstruct(Class<?> clazz) throws ReflectiveOperationException {
        try {
            return clazz.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException _) {}

        for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {
            try {
                List<Object> paramsList = new ArrayList<>();

                for (Class<?> parameter : constructor.getParameterTypes()) {
                    if (Number.class.isAssignableFrom(parameter) || parameter.isPrimitive()) {
                        paramsList.add(0);
                    } else {
                        paramsList.add(null);
                    }
                }
                Object[] params = paramsList.toArray(Object[]::new);
                return constructor.newInstance(params);
            } catch (ReflectiveOperationException _) {}
        }

        throw new NoSuchMethodException("No working constructor found in " + clazz);
    }

    private static void loadAllEntityClasses() {
        List<Class<? extends AbstractChannel>> nio = List.of(LocalServerChannel.class, EpollSocketChannel.class, NioSocketChannel.class, EpollServerSocketChannel.class, NioServerSocketChannel.class);
        AtomicInteger count = new AtomicInteger(0);

        for (Class<? extends AbstractChannel> aClass : nio) {
            Constructor<?>[] constructors = aClass.getConstructors();
            for (Constructor<?> constructor : constructors) {
                try {
                    constructor.newInstance(new Object[constructor.getParameterCount()]);
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    //new RuntimeException(e).printStackTrace();
                } catch (Throwable throwable) {
                    // Ignore
                    LOGGER.warn("Woke {} up but had error", aClass.getName());
                }
            }
        }
        wakeUpClassCollection(TileEntity.nameToClassMap.values(), count);
        wakeUpClassCollection(MapGenStructureIO.startNameToClassMap.values(), count);
        wakeUpClassCollection(MapGenStructureIO.componentClassToNameMap.keySet(), count);
        wakeUpClassCollection(MapGenStructureIO.startClassToNameMap.keySet(), count);
        wakeUpClassCollection(MapGenStructureIO.componentNameToClassMap.values(), count);
        wakeUpEntityCollection(EntityList.classToIDMapping.keySet(), count);
        wakeUpConstructor(count, VillageCollection.class, "");
        wakeUpConstructor(count, MapGenStructureData.class, "");
        wakeUpConstructor(count, MapData.class, "");
        wakeUpConstructor(count, ScoreboardSaveData.class, "");
        LOGGER.info("Woke {} classes up", count.get());
    }

    private static void wakeUpEntityCollection(Set<Class<? extends Entity>> classes, AtomicInteger count) {
        WorldClient worldClient = new WorldClient(null, new WorldSettings(0, WorldSettings.GameType.SURVIVAL, true, true, WorldType.DEFAULT), 0, Difficulty.HARD);
        for (Class<? extends Entity> value : classes) {
            if (Modifier.isAbstract(value.getModifiers())) {
                continue;
            }
            try {
                Constructor<? extends Entity> constructor = value.getConstructor(World.class);
                constructor.setAccessible(true);
                constructor.newInstance(worldClient);
            } catch (InstantiationException | IllegalAccessException | NoSuchMethodException |
                     InvocationTargetException e) {
                throw new RuntimeException("Waking up " + value.getName(), e);
            }
            count.incrementAndGet();
        }
    }

    private static void wakeUpClassCollection(Iterable<? extends Class<?>> values, AtomicInteger count) {
        for (Class<?> clazz : values) {
            try {
                clazz.getDeclaredConstructor().newInstance();
            } catch (Throwable e) {
                throw new RuntimeException("Waking up " + clazz.getName(), e);
            }
            count.incrementAndGet();
        }
    }

    private static void wakeUpConstructor(AtomicInteger count, Class<?> clazz, Object... args) {
        try {
            Class<?>[] params = Arrays.stream(args).map(Object::getClass).toArray(Class[]::new);
            Constructor<?> constructor = clazz.getDeclaredConstructor(params);
            constructor.newInstance(args);
        } catch (Throwable throwable) {
            throw new RuntimeException("Waking up " + clazz.getName(), throwable);
        }

        count.incrementAndGet();
    }

    private static void loadAllResources() {
        recurseResource(new File("../src/main/resources"));
        LOGGER.info("Woke {} resources up", resourceCount);
    }

    private static void recurseResource(File dirFile) {
        File[] files = dirFile.listFiles();
        if (files == null) {
            return;
        }

        String pattern = "src/main/resources/".replace('/', File.separatorChar);
        for (File file : files) {
            if (file.isDirectory()) {
                recurseResource(file);
                continue;
            }

            String absolutePath = file.getAbsolutePath();
            absolutePath = absolutePath.substring(absolutePath.lastIndexOf(pattern) + pattern.length());
            String absolutePathSep = absolutePath.replace(File.separatorChar, '/');
            if (absolutePathSep.contains("META-INF/native-image")) {
                continue;
            }

            loadResource(absolutePathSep);
            ++resourceCount;
        }
    }

    private static void loadResource(String path) {
        try (InputStream stream = NativeImageExerciser.class.getClassLoader().getResourceAsStream(path)) {
            Objects.requireNonNull(stream, path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
