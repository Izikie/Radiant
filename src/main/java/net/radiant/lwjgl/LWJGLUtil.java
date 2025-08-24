/*
 * Copyright (c) 2002-2008 LWJGL Project
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * * Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright
 *   notice, this list of conditions and the following disclaimer in the
 *   documentation and/or other materials provided with the distribution.
 *
 * * Neither the name of 'LWJGL' nor the names of
 *   its contributors may be used to endorse or promote products derived
 *   from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.radiant.lwjgl;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.util.*;

/**
 * <p>
 * Internal library methods
 * </p>
 *
 * @author Brian Matzon <brian@matzon.dk>
 * @version $Revision: 3608 $
 * $Id: LWJGLUtil.java 3608 2011-08-10 16:05:46Z spasi $
 */
public class LWJGLUtil {
    public static final int PLATFORM_LINUX = 1;
    public static final int PLATFORM_MACOSX = 2;
    public static final int PLATFORM_WINDOWS = 3;
    public static final String PLATFORM_LINUX_NAME = "linux";
    public static final String PLATFORM_MACOSX_NAME = "macosx";
    public static final String PLATFORM_WINDOWS_NAME = "windows";
    /**
     * Debug flag.
     */
    public static final boolean DEBUG = getPrivilegedBoolean("org.lwjgl.util.Debug");

    public static final boolean CHECKS = !getPrivilegedBoolean("org.lwjgl.util.NoChecks");

    private static final int PLATFORM;

    static {
        final String osName = getPrivilegedProperty("os.name");
        if (osName.startsWith("Windows"))
            PLATFORM = PLATFORM_WINDOWS;
        else if (osName.startsWith("Linux") || osName.startsWith("FreeBSD") || osName.startsWith("SunOS") || osName.startsWith("Unix"))
            PLATFORM = PLATFORM_LINUX;
        else if (osName.startsWith("Mac OS X") || osName.startsWith("Darwin"))
            PLATFORM = PLATFORM_MACOSX;
        else
            throw new LinkageError("Unknown platform: " + osName);
    }

    private static ByteBuffer loadIcon(String data) {
        int len = data.length();
        ByteBuffer bb = BufferUtils.createByteBuffer(len);
        for (int i = 0; i < len; i++) {
            bb.put(i, (byte) data.charAt(i));
        }
        return bb.asReadOnlyBuffer();
    }

    /**
     * @return the current platform type
     * @see #PLATFORM_WINDOWS
     * @see #PLATFORM_LINUX
     * @see #PLATFORM_MACOSX
     */
    public static int getPlatform() {
        return PLATFORM;
    }


    /**
     * @return current platform name
     * @see #PLATFORM_WINDOWS_NAME
     * @see #PLATFORM_LINUX_NAME
     * @see #PLATFORM_MACOSX_NAME
     */
    public static String getPlatformName() {
        return switch (LWJGLUtil.getPlatform()) {
            case LWJGLUtil.PLATFORM_LINUX -> PLATFORM_LINUX_NAME;
            case LWJGLUtil.PLATFORM_MACOSX -> PLATFORM_MACOSX_NAME;
            case LWJGLUtil.PLATFORM_WINDOWS -> PLATFORM_WINDOWS_NAME;
            default -> "unknown";
        };
    }

    /**
     * Locates the paths required by a library.
     *
     * @param libname           Local Library Name to search the classloader with ("openal").
     * @param platform_lib_name The native library name ("libopenal.so")
     * @param classloader       The classloader to ask for library paths
     * @return Paths to located libraries, if any
     */
    public static String[] getLibraryPaths(String libname, String platform_lib_name, ClassLoader classloader) {
        return getLibraryPaths(libname, new String[]{platform_lib_name}, classloader);
    }

    /**
     * Locates the paths required by a library.
     *
     * @param libname            Local Library Name to search the classloader with ("openal").
     * @param platform_lib_names The list of possible library names ("libopenal.so")
     * @param classloader        The classloader to ask for library paths
     * @return Paths to located libraries, if any
     */
    public static String[] getLibraryPaths(String libname, String[] platform_lib_names, ClassLoader classloader) {
        // need to pass path of possible locations of library to native side
        List<String> possible_paths = new ArrayList<>();

        String classloader_path = getPathFromClassLoader(libname, classloader);
        if (classloader_path != null) {
            log("getPathFromClassLoader: Path found: " + classloader_path);
            possible_paths.add(classloader_path);
        }

        for (String platform_lib_name : platform_lib_names) {
            String lwjgl_classloader_path = getPathFromClassLoader("lwjgl", classloader);
            if (lwjgl_classloader_path != null) {
                log("getPathFromClassLoader: Path found: " + lwjgl_classloader_path);
                possible_paths.add(lwjgl_classloader_path.substring(0, lwjgl_classloader_path.lastIndexOf(File.separator))
                        + File.separator + platform_lib_name);
            }

            // add Installer path
            String alternative_path = getPrivilegedProperty("org.lwjgl.librarypath");
            if (alternative_path != null) {
                possible_paths.add(alternative_path + File.separator + platform_lib_name);
            }

            // Add all possible paths from java.library.path
            String java_library_path = getPrivilegedProperty("java.library.path");

            StringTokenizer st = new StringTokenizer(java_library_path, File.pathSeparator);
            while (st.hasMoreTokens()) {
                String path = st.nextToken();
                possible_paths.add(path + File.separator + platform_lib_name);
            }

            //add current path
            String current_dir = getPrivilegedProperty("user.dir");
            possible_paths.add(current_dir + File.separator + platform_lib_name);

            //add pure library (no path, let OS search)
            possible_paths.add(platform_lib_name);
        }

        //create needed string array
        return possible_paths.toArray(new String[0]);
    }

