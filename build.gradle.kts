import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import java.io.FileInputStream
import java.lang.reflect.Modifier

plugins {
    id("java")
}

group = "com.github.izikie"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

var lwjglVersion = "3.3.6"
val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    when {
        "FreeBSD" == name ->
            "natives-freebsd"
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (arrayOf("arm", "aarch64").any { arch.startsWith(it) })
                "natives-linux${if (arch.contains("64") || arch.startsWith("armv8")) "-arm64" else "-arm32"}"
            else if (arch.startsWith("ppc"))
                "natives-linux-ppc64le"
            else if (arch.startsWith("riscv"))
                "natives-linux-riscv64"
            else
                "natives-linux"
        arrayOf("Mac OS X", "Darwin").any { name.startsWith(it) } ->
            "natives-macos${if (arch.startsWith("aarch64")) "-arm64" else ""}"
        arrayOf("Windows").any { name.startsWith(it) } ->
            if (arch.contains("64"))
                "natives-windows${if (arch.startsWith("aarch64")) "-arm64" else ""}"
            else
                "natives-windows-x86"
        else ->
            throw Error("Unrecognized or unsupported platform: $name $arch")
    }
}

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.ow2.asm:asm:9.7.1")
        classpath("org.ow2.asm:asm-tree:9.7.1")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://libraries.minecraft.net") }
    maven { url = uri("https://litarvan.github.io/maven") }
}

dependencies {
    // Excluding transitive jinput from lwjgl to avoid version conflicts (lwjgl brings 2.0.5, we want 2.0.10)
    implementation(group = "net.java.jinput", name = "jinput", version = "2.0.10")

    implementation(group = "com.ibm.icu", name = "icu4j", version = "77.1")

    implementation(group = "com.mojang", name = "authlib", version = "1.5.21") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")

        // Optimization: Exclude outdated transitive dependencies - we provide newer versions
        exclude(group = "com.google.guava", module = "guava")           // authlib uses 17.0, we use 33.4.8-jre
        exclude(group = "org.apache.commons", module = "commons-lang3") // authlib uses 3.3.2, we use 3.17.0
        exclude(group = "commons-io", module = "commons-io")            // authlib uses 2.4, we use 2.19.0
        exclude(group = "commons-codec", module = "commons-codec")      // authlib uses 1.9, we use 1.18.0
        exclude(group = "com.google.code.gson", module = "gson")        // authlib uses 2.2.4, we use 2.13.1
    }

    // Audio codec libraries - all depend on soundsystem, so we exclude redundant transitive dependencies
    implementation(group = "com.paulscode", name = "codecjorbis", version = "20101023") {
        exclude(group = "com.paulscode", module = "soundsystem") // Provided explicitly below
    }
    implementation(group = "com.paulscode", name = "codecwav", version = "20101023") {
        exclude(group = "com.paulscode", module = "soundsystem") // Provided explicitly below
    }
    implementation(group = "com.paulscode", name = "libraryjavasound", version = "20101123") {
        exclude(group = "com.paulscode", module = "soundsystem") // Provided explicitly below
    }
    implementation(group = "com.paulscode", name = "librarylwjglopenal", version = "20100824") {
        exclude(group = "com.paulscode", module = "soundsystem") // Provided explicitly below
        exclude(group = "org.lwjgl.lwjgl", module = "lwjgl")      // Provided explicitly below
        exclude(group = "net.java.jinput", module = "jinput")    // Provided explicitly above
    }
    // Core sound system - single instance for all audio codecs
    implementation(group = "com.paulscode", name = "soundsystem", version = "20120107")

    // Core utility libraries - latest versions to replace all transitive dependencies
    implementation(group = "com.google.guava", name = "guava", version = "33.4.8-jre")
    implementation(group = "com.google.code.gson", name = "gson", version = "2.13.1")

    // Networking - using netty-all for simplicity, but could be optimized to specific modules if needed
    implementation(group = "io.netty", name = "netty-all", version = "4.2.1.Final")

    // Apache Commons utilities - optimized to exclude redundant transitive dependencies
    implementation(group = "commons-io", name = "commons-io", version = "2.19.0")
    implementation(group = "commons-codec", name = "commons-codec", version = "1.18.0")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.17.0")

    // Commons-compress brings older versions of commons-io (2.16.1), commons-codec (1.17.1), commons-lang3 (3.16.0)
    // We exclude these to use our newer explicit versions
    implementation(group = "org.apache.commons", name = "commons-compress", version = "1.27.1") {
        exclude(group = "commons-io", module = "commons-io")            // We use 2.19.0 instead of 2.16.1
        exclude(group = "commons-codec", module = "commons-codec")      // We use 1.18.0 instead of 1.17.1
        exclude(group = "org.apache.commons", module = "commons-lang3") // We use 3.17.0 instead of 3.16.0
    }

    // Commons-text depends on commons-lang3 3.17.0 which matches our version, so no exclusions needed
    implementation(group = "org.apache.commons", name = "commons-text", version = "1.13.1")

    implementation(group = "org.jcommander", name = "jcommander", version = "2.0")

    implementation(group = "org.apache.logging.log4j", name = "log4j-api", version = "2.24.3")
    implementation(group = "org.apache.logging.log4j", name = "log4j-core", version = "2.24.3")
    // Alternative: Switch to logback, tinylog, or java.util.logging if preferred

    // LWJGL 2.x for Minecraft 1.8.9 compatibility - excluding transitive jinput to avoid conflicts
    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl", version = "2.9.3") {
        exclude(group = "net.java.jinput", module = "jinput") // We use explicit jinput 2.0.10 above
    }
    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl_util", version = "2.9.3") // Depends on lwjgl above

    /*implementation(group = "org.lwjgl", name = "lwjgl", version = lwjglVersion)
    implementation(group = "org.lwjgl", name = "lwjgl-glfw", version = lwjglVersion)
    implementation(group = "org.lwjgl", name = "lwjgl-openal", version = lwjglVersion)
    implementation(group = "org.lwjgl", name = "lwjgl-opengl", version = lwjglVersion)

    runtimeOnly(group = "org.lwjgl", name = "lwjgl", version = lwjglVersion, classifier = lwjglNatives)
    runtimeOnly(group = "org.lwjgl", name = "lwjgl-glfw", version = lwjglVersion, classifier = lwjglNatives)
    runtimeOnly(group = "org.lwjgl", name = "lwjgl-openal", version = lwjglVersion, classifier = lwjglNatives)
    runtimeOnly(group = "org.lwjgl", name = "lwjgl-opengl", version = lwjglVersion, classifier = lwjglNatives)*/

    // Alternative authentication library - excluding gson to use our explicit version
    implementation(group = "fr.litarvan", name = "openauth", version = "1.1.6") {
        exclude(group = "com.google.code.gson", module = "gson") // openauth uses 2.10.1, we use 2.13.1
    }

    implementation(group = "it.unimi.dsi", name = "fastutil", version = "8.5.15")

    // Math library for 3D operations - excluding kotlin stdlib to reduce dependency bloat
    implementation(group = "org.joml", name = "joml", version = "1.10.8") {
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk8") // Not needed for Java-only project
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-jdk7") // Not needed for Java-only project
        exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib")      // Not needed for Java-only project
    }

    //implementation(group = "com.alibaba.fastjson2", name = "fastjson2", version = "2.0.57")
    // Look into, how adapter stuff works, and if the code rework is worth the speedup
}

