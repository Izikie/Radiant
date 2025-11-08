package run;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;

public class DevStart {

    private static final Logger LOGGER = LoggerFactory.getLogger(DevStart.class);

    public static void main(String[] args) {
        preventUpToDateHack();

        if (System.getProperty("radiant.exerciseClasses") != null) {
            LOGGER.debug("You have the tracing agent attached, make sure you join both a server and a single player world at least once!");
        }

        try {
            //Start.main(args);
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
        File file = new File("../modules/dev/src/main/resources/sammy_gradle_hack.txt");
        try (FileWriter stream = new FileWriter(file)) {
            stream.write(String.valueOf(System.currentTimeMillis()));
        } catch (Throwable throwable) {
            LOGGER.warn("Couldn't update gradle hack file", throwable);
        }
    }
}
