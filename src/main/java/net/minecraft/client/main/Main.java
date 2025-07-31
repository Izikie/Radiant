package net.minecraft.client.main;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.converters.FileConverter;
import com.beust.jcommander.converters.IntegerConverter;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import com.mojang.authlib.properties.PropertyMap.Serializer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfiguration.DisplayInformation;
import net.minecraft.client.main.GameConfiguration.FolderInformation;
import net.minecraft.client.main.GameConfiguration.ServerInformation;
import net.minecraft.client.main.GameConfiguration.UserInformation;
import net.minecraft.util.Session;

import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.ArrayList;
import java.util.List;

public class Main {
    private static class MinecraftArgs {
        // User Information
        @Parameter(names = {"--username", "-username"}, description = "Username")
        private String username = "Player";

        @Parameter(names = {"--uuid", "-uuid"}, description = "UUID")
        private String uuid;

        @Parameter(names = {"--accessToken", "-accessToken"}, description = "Access token", required = true)
        private String accessToken;

        @Parameter(names = {"--userType", "-userType"}, description = "User type")
        private String userType = "legacy";

        @Parameter(names = {"--userProperties", "-userProperties"}, description = "User properties")
        private String userProperties = "{}";

        @Parameter(names = {"--profileProperties", "-profileProperties"}, description = "Profile properties")
        private String profileProperties = "{}";

        // Display Information
        @Parameter(names = {"--width", "-width"}, description = "Window width", converter = IntegerConverter.class)
        private Integer width = 854;

        @Parameter(names = {"--height", "-height"}, description = "Window height", converter = IntegerConverter.class)
        private Integer height = 480;

        @Parameter(names = {"--fullscreen", "-fullscreen"}, description = "Start in fullscreen mode")
        private boolean fullscreen = false;

        @Parameter(names = {"--checkGlErrors", "-checkGlErrors"}, description = "Check for OpenGL errors")
        private boolean checkGlErrors = false;

        // Folder Information
        @Parameter(names = {"--gameDir", "-gameDir"}, description = "Game directory", converter = FileConverter.class)
        private File gameDir = new File(".");

        @Parameter(names = {"--resourcePackDir", "-resourcePackDir"}, description = "Resource pack directory", converter = FileConverter.class)
        private File resourcePackDir;

        @Parameter(names = {"--assetsDir", "-assetsDir"}, description = "Assets directory", converter = FileConverter.class)
        private File assetsDir;

        // Server Information
        @Parameter(names = {"--server", "-server"}, description = "Server address")
        private String server;

        @Parameter(names = {"--port", "-port"}, description = "Server port", converter = IntegerConverter.class)
        private Integer port = 25565;

        // Proxy Information
        @Parameter(names = {"--proxyHost", "-proxyHost"}, description = "Proxy host")
        private String proxyHost;

        @Parameter(names = {"--proxyPort", "-proxyPort"}, description = "Proxy port", converter = IntegerConverter.class)
        private Integer proxyPort = 8080;

        @Parameter(names = {"--proxyUser", "-proxyUser"}, description = "Proxy username")
        private String proxyUser;

        @Parameter(names = {"--proxyPass", "-proxyPass"}, description = "Proxy password")
        private String proxyPass;

        // Other
        @Parameter(description = "Other arguments")
        private List<String> otherArgs = new ArrayList<>();
    }

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("log4j2.formatMsgNoLookups", "true");
        System.setProperty("io.netty.allocator.maxOrder", "9"); // Default is 16MiB, Minecraft uses 2MiB, use 4MiB as safe default

        MinecraftArgs minecraftArgs = new MinecraftArgs();
        JCommander jCommander = JCommander.newBuilder()
                .addObject(minecraftArgs)
                .allowParameterOverwriting(true)
                .programName("Minecraft")
                .build();

        try {
            jCommander.parse(args);
        } catch (Exception exception) {
            System.err.println("Error parsing command line arguments: " + exception.getMessage());
            jCommander.usage();
            return;
        }

        if (!minecraftArgs.otherArgs.isEmpty()) {
            System.out.println("Completely ignored arguments: " + minecraftArgs.otherArgs);
        }

        Proxy proxy = Proxy.NO_PROXY;
        if (isNotNullOrEmpty(minecraftArgs.proxyHost)) {
            try {
                proxy = new Proxy(Type.SOCKS, new InetSocketAddress(minecraftArgs.proxyHost, minecraftArgs.proxyPort));

                if (isNotNullOrEmpty(minecraftArgs.proxyUser) && isNotNullOrEmpty(minecraftArgs.proxyPass)) {
                    Authenticator.setDefault(new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(minecraftArgs.proxyUser, minecraftArgs.proxyPass.toCharArray());
                        }
                    });
                }
            } catch (Exception exception) {
                System.err.println("Error configuring proxy: " + exception.getMessage());
            }
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PropertyMap.class, new Serializer())
                .create();
        PropertyMap userProperties = gson.fromJson(minecraftArgs.userProperties, PropertyMap.class);
        PropertyMap profileProperties = gson.fromJson(minecraftArgs.profileProperties, PropertyMap.class);

        File gameDir = minecraftArgs.gameDir;
        File assetsDir = minecraftArgs.assetsDir != null ?
                minecraftArgs.assetsDir : new File(gameDir, "assets/");
        File resourcePackDir = minecraftArgs.resourcePackDir != null ?
                minecraftArgs.resourcePackDir : new File(gameDir, "resourcepacks/");

        String uuid = minecraftArgs.uuid != null ?
                minecraftArgs.uuid : minecraftArgs.username;

        Session session = new Session(
                minecraftArgs.username,
                uuid,
                minecraftArgs.accessToken,
                minecraftArgs.userType
        );

        GameConfiguration gameConfiguration = new GameConfiguration(
                new UserInformation(
                        session,
                        userProperties,
                        profileProperties,
                        proxy
                ),
                new DisplayInformation(
                        minecraftArgs.width,
                        minecraftArgs.height,
                        minecraftArgs.fullscreen,
                        minecraftArgs.checkGlErrors
                ),
                new FolderInformation(
                        gameDir,
                        resourcePackDir,
                        assetsDir
                ),
                new ServerInformation(
                        minecraftArgs.server,
                        minecraftArgs.port
                )
        );

        Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread") {
            @Override
            public void run() {
                Minecraft.stopIntegratedServer();
            }
        });

        Thread.currentThread().setName("Client Thread");
        new Minecraft(gameConfiguration).run();
    }

    private static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }
}
