package net.radiant.start;

import net.minecraft.client.main.Main;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;

public class DevStart {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevStart.class);

    static void main(String[] args) {

        preventUpToDateHack();

        /*if (NativeImageExerciser.shouldExercise()) {
            ModernLogger.debug("You have the tracing agent attached, make sure you join both a server and a single player world at least once!");
        }*/

        try {
            Main.main(args);
        } catch (Throwable _) {
        }

        try {
            Thread.sleep(1300L);
        } catch (InterruptedException _) {
        }
    }

    private static void preventUpToDateHack() {
        // Makes sure a resource file is changed on every client run preventing the client from not running because
        // it's already "up to date".
        File file = new File("../modules/dev/src/main/resources/gradle_hack.txt");
        try (FileWriter stream = new FileWriter(file)) {
            stream.write(String.valueOf(System.currentTimeMillis()));
        } catch (Throwable e) {
            LOGGER.warn("Couldn't update gradle hack file", e);
        }
    }
}