val minecraftDir: String = when {
    System.getProperty("os.name").contains("Windows", ignoreCase = true) -> System.getenv("APPDATA") + "/.minecraft"
    System.getProperty("os.name").contains("Mac", ignoreCase = true) -> System.getProperty("user.home") + "/Library/Application Support/minecraft"
    else -> System.getProperty("user.home") + "/.minecraft"
}

tasks.register<JavaExec>("RunClient") {
    group = "GradleMCP"
    description = "Start's the client"
    classpath(sourceSets.getByName("main").runtimeClasspath)

    workingDir = file(minecraftDir)
    args("--assetsDir", "assets")
    args("--assetIndex", "1.8")
    args("--accessToken", "0")
    args("--userProperties", "{}")

    systemProperty("java.library.path", "${projectDir}/natives")
    systemProperty("log4j2.formatMsgNoLookups", "true")

    mainClass = "net.minecraft.client.main.Main"
}

tasks.withType<JavaCompile> {
    options.isIncremental = false
    //dependsOn("transformEnumValues")
}

fun getOpcodeName(opcode: Int): String? {
    val fields = Opcodes::class.java.fields
    for (field in fields) {
        if (Modifier.isStatic(field.modifiers) && field.type == Int::class.javaPrimitiveType) {
            try {
                if (field.getInt(null) == opcode) {
                    return field.name
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }
    return null
}

fun getTypeName(type: Int): String? {
    val fields = AbstractInsnNode::class.java.fields
    for (field in fields) {
        if (Modifier.isStatic(field.modifiers) && field.type == Int::class.javaPrimitiveType) {
            try {
                if (field.getInt(null) == type) {
                    return field.name
                }
            } catch (e: IllegalAccessException) {
                e.printStackTrace()
            }
        }
    }
    return null
}

// Credit To: pOtAto__bOy, and his Redirector Mod for the optimization trick.
// Still need to make sure it actually even works correctly and how much it actually optimizes.
tasks.register("transformEnumValues") {
    group = "optimization"
    description = "Transforms enum classes to modify the 'values' method."

    doFirst {
        val classpath = sourceSets.getByName("main").runtimeClasspath

        classpath.forEach fileLoop@{ file ->
            if (!file.isDirectory)
                return@fileLoop

            file.walkTopDown().forEach classLoop@{ classFile ->
                if (!classFile.name.endsWith(".class"))
                    return@classLoop

                val fileInputStream = FileInputStream(classFile)
                val classReader = ClassReader(fileInputStream)
                val classNode = ClassNode()
                classReader.accept(classNode, 0)

                if ("java/lang/Enum" != classNode.superName)
                    return@classLoop

                println("Optimized class: ${classNode.name.replace('/', '.')}")

                for (methodNode in classNode.methods) {
                    if ("values" != methodNode.name || ("()[L" + classNode.name + ";") != methodNode.desc)
                        continue

                    println("\tFound: ${methodNode.name} ${methodNode.desc}")

                    val iterator = methodNode.instructions.iterator()
                    while (iterator.hasNext()) {
                        val node = iterator.next()
                        val code = node.opcode

                        if (code == Opcodes.GETSTATIC || code == Opcodes.ARETURN)
                            continue

                        println("\t\t Removed: TYPE(${getTypeName(node.type)}) OP(${getOpcodeName(node.opcode)})")

                        iterator.remove()
                    }
                }

                val classWriter = ClassWriter(0)
                classNode.accept(classWriter)
                val modifiedClassBytes = classWriter.toByteArray()

                classFile.writeBytes(modifiedClassBytes)
            }
        }
    }
}
