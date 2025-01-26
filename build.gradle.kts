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

group = "com.github.tectonic"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

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
    // Third Party
    implementation(group = "fr.litarvan", name = "openauth", version = "1.1.6")
    implementation(group = "org.joml", name = "joml", version = "1.10.8")

    implementation(group = "net.java.dev.jna", name = "jna", version = "5.16.0")
    implementation(group = "net.java.dev.jna", name = "jna-platform", version = "5.16.0")

    implementation(group = "com.ibm.icu", name = "icu4j", version = "76.1")
    implementation(group = "net.sf.jopt-simple", name = "jopt-simple", version = "5.0.4")

    implementation(group = "com.paulscode", name = "codecjorbis", version = "20101023")
    implementation(group = "com.paulscode", name = "codecwav", version = "20101023")
    implementation(group = "com.paulscode", name = "libraryjavasound", version = "20101123")
    implementation(group = "com.paulscode", name = "librarylwjglopenal", version = "20100824")
    implementation(group = "com.paulscode", name = "soundsystem", version = "20120107")

    implementation(group = "io.netty", name = "netty-all", version = "4.0.23.Final") // Update To Newer Rewriten Netty Versions Or Use Another Library

    implementation(group = "net.java.jinput", name = "jinput", version = "2.0.7")
    implementation(group = "net.java.jutils", name = "jutils", version = "1.0.0")

    implementation(group = "com.google.guava", name = "guava", version = "17.0")
    implementation(group = "com.google.code.gson", name = "gson", version = "2.11.0") // Move to another faster more modern JSON library

    implementation(group = "commons-io", name = "commons-io", version = "2.18.0")
    implementation(group = "commons-codec", name = "commons-codec", version = "1.17.2")
    implementation(group = "commons-logging", name = "commons-logging", version = "1.3.4")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.17.0")
    implementation(group = "org.apache.commons", name = "commons-compress", version = "1.27.1")
    implementation(group = "org.apache.commons", name = "commons-text", version = "1.13.0")


    implementation(group = "org.apache.logging.log4j", name = "log4j-api", version = "2.24.3")
    implementation(group = "org.apache.logging.log4j", name = "log4j-core", version = "2.24.3")

    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl", version = "2.9.3") // Update To 3.0 Properly
    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl_util", version = "2.9.3") // Update To 3.0 Properly

    implementation(group = "com.mojang", name = "authlib", version = "1.5.21")
}

val minecraftDir: String = when {
    System.getProperty("os.name").contains("Windows", ignoreCase = true) -> System.getenv("APPDATA") + "/.minecraft"
    System.getProperty("os.name").contains("Mac", ignoreCase = true) -> System.getProperty("user.home") + "/Library/Application Support/minecraft"
    else -> System.getProperty("user.home") + "/.minecraft"
}

tasks.register<JavaExec>("RunClient") {
    group = "GradleMCP"
    description = "Runs the client"
    classpath(sourceSets.getByName("main").runtimeClasspath)

    workingDir = file(minecraftDir)
    args("--version", "${project.name}-1.8.9")
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
