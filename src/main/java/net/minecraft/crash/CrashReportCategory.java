package net.minecraft.crash;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

public class CrashReportCategory {
    private final CrashReport crashReport;
    private final String name;
    private final List<Entry> children = new ArrayList<>();
    private StackTraceElement[] stackTrace = new StackTraceElement[0];

    public CrashReportCategory(CrashReport report, String name) {
        this.crashReport = report;
        this.name = name;
    }

    public static String getCoordinateInfo(double x, double y, double z) {
        return String.format("%.2f,%.2f,%.2f - %s", x, y, z, getCoordinateInfo(new BlockPos(x, y, z)));
    }

    public static String getCoordinateInfo(BlockPos pos) {
        int x = pos.getX();
        int y = pos.getY();
        int z = pos.getZ();
        StringBuilder builder = new StringBuilder();

        try {
            builder.append(String.format("World: (%d,%d,%d)", x, y, z));
        } catch (Throwable ignore) {
            builder.append("(Error finding world loc)");
        }

        builder.append(", ");

        try {
            int l = x >> 4;
            int i1 = z >> 4;
            int j1 = x & 15;
            int k1 = y >> 4;
            int l1 = z & 15;
            int i2 = l << 4;
            int j2 = i1 << 4;
            int k2 = (l + 1 << 4) - 1;
            int l2 = (i1 + 1 << 4) - 1;
            builder.append(String.format("Chunk: (at %d,%d,%d in %d,%d; contains blocks %d,0,%d to %d,255,%d)", j1, k1, l1, l, i1, i2, j2, k2, l2));
        } catch (Throwable ignore) {
            builder.append("(Error finding chunk loc)");
        }

        builder.append(", ");

        try {
            int j3 = x >> 9;
            int k3 = z >> 9;
            int l3 = j3 << 5;
            int i4 = k3 << 5;
            int j4 = (j3 + 1 << 5) - 1;
            int k4 = (k3 + 1 << 5) - 1;
            int l4 = j3 << 9;
            int i5 = k3 << 9;
            int j5 = (j3 + 1 << 9) - 1;
            int i3 = (k3 + 1 << 9) - 1;
            builder.append(String.format("Region: (%d,%d; contains chunks %d,%d to %d,%d, blocks %d,0,%d to %d,255,%d)", j3, k3, l3, i4, j4, k4, l4, i5, j5, i3));
        } catch (Throwable ignore) {
            builder.append("(Error finding world loc)");
        }

        return builder.toString();
    }

    public void addCrashSectionCallable(String sectionName, Callable<String> callable) {
        try {
            this.addCrashSection(sectionName, callable.call());
        } catch (Throwable throwable) {
            this.addCrashSectionThrowable(sectionName, throwable);
        }
    }

    public void addCrashSection(String sectionName, Object value) {
        this.children.add(new Entry(sectionName, value));
    }

    public void addCrashSectionThrowable(String sectionName, Throwable throwable) {
        this.addCrashSection(sectionName, throwable);
    }

    public int getPrunedStackTrace(int size) {
        StackTraceElement[] astacktraceelement = Thread.currentThread().getStackTrace();

        if (astacktraceelement.length == 0) {
            return 0;
        } else {
            this.stackTrace = new StackTraceElement[astacktraceelement.length - 3 - size];
            System.arraycopy(astacktraceelement, 3 + size, this.stackTrace, 0, this.stackTrace.length);
            return this.stackTrace.length;
        }
    }

    public boolean firstTwoElementsOfStackTraceMatch(StackTraceElement s1, StackTraceElement s2) {
        if (this.stackTrace.length != 0 && s1 != null) {
            StackTraceElement stacktraceelement = this.stackTrace[0];

            if (stacktraceelement.isNativeMethod() == s1.isNativeMethod() && stacktraceelement.getClassName().equals(s1.getClassName()) && stacktraceelement.getFileName().equals(s1.getFileName()) && stacktraceelement.getMethodName().equals(s1.getMethodName())) {
                if (s2 == null == this.stackTrace.length > 1) {
                    return false;
                } else if (s2 != null && !this.stackTrace[1].equals(s2)) {
                    return false;
                } else {
                    this.stackTrace[0] = s1;
                    return true;
                }
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    public void trimStackTraceEntriesFromBottom(int amount) {
        StackTraceElement[] astacktraceelement = new StackTraceElement[this.stackTrace.length - amount];
        System.arraycopy(this.stackTrace, 0, astacktraceelement, 0, astacktraceelement.length);
        this.stackTrace = astacktraceelement;
    }

    public void appendToStringBuilder(StringBuilder builder) {
        builder.append("-- ").append(this.name).append(" --\n");
        builder.append("Details:");

        for (Entry entry : this.children) {
            builder.append("\n\t");
            builder.append(entry.getKey());
            builder.append(": ");
            builder.append(entry.getValue());
        }

        if (this.stackTrace != null && this.stackTrace.length > 0) {
            builder.append("\nStacktrace:");

            for (StackTraceElement stacktraceelement : this.stackTrace) {
                builder.append("\n\tat ");
                builder.append(stacktraceelement.toString());
            }
        }
    }

    public StackTraceElement[] getStackTrace() {
        return this.stackTrace;
    }

    public static void addBlockInfo(CrashReportCategory category, final BlockPos pos, final Block blockIn, final int blockData) {
        final int blockID = Block.getIdFromBlock(blockIn);
        category.addCrashSectionCallable("Block Type", () -> {
            try {
                return String.format("ID #%d (%s // %s)", blockID, blockIn.getUnlocalizedName(), blockIn.getClass().getCanonicalName());
            } catch (Throwable ignore) {
                return "ID #" + blockID;
            }
        });
        category.addCrashSectionCallable("Block Data Value", () -> {
            if (blockData < 0) {
                return "Unknown? (Got " + blockData + ")";
            } else {
                String binaryString = String.format("%4s", Integer.toBinaryString(blockData)).replace(" ", "0");
                return String.format("%1$d / 0x%1$X / 0b%2$s", blockData, binaryString);
            }
        });
        category.addCrashSectionCallable("Block Location", () -> CrashReportCategory.getCoordinateInfo(pos));
    }

    public static void addBlockInfo(CrashReportCategory category, final BlockPos pos, final IBlockState state) {
        category.addCrashSectionCallable("Block", state::toString);
        category.addCrashSectionCallable("Block Location", () -> CrashReportCategory.getCoordinateInfo(pos));
    }

    static class Entry {
        private final String key;
        private final String value;

        public Entry(String key, Object value) {
            this.key = key;

            if (value == null) {
                this.value = "~~NULL~~";
            } else if (value instanceof Throwable throwable) {
                this.value = "~~ERROR~~ " + throwable.getClass().getSimpleName() + ": " + throwable.getMessage();
            } else {
                this.value = value.toString();
            }
        }

        public String getKey() {
            return this.key;
        }

        public String getValue() {
            return this.value;
        }
    }
}
