package net.minecraft.crash;

import net.minecraft.client.Minecraft;
import net.minecraft.src.Config;
import net.minecraft.world.gen.layer.IntCache;
import net.optifine.Log;
import net.optifine.shaders.Shaders;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

public class CrashReport {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrashReport.class);
    private final String description;
    private final Throwable throwable;
    private final CrashReportCategory theReportCategory = new CrashReportCategory(this, "System Details");
    private final List<CrashReportCategory> crashReportSections = new ArrayList<>();
    private File crashReportFile;
    private boolean firstCategoryInCrashReport = true;
    private StackTraceElement[] stackTrace = new StackTraceElement[0];
    String[] comments = new String[]{"Who set us up the TNT?", "Everything's going to plan. No, really, that was supposed to happen.", "Uh... Did I do that?", "Oops.", "Why did you do that?", "I feel sad now :(", "My bad.", "I'm sorry, Dave.", "I let you down. Sorry :(", "On the bright side, I bought you a teddy bear!", "Daisy, daisy...", "Oh - I know what I did wrong!", "Hey, that tickles! Hehehe!", "I blame Dinnerbone.", "You should try our sister game, Minceraft!", "Don't be sad. I'll do better next time, I promise!", "Don't be sad, have a hug! <3", "I just don't know what went wrong :(", "Shall we play a game?", "Quite honestly, I wouldn't worry myself about that.", "I bet Cylons wouldn't have this problem.", "Sorry :(", "Surprise! Haha. Well, this is awkward.", "Would you like a cupcake?", "Hi. I'm Minecraft, and I'm a crashaholic.", "Ooh. Shiny.", "This doesn't make any sense!", "Why is it breaking :(", "Don't do that.", "Ouch. That hurt :(", "You're mean.", "This is a token for 1 free hug. Redeem at your nearest Mojangsta: [~~HUG~~]", "There are four lights!", "But it works on my machine."};

    public CrashReport(String descriptionIn, Throwable throwable) {
        this.description = descriptionIn;
        this.throwable = throwable;
        this.populateEnvironment();
    }

    private void populateEnvironment() {
        this.theReportCategory.addCrashSectionCallable("Minecraft Version", () -> "1.8.9");
        this.theReportCategory.addCrashSectionCallable("Operating System", () -> System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ") version " + System.getProperty("os.version"));
        this.theReportCategory.addCrashSectionCallable("Java Version", () -> System.getProperty("java.version") + ", " + System.getProperty("java.vendor"));
        this.theReportCategory.addCrashSectionCallable("Java VM Version", () -> System.getProperty("java.vm.name") + " (" + System.getProperty("java.vm.info") + "), " + System.getProperty("java.vm.vendor"));
        this.theReportCategory.addCrashSectionCallable("Memory", () -> {
            Runtime runtime = Runtime.getRuntime();
            long maxMemory = runtime.maxMemory();
            long totalMemory = runtime.totalMemory();
            long freeMemory = runtime.freeMemory();
            long maxMemoryMB = maxMemory / 1024L / 1024L;
            long totalMemoryMB = totalMemory / 1024L / 1024L;
            long freeMemoryMB = freeMemory / 1024L / 1024L;
            return freeMemory + " bytes (" + freeMemoryMB + " MB) / " + totalMemory + " bytes (" + totalMemoryMB + " MB) up to " + maxMemory + " bytes (" + maxMemoryMB + " MB)";
        });
        this.theReportCategory.addCrashSectionCallable("JVM Flags", () -> {
            RuntimeMXBean runtimeMXBean = ManagementFactory.getRuntimeMXBean();
            List<String> inputArguments = runtimeMXBean.getInputArguments();

            return inputArguments.stream()
                    .filter(s -> s.startsWith("-X"))
                    .collect(Collectors.joining(" ", inputArguments.size() + " total; ", ""));
        });
        this.theReportCategory.addCrashSectionCallable("IntCache", IntCache::getCacheSizes);
    }

    public void getSectionsInStringBuilder(StringBuilder builder) {
        if ((this.stackTrace == null || this.stackTrace.length == 0) && !this.crashReportSections.isEmpty()) {
            this.stackTrace = ArrayUtils.subarray(this.crashReportSections.getFirst().getStackTrace(), 0, 1);
        }

        if (this.stackTrace != null && this.stackTrace.length > 0) {
            builder.append("-- Head --\n");
            builder.append("Stacktrace:\n");

            for (StackTraceElement element : this.stackTrace) {
                builder.append("\tat ").append(element.toString());
                builder.append("\n");
            }

            builder.append("\n");
        }

        for (CrashReportCategory category : this.crashReportSections) {
            category.appendToStringBuilder(builder);
            builder.append("\n\n");
        }

        this.theReportCategory.appendToStringBuilder(builder);
    }

    public String getCauseStackTraceOrString() {
        Throwable throwable = this.throwable;

        if (throwable.getMessage() == null) {
            switch (throwable) {
                case NullPointerException ignore -> throwable = new NullPointerException(this.description);
                case StackOverflowError ignore -> throwable = new StackOverflowError(this.description);
                case OutOfMemoryError ignore -> throwable = new OutOfMemoryError(this.description);
                default -> {
                }
            }
            throwable.setStackTrace(this.throwable.getStackTrace());
        }

        try (StringWriter stringWriter = new StringWriter();
             PrintWriter printWriter = new PrintWriter(stringWriter)) {
            throwable.printStackTrace(printWriter);
            return stringWriter.toString();
        } catch (IOException exception) {
            return "Failed to get stack trace: " + exception.getMessage();
        }
    }

    public boolean saveToFile(File toFile) {
        if (this.crashReportFile != null) {
            return false;
        } else {
            if (toFile.getParentFile() != null) {
                toFile.getParentFile().mkdirs();
            }

            try (FileWriter filewriter = new FileWriter(toFile)) {
                filewriter.write(this.getCompleteReport());
                this.crashReportFile = toFile;
                return true;
            } catch (IOException exception) {
                LOGGER.error("Could not save crash report to {}", toFile, exception);
                return false;
            }
        }
    }

    public void onOptifineData() {
        try {
            this.theReportCategory.addCrashSection("OptiFine Version/Build", Config.VERSION);

            if (Config.getGameSettings() != null) {
                this.theReportCategory.addCrashSection("Render Distance Chunks", Config.getChunkViewDistance());
                this.theReportCategory.addCrashSection("Mipmaps", Config.getMipmapLevels());
                this.theReportCategory.addCrashSection("Anisotropic Filtering", Config.getAnisotropicFilterLevel());
                this.theReportCategory.addCrashSection("Antialiasing", Config.getAntialiasingLevel());
                this.theReportCategory.addCrashSection("Multitexture", Config.isMultiTexture());
            }

            this.theReportCategory.addCrashSection("Shaders", Shaders.getShaderPackName());
            this.theReportCategory.addCrashSection("OpenGlVersion", Config.openGlVersion);
            this.theReportCategory.addCrashSection("OpenGlRenderer", Config.openGlRenderer);
            this.theReportCategory.addCrashSection("OpenGlVendor", Config.openGlVendor);
            this.theReportCategory.addCrashSection("CpuCount", Config.getAvailableProcessors());
        } catch (Exception exception) {
            Log.info(exception.getClass().getName() + ": " + exception.getMessage());
        }
    }

    public String getCompleteReport() {
        onOptifineData();
        StringBuilder builder = new StringBuilder();
        builder.append("---- Minecraft Crash Report ----\n");
        builder.append(getWittyComment());
        builder.append("\n\n");
        builder.append("Time: ");
        builder.append((new SimpleDateFormat()).format(new Date()));
        builder.append("\n");
        builder.append("Description: ");
        builder.append(this.description);
        builder.append("\n\n");
        builder.append(this.getCauseStackTraceOrString());
        builder.append("\n\nA detailed walkthrough of the error, its code path and all known details is as follows:\n");

        builder.repeat("-", 87);

        builder.append("\n\n");
        this.getSectionsInStringBuilder(builder);
        return builder.toString();
    }

    public CrashReportCategory getCategory() {
        return this.theReportCategory;
    }

    public String getDescription() {
        return this.description;
    }

    public Throwable getCrashCause() {
        return this.throwable;
    }

    public File getFile() {
        return this.crashReportFile;
    }

    public CrashReportCategory makeCategory(String name) {
        CrashReportCategory category = new CrashReportCategory(this, name);

        if (this.firstCategoryInCrashReport) {
            int i = category.getPrunedStackTrace(1);
            StackTraceElement[] astacktraceelement = this.throwable.getStackTrace();
            StackTraceElement stacktraceelement = null;
            StackTraceElement stacktraceelement1 = null;
            int j = astacktraceelement.length - i;

            if (j < 0) {
                LOGGER.warn("Negative index in crash report handler ({} / {})", astacktraceelement.length, i);
            }

            if (astacktraceelement != null && 0 <= j && j < astacktraceelement.length) {
                stacktraceelement = astacktraceelement[j];

                if (astacktraceelement.length + 1 - i < astacktraceelement.length) {
                    stacktraceelement1 = astacktraceelement[astacktraceelement.length + 1 - i];
                }
            }

            this.firstCategoryInCrashReport = category.firstTwoElementsOfStackTraceMatch(stacktraceelement, stacktraceelement1);

            if (i > 0 && !this.crashReportSections.isEmpty()) {
                CrashReportCategory category1 = this.crashReportSections.getLast();
                category1.trimStackTraceEntriesFromBottom(i);
            } else if (astacktraceelement != null && astacktraceelement.length >= i && 0 <= j && j < astacktraceelement.length) {
                this.stackTrace = new StackTraceElement[j];
                System.arraycopy(astacktraceelement, 0, this.stackTrace, 0, this.stackTrace.length);
            } else {
                this.firstCategoryInCrashReport = false;
            }
        }

        this.crashReportSections.add(category);
        return category;
    }

    private String getWittyComment() {
        try {
            return comments[Minecraft.RANDOM.nextInt(comments.length)];
        } catch (Throwable ignore) {
            return "Witty comment unavailable :(";
        }
    }

    public static CrashReport makeCrashReport(Throwable cause, String description) {
        if (cause instanceof ReportedException reportedException) {
            return reportedException.getCrashReport();
        } else {
            return new CrashReport(description, cause);
        }
    }
}
