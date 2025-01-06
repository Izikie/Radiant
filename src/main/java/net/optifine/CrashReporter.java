package net.optifine;

import net.minecraft.crash.CrashReport;
import net.minecraft.crash.CrashReportCategory;
import net.minecraft.src.Config;
import net.optifine.shaders.Shaders;

public class CrashReporter {
    public static void onCrashReport(CrashReport crashReport, CrashReportCategory category) {
        try {
            Throwable throwable = crashReport.getCrashCause();

            if (throwable == null || throwable.getClass() == Throwable.class) {
                return;
            }

            extendCrashReport(category);
        } catch (Exception exception) {
            Config.dbg(exception.getClass().getName() + ": " + exception.getMessage());
        }
    }

    public static void extendCrashReport(CrashReportCategory cat) {
        cat.addCrashSection("OptiFine Version/Build", Config.getVersion());

        if (Config.getGameSettings() != null) {
            cat.addCrashSection("Render Distance Chunks", Config.getChunkViewDistance());
            cat.addCrashSection("Mipmaps", Config.getMipmapLevels());
            cat.addCrashSection("Anisotropic Filtering", Config.getAnisotropicFilterLevel());
            cat.addCrashSection("Antialiasing", Config.getAntialiasingLevel());
            cat.addCrashSection("Multitexture", Config.isMultiTexture());
        }

        cat.addCrashSection("Shaders", Shaders.getShaderPackName());
        cat.addCrashSection("OpenGlVersion", Config.openGlVersion);
        cat.addCrashSection("OpenGlRenderer", Config.openGlRenderer);
        cat.addCrashSection("OpenGlVendor", Config.openGlVendor);
        cat.addCrashSection("CpuCount", Config.getAvailableProcessors());
    }
}
