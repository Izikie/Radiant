plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.11.0"
}

group = "com.github.izikie"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

val lwjglVersion = project.property("lwjgl_version") as String

val osName = System.getProperty("os.name")
val osArch = System.getProperty("os.arch")

fun isArm(arch: String) = arch == "aarch64" || arch == "arm64" || arch.startsWith("armv8")
fun isPpc(arch: String) = arch.startsWith("ppc") || arch.startsWith("powerpc")
fun isRiscv(arch: String) = arch.startsWith("riscv")

val lwjglNatives = "natives-${when {
    osName == "FreeBSD" -> "freebsd"

    osName.startsWith("Linux") || osName.startsWith("Unix") -> when {
        isArm(osArch) -> "linux-arm64"
        isPpc(osArch) -> "linux-ppc64le"
        isRiscv(osArch) -> "linux-riscv64"
        else -> "linux"
    }

    osName.startsWith("Mac OS X") || osName.startsWith("Darwin") ->
        "macos${if (isArm(osArch)) "-arm64" else ""}"

    osName.startsWith("Windows") ->
        "windows${if (isArm(osArch)) "-arm64" else ""}"

    else -> error("Unsupported platform: $osName $osArch")
}}"


repositories {
    mavenCentral()
    maven { url = uri("https://libraries.minecraft.net") }
    maven { url = uri("https://litarvan.github.io/maven") }
}

dependencies {
    implementation(group = "com.ibm.icu", name = "icu4j", version = "77.1")

    implementation(group = "com.mojang", name = "authlib", version = "3.18.38") {
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
        exclude(group = "org.apache.logging.log4j", module = "log4j-api")

        // Optimization: Exclude outdated transitive dependencies - we provide newer versions
        exclude(group = "com.google.guava", module = "guava")           // authlib uses 17.0, we use 33.4.8-jre
        exclude(group = "org.apache.commons", module = "commons-lang3") // authlib uses 3.3.2, we use 3.17.0
        exclude(group = "commons-io", module = "commons-io")            // authlib uses 2.4, we use 2.19.0
        exclude(group = "commons-codec", module = "commons-codec")      // authlib uses 1.9, we use 1.18.0
        exclude(group = "com.google.code.gson", module = "gson")        // authlib uses 2.2.4, we use 2.13.1
    }

    // LWJGL libraries
    implementation(group = "org.lwjgl", name = "lwjgl", version = lwjglVersion)
    implementation(group = "org.lwjgl", name = "lwjgl-glfw", version = lwjglVersion)
    implementation(group = "org.lwjgl", name = "lwjgl-openal", version = lwjglVersion)
    implementation(group = "org.lwjgl", name = "lwjgl-opengl", version = lwjglVersion)
    implementation(group = "org.lwjgl", name = "lwjgl-stb", version = lwjglVersion)

    // LWJGL natives
    runtimeOnly(group = "org.lwjgl", name = "lwjgl", version = lwjglVersion, classifier = lwjglNatives)
    runtimeOnly(group = "org.lwjgl", name = "lwjgl-glfw", version = lwjglVersion, classifier = lwjglNatives)
    runtimeOnly(group = "org.lwjgl", name = "lwjgl-openal", version = lwjglVersion, classifier = lwjglNatives)
    runtimeOnly(group = "org.lwjgl", name = "lwjgl-opengl", version = lwjglVersion, classifier = lwjglNatives)
    runtimeOnly(group = "org.lwjgl", name = "lwjgl-stb", version = lwjglVersion, classifier = lwjglNatives)

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

    // Although is slower but easier than fastjson2 and works with Native Image if proper setup is done
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

    implementation(group = "net.sf.jopt-simple", name = "jopt-simple", version = "5.0.4")

    implementation(
        group = "org.slf4j", name = "slf4j-api",
        version = project.property("slf4j_version") as String
    )
    implementation(
        group = "org.apache.logging.log4j", name = "log4j-api",
        version = project.property("log4j_version") as String
    )
    implementation(
        group = "org.apache.logging.log4j", name = "log4j-core",
        version = project.property("log4j_version") as String
    )
    implementation(
        group = "org.apache.logging.log4j", name = "log4j-slf4j2-impl",
        version = project.property("log4j_version") as String
    )

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

    compileOnly(group = "org.jetbrains", name = "annotations", version = "26.0.2")
}

val os = System.getProperty("os.name").lowercase()
val home = System.getProperty("user.home")
val appData = System.getenv("APPDATA")

val minecraftDir = when {
    "windows" in os -> "$appData/.minecraft"
    "mac" in os -> "$home/Library/Application Support/minecraft"
    else -> "$home/.minecraft"
}


tasks.register<JavaExec>("RunClient") {
    group = "GradleMCP"
    description = "Starts the Minecraft ."

    mainClass.set("net.minecraft.client.main.Main")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = file(minecraftDir)

    args = listOf(
        "--assetsDir", "assets",
        "--accessToken", "0",
        "--userProperties", "{}"
    )

    systemProperty("log4j2.formatMsgNoLookups", "true")
}

tasks.register<Jar>("AllJar") {
    group = "GradleMCP"
    description = "Builds a jar that includes all the libraries as well as the client classes."

    archiveFileName.set("all.jar")

    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().map {
        if (it.isDirectory) it else zipTree(it)
    })

    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA")

    duplicatesStrategy = DuplicatesStrategy.INCLUDE

    manifest {
        attributes(
            mapOf(
                "Main-Class" to "net.minecraft.client.main.Main",
                "Multi-Release" to "true"
            )
        )
    }

    dependsOn(tasks.named("classes"))
}
