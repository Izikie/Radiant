package net.radiant.logger;

import org.jetbrains.annotations.Nullable;
import org.slf4j.event.Level;

import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.locks.ReentrantLock;
import java.util.zip.GZIPOutputStream;

public class RadiantLoggerFile {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd-");

    private static final File LOGS_DIR = new File("logs");
    private static final File LATEST = new File(LOGS_DIR, "latest.log");
    private final ReentrantLock lock = new ReentrantLock();
    private FileWriter writer;

    public void init() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::shutdown));

        try {
            LOGS_DIR.mkdirs();
            LATEST.createNewFile();
            this.writer = new FileWriter(LATEST);
        } catch (IOException e) {
            throw new LoggerError("Failed to initialize logger file", e);
        }
    }

    public void log(Level level, String msg, String time, String thread, @Nullable Throwable t) {
        if (writer == null) {
            throw new LoggerError("Logger file used before initialization");
        }

        String output = String.format("[%s] [%s/%s]: %s\n", time, thread, level.name(), msg);
        printSafe(output, t);
    }

    private void printSafe(String output, @Nullable Throwable t) {
        this.lock.lock();
        try {
            this.writer.write(output);

            if (t != null) {
                PrintWriter wrapped = new PrintWriter(this.writer);
                t.printStackTrace(wrapped);
            }
        } catch (IOException e) {
            throw new LoggerError(e);
        } finally {
            this.lock.unlock();
        }
    }

    public void shutdown() {
        if (this.writer != null) {
            try {
                this.writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        this.gzipFile();
    }

    private void gzipFile() {
        String date = DATE_FORMAT.format(LocalDateTime.now());

        File file = null;
        int index = 1;
        while (index < Integer.MAX_VALUE) {
            File testFile = new File(LOGS_DIR, date + index + ".log.gz");
            if (testFile.exists()) {
                index++;
            } else {
                file = testFile;
                break;
            }
        }
        if (file == null) {
            return;
        }

        try (
                FileInputStream fis = new FileInputStream(LATEST);
                FileOutputStream fos = new FileOutputStream(file);
                GZIPOutputStream gos = new GZIPOutputStream(fos)
        ) {
            byte[] buffer = new byte[0x2000]; // 8 KB buffer
            int length;
            while ((length = fis.read(buffer)) != -1) {
                gos.write(buffer, 0, length);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