    static void execPrivileged(final String[] cmd_array) throws Exception {
        Process process = Runtime.getRuntime().exec(cmd_array);
        // Close unused streams to make sure the child process won't hang
        process.getInputStream().close();
        process.getOutputStream().close();
        process.getErrorStream().close();
    }

    private static String getPrivilegedProperty(final String property_name) {
        return System.getProperty(property_name);
    }

    /**
     * Tries to locate named library from the current ClassLoader
     * This method exists because native libraries are loaded from native code, and as such
     * is exempt from ClassLoader library loading rutines. It therefore always fails.
     * We therefore invoke the protected method of the ClassLoader to see if it can
     * locate it.
     *
     * @param libname     Name of library to search for
     * @param classloader Classloader to use
     * @return Absolute path to library if found, otherwise null
     */
    private static String getPathFromClassLoader(final String libname, final ClassLoader classloader) {
        try {
            log("getPathFromClassLoader: searching for: " + libname);
            Class<?> clazz = classloader.getClass();
            while (clazz != null) {
                Method findLibrary = clazz.getDeclaredMethod("findLibrary", String.class);
                findLibrary.setAccessible(true);
                return (String) findLibrary.invoke(classloader, libname);
            }
        } catch (Exception e) {
            log("Failure locating " + e + " using classloader:" + e);
        }
        return null;
    }

    /**
     * Gets a boolean property as a privileged action.
     */
    public static boolean getPrivilegedBoolean(final String property_name) {
        return Boolean.getBoolean(property_name);
    }

    /**
     * Gets an integer property as a privileged action.
     *
     * @param property_name the integer property name
     * @return the property value
     */
    public static Integer getPrivilegedInteger(final String property_name) {
        return Integer.getInteger(property_name);
    }

    /**
     * Gets an integer property as a privileged action.
     *
     * @param property_name the integer property name
     * @param default_val   the default value to use if the property is not defined
     * @return the property value
     */
    public static Integer getPrivilegedInteger(final String property_name, final int default_val) {
        return Integer.getInteger(property_name, default_val);
    }

    /**
     * Prints the given message to System.err if DEBUG is true.
     *
     * @param msg Message to print
     */
    public static void log(CharSequence msg) {
        if (DEBUG) {
            System.err.println("[LWJGL] " + msg);
        }
    }

    /**
     * Method to determine if the current system is running a version of
     * Mac OS X better than the given version. This is only useful for Mac OS X
     * specific code and will not work for any other platform.
     */
    public static boolean isMacOSXEqualsOrBetterThan(int major_required, int minor_required) {
        String os_version = getPrivilegedProperty("os.version");
        StringTokenizer version_tokenizer = new StringTokenizer(os_version, ".");
        int major;
        int minor;
        try {
            String major_str = version_tokenizer.nextToken();
            String minor_str = version_tokenizer.nextToken();
            major = Integer.parseInt(major_str);
            minor = Integer.parseInt(minor_str);
        } catch (Exception e) {
            LWJGLUtil.log("Exception occurred while trying to determine OS version: " + e);
            // Best guess, no
            return false;
        }
        return major > major_required || (major == major_required && minor >= minor_required);
    }

    /**
     * Returns a string representation of the integer argument as an
     * unsigned integer in base&nbsp;16. The string will be uppercase
     * and will have a leading '0x'.
     *
     * @param value the integer value
     * @return the hex string representation
     */
    public static String toHexString(final int value) {
        return "0x" + Integer.toHexString(value).toUpperCase();
    }

    /**
     * Simple interface for Field filtering.
     */
    public interface TokenFilter {

        /**
         * Should return true if the specified Field passes the filter.
         *
         * @param field the Field to test
         * @param value the integer value of the field
         * @result true if the Field is accepted
         */
        boolean accept(Field field, int value);

    }

}
