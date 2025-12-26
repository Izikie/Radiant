package net.radiant.dev.start;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.main.Main;
import net.radiant.dev.DevGuiMainMenu;
import net.radiant.dev.nativeimage.NativeImageExerciser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;

public class LaunchWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(LaunchWrapper.class);

    private LaunchWrapper() {
    }

    static void main(String[] args) {
        preventUpToDateHack();
        startHotswapHook();

        NativeImageExerciser.exercise();

        try {
            Main.main(args);
        } catch (Exception e) {
            LOGGER.error("Failed to launch Minecraft", e);
        }

        try {
            Thread.sleep(1300L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error("DevStart sleep interrupted", e);
        }
    }

    private static void startHotswapHook() {
        Thread thread = new Thread(() -> {
            try {
                while (true) {
                    Thread.sleep(50);

                    Minecraft mc = Minecraft.get();
                    if (mc != null && mc.currentScreen != null &&
                            mc.currentScreen.getClass() == GuiMainMenu.class) {

                        mc.addScheduledTask(() -> {
                            if (mc.currentScreen != null && mc.currentScreen.getClass() == GuiMainMenu.class) {
                                mc.displayGuiScreen(new DevGuiMainMenu());
                                LOGGER.info("Hotswapped GuiMainMenu to DevGuiMainMenu on Main Thread");
                            }
                        });
                    }
                }
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                LOGGER.error("Dev hook encountered an error", e);
            }
        });
        thread.setDaemon(true);
        thread.setName("Dev-Hotswap-Watcher");
        thread.start();
    }

    // Prevents Gradle's up-to-date check from skipping the client run task
    private static void preventUpToDateHack() {
        File file = new File("../modules/dev/src/main/resources/gradle_hack.txt");

        try (FileWriter stream = new FileWriter(file)) {
            stream.write(String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            LOGGER.warn("Couldn't update gradle hack file", e);
        }
    }
}
