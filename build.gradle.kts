plugins {
    id("java")
    id("org.graalvm.buildtools.native") version "0.11.0"
}

group = "com.github.izikie"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

val lwjglVersion = project.property("lwjgl_version") as String
val osName = System.getProperty("os.name") as String
val osArch = System.getProperty("os.arch") as String

fun String.isArm() = startsWith("armv8") || this == "aarch64" || this == "arm64"
fun String.isPpc() = startsWith("ppc") || startsWith("powerpc")
fun String.isRiscv() = startsWith("riscv")

val lwjglNatives = "natives-${when {
    osName == "FreeBSD" -> "freebsd"
    osName.startsWith("Linux") || osName.startsWith("Unix") -> when {
        osArch.isArm() -> "linux-arm64"
        osArch.isPpc() -> "linux-ppc64le"
        osArch.isRiscv() -> "linux-riscv64"
        else -> "linux"
    }
    osName.startsWith("Mac OS X") || osName.startsWith("Darwin") ->
        "macos${if (osArch.isArm()) "-arm64" else ""}"
    osName.startsWith("Windows") ->
        "windows${if (osArch.isArm()) "-arm64" else ""}"
    else -> error("Unsupported platform: $osName $osArch")
}}"


repositories {
    mavenCentral()
    maven { url = uri("https://libraries.minecraft.net") }
    maven { url = uri("https://litarvan.github.io/maven") }
}


dependencies {
    val lwjglModules = listOf("lwjgl", "lwjgl-glfw", "lwjgl-openal", "lwjgl-opengl", "lwjgl-stb")
    lwjglModules.forEach { module ->
        impl(group = "org.lwjgl", name = module, version = lwjglVersion)
        runtimeOnly(group = "org.lwjgl", name = module, version = lwjglVersion, classifier = lwjglNatives)
    }

    impl(group = "com.paulscode", name = "codecjorbis", version = "20101023")
    impl(group = "com.paulscode", name = "soundsystem", version = "20120107")

//    impl(group = "org.jcraft", name = "jorbis", version = "0.0.17")

    /*val nettyVersion = "4.2.3.Final"
    val nettyModules = listOf("netty-buffer", "netty-handler", "netty-transport", "netty-common", "netty-codec")
    nettyModules.forEach { module ->
        impl(group = "io.netty", name = module, version = nettyVersion, isTransitive = true)
    }
    listOf("linux-x86_64", "linux-aarch_64").forEach { c ->
        impl(group = "io.netty", name = "netty-transport-native-epoll", version = nettyVersion, classifier = c, isTransitive = true)
    }*/

    implementation("io.netty", "netty-all","4.2.3.Final")

    impl(group = "com.ibm.icu", name = "icu4j", version = "77.1")

    impl(group = "com.mojang", name = "authlib", version = "3.18.38")

    impl(group = "com.google.guava", name = "guava", version = "33.4.8-jre", isTransitive = true)
    impl(group = "com.google.code.gson", name = "gson", version = "2.13.1")

    impl(group = "commons-io", name = "commons-io", version = "2.20.0")
    impl(group = "commons-codec", name = "commons-codec", version = "1.19.0")

    impl(group = "org.apache.commons", name = "commons-lang3", version = "3.18.0")
    impl(group = "org.apache.commons", name = "commons-compress", version = "1.28.0")
    impl(group = "org.apache.commons", name = "commons-text", version = "1.14.0")

    impl(group = "net.sf.jopt-simple", name = "jopt-simple", version = "5.0.4")

    impl(
        group = "org.slf4j", name = "slf4j-api",
        version = project.property("slf4j_version") as String
    )
    impl(group = "fr.litarvan", name = "openauth", version = "1.1.6")

    impl(group = "it.unimi.dsi", name = "fastutil", version = "8.5.16")

    impl(group = "org.joml", name = "joml", version = "1.10.8")

    compileOnly(group = "org.jetbrains", name = "annotations", version = "26.0.2")
}

fun DependencyHandler.impl(
    group: String,
    name: String,
    version: String,
    classifier: String? = null,
    isTransitive: Boolean = false
) {
    implementation(group = group, name = name, version = version) {
        this.isTransitive = isTransitive
        if (classifier != null) {
            artifact {
                this.classifier = classifier
            }
        }
    }
}

val os = System.getProperty("os.name").lowercase()
val home = System.getProperty("user.home") as String
val appData = System.getenv("APPDATA") as String

val minecraftDir = when {
    "windows" in os -> "$appData/.minecraft"
    "mac" in os -> "$home/Library/Application Support/minecraft"
    else -> "$home/.minecraft"
}

fun JavaExec.configureRunClient() {
    group = "GradleMCP"

    doFirst {
        val runDir = file("run")
        if (!runDir.exists()) {
            runDir.mkdirs()
        }
    }

    mainClass.set("net.minecraft.client.main.Main")
    classpath = sourceSets["main"].runtimeClasspath
    workingDir = file("run")

    args = listOf(
        "--gameDir", minecraftDir,
        "--accessToken", "0",
        "--userProperties", "{}"
    )
}

tasks.register<JavaExec>("RunClient") {
    description = "Starts the Minecraft client."
    configureRunClient()
}

tasks.register<JavaExec>("RunClientNativeAgent") {
    description = "Starts the Minecraft client with the native image tracing agent attached. This won't work if ran in debug mode."
    configureRunClient()
    jvmArgs = listOf(
        "-agentlib:native-image-agent=config-output-dir=../src/main/resources/META-INF/native-image",
        "-Dradiant.exerciseClasses"
    )
}

graalvmNative {
    binaries {
        named("main") {
            toolchainDetection.set(true)

            classpath.from(sourceSets.main.get().runtimeClasspath)
            imageName.set("Minecraft")
            mainClass.set("net.minecraft.client.main.Main")
            verbose.set(true)
            fallback.set(false)

            buildArgs.addAll(
                "--emit", "build-report",
                "--initialize-at-run-time",
                "--enable-url-protocols=http,https",
                "-march=x86-64-v3",
                "--color=always",
                "-Ob"
            )
            quickBuild = true
        }
    }
}
