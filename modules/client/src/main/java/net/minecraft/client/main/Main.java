package net.minecraft.client.main;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.authlib.properties.PropertyMap;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import joptsimple.ValueConverter;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Session;
import net.radiant.nativeimage.NativeImageExerciser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.Proxy.Type;
import java.util.List;

public class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        System.setProperty("java.net.preferIPv4Stack", "true");
        System.setProperty("io.netty.allocator.maxOrder", "9"); // Default is 16MiB, Minecraft uses 2MiB, use 4MiB as safe default
        // Lowering maxOrder also lowers memory allocation

        if (!System.getProperty("os.arch").contains("64")) {
            LOGGER.error("❌ Radiant requires a 64-bit JVM to run.");
            System.exit(1);
        }

        OptionParser parser = new OptionParser();
        parser.allowsUnrecognizedOptions();

        // Proxy Info
        OptionSpec<String> optionProxyHost = parser.accepts("proxyHost").withRequiredArg();
        OptionSpec<Integer> optionProxyPort = parser.accepts("proxyPort").withRequiredArg().withValuesConvertedBy(new IntegerConverter()).defaultsTo(8080);
        OptionSpec<String> optionProxyUser = parser.accepts("proxyUser").withRequiredArg();
        OptionSpec<String> optionProxyPass = parser.accepts("proxyPass").withRequiredArg();

        // User Info
        OptionSpec<String> optionUsername = parser.accepts("username").withRequiredArg().defaultsTo("Player");
        OptionSpec<String> optionUuid = parser.accepts("uuid").withRequiredArg();
        OptionSpec<String> optionAccessToken = parser.accepts("accessToken").withRequiredArg().required();
        OptionSpec<String> optionUserProperties = parser.accepts("userProperties").withRequiredArg().defaultsTo("{}");
        OptionSpec<String> optionProfileProperties = parser.accepts("profileProperties").withRequiredArg().defaultsTo("{}");
        OptionSpec<String> optionUserType = parser.accepts("userType").withRequiredArg().defaultsTo("legacy");

        // Display Info
        OptionSpec<Integer> optionWidth = parser.accepts("width").withRequiredArg().withValuesConvertedBy(new IntegerConverter()).defaultsTo(854);
        OptionSpec<Integer> optionHeight = parser.accepts("height").withRequiredArg().withValuesConvertedBy(new IntegerConverter()).defaultsTo(480);
        parser.accepts("fullscreen");
        parser.accepts("checkGlErrors");

        // Folder Info
        OptionSpec<File> optionGameDir = parser.accepts("gameDir").withRequiredArg().withValuesConvertedBy(new FileConverter()).defaultsTo(new File("."));
        OptionSpec<File> optionResourcePackDir = parser.accepts("resourcePackDir").withRequiredArg().withValuesConvertedBy(new FileConverter());
        OptionSpec<File> optionAssetsDir = parser.accepts("assetsDir").withRequiredArg().withValuesConvertedBy(new FileConverter());

        // Server Info
        OptionSpec<String> optionServer = parser.accepts("server").withRequiredArg();
        OptionSpec<Integer> optionPort = parser.accepts("port").withRequiredArg().withValuesConvertedBy(new IntegerConverter()).defaultsTo(25565);

        OptionSpec<String> nonOptions = parser.nonOptions();
        OptionSet options = parser.parse(args);

        List<String> ignoredArgs = options.valuesOf(nonOptions);
        if (!ignoredArgs.isEmpty()) {
            LOGGER.warn("Completely ignored arguments: {}", ignoredArgs);
        }

        String proxyHost = options.valueOf(optionProxyHost);
        Proxy proxy = Proxy.NO_PROXY;

        if (proxyHost != null) {
            try {
                proxy = new Proxy(Type.SOCKS, new InetSocketAddress(proxyHost, options.valueOf(optionProxyPort)));
            } catch (Exception _) {

            }
        }

        final String proxyUser = options.valueOf(optionProxyUser);
        final String proxyPass = options.valueOf(optionProxyPass);

        if (!proxy.equals(Proxy.NO_PROXY) && isNotNullOrEmpty(proxyUser) && isNotNullOrEmpty(proxyPass)) {
            Authenticator.setDefault(new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(proxyUser, proxyPass.toCharArray());
                }
            });
        }

        Gson gson = new GsonBuilder()
                .registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer())
                .create();

        // User Info
        String playerID = options.has(optionUuid) ? optionUuid.value(options) : optionUsername.value(options);
        Session session = new Session(optionUsername.value(options), playerID, optionAccessToken.value(options), optionUserType.value(options));
        PropertyMap userProperties = gson.fromJson(options.valueOf(optionUserProperties), PropertyMap.class);
        PropertyMap profileProperties = gson.fromJson(options.valueOf(optionProfileProperties), PropertyMap.class);

        // Display Info
        int width = options.valueOf(optionWidth);
        int height = options.valueOf(optionHeight);
        boolean fullscreen = options.has("fullscreen");
        boolean checkGlErrors = options.has("checkGlErrors");

        // Folder Info
        File gameDir = options.valueOf(optionGameDir);
        File assetsDir = options.has(optionAssetsDir) ? options.valueOf(optionAssetsDir) : new File(gameDir, "assets/");
        File resourcePackDir = options.has(optionResourcePackDir) ? options.valueOf(optionResourcePackDir) : new File(gameDir, "resourcepacks/");

        // Server Info
        String server = options.valueOf(optionServer);
        Integer port = options.valueOf(optionPort);

        GameConfiguration gameConfiguration = new GameConfiguration(
                new GameConfiguration.UserInformation(session, userProperties, profileProperties, proxy),
                new GameConfiguration.DisplayInformation(width, height, fullscreen, checkGlErrors),
                new GameConfiguration.FolderInformation(gameDir, resourcePackDir, assetsDir),
                new GameConfiguration.ServerInformation(server, port)
        );

        Runtime.getRuntime().addShutdownHook(new Thread("Client Shutdown Thread") {
            @Override
            public void run() {
                Minecraft.stopIntegratedServer();
            }
        });
        Thread.currentThread().setName("Client thread");
        Minecraft minecraft = new Minecraft(gameConfiguration);
        if (System.getProperty("radiant.exerciseClasses") != null) {
            NativeImageExerciser.exercise();
        }
        minecraft.run();

    }

    private static boolean isNotNullOrEmpty(String str) {
        return str != null && !str.isEmpty();
    }

    private static class IntegerConverter implements ValueConverter<Integer> {
        @Override
        public Integer convert(String value) {
            return Integer.valueOf(value);
        }

        @Override
        public Class<? extends Integer> valueType() {
            return Integer.class;
        }

        @Override
        public String valuePattern() {
            return null;
        }
    }

    private static class FileConverter implements ValueConverter<File> {

        @Override
        public File convert(String value) {
            return new File(value);
        }

        @Override
        public Class<? extends File> valueType() {
            return File.class;
        }

        @Override
        public String valuePattern() {
            return null;
        }
    }
}
