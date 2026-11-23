import org.gradle.internal.os.OperatingSystem

plugins {
    id("java-library")
    alias(libs.plugins.javafx)
}

val javafxVer = libs.versions.javafx.version.get()

// 1. Create a shared configuration for LWJGL natives
val lwjglNativesConfig = configurations.create("lwjglNativesConfig") {
    isCanBeResolved = false
    isCanBeConsumed = false
}

// 2. Make extraction and runtime configurations inherit from it
val lwjglExtract = configurations.create("lwjglExtract") {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(lwjglNativesConfig)
}

configurations.named("runtimeOnly") {
    extendsFrom(lwjglNativesConfig)
}

javafx {
    version = javafxVer
    modules("javafx.base", "javafx.controls", "javafx.graphics", "javafx.swing", "javafx.web", "javafx.media")
}

dependencies {
    implementation(project(":client"))

    @Suppress("UNCHECKED_CAST")
    val lwjglModules = rootProject.extra["lwjglModules"] as List<String>
    val lwjglVer = libs.versions.lwjgl.get()
    val lwjglNatives = rootProject.extra["lwjglNatives"] as String

    // Add LWJGL natives to the shared config
    lwjglModules.distinct().forEach { module ->
        val notation = "org.lwjgl:$module:$lwjglVer:$lwjglNatives"
        lwjglNativesConfig(notation)
    }
}

val minecraftDir = run {
    val home = System.getProperty("user.home")
    when {
        OperatingSystem.current().isWindows ->
            "${System.getenv("APPDATA")}/.minecraft"

        OperatingSystem.current().isMacOsX ->
            "$home/Library/Application Support/minecraft"

        else -> "$home/.minecraft"
    }
}

tasks.withType<JavaExec>().configureEach {
    dependsOn("ExtractLwjglNatives")

    doFirst {
        val runDir = rootProject.file("run")
        if (!runDir.exists()) runDir.mkdirs()
    }

    workingDir = rootProject.file("run")
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("net.radiant.dev.start.LaunchWrapper")

    if (name != "RunVulkanSmoke") {
        args("--accessToken", "0", "--userProperties", "{}")
        if (name != "RunClientBenchmark") {
            args("--gameDir", minecraftDir)
        }
    }
    val vulkanTerrainOnly = providers.gradleProperty("vulkanTerrainOnly").orNull == "true"
    val compareFrames = name == "RunClientBenchmark" && providers.gradleProperty("benchmarkCompare").orNull == "true"
    if (compareFrames) {
        doFirst {
            require(!vulkanTerrainOnly) { "benchmarkCompare requires OpenGL terrain draws. Remove -PvulkanTerrainOnly=true." }
        }
        systemProperty("radiant.benchmark.compare", "true")
    }
    if (name == "RunClientBenchmark" && providers.gradleProperty("benchmarkCaptureReference").orNull == "true") {
        doFirst {
            require(!compareFrames && !vulkanTerrainOnly && providers.gradleProperty("vulkanTerrain").orNull != "true") {
                "benchmarkCaptureReference requires an OpenGL-only run."
            }
        }
        systemProperty("radiant.benchmark.captureReference", "true")
    }
    if ((name == "RunClient" || name == "RunClientBenchmark") &&
        (compareFrames || vulkanTerrainOnly || providers.gradleProperty("vulkanTerrain").orNull == "true")) {
        systemProperty("radiant.vulkan.terrain", "true")
        systemProperty("radiant.vulkan.terrainOnly", vulkanTerrainOnly)
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        providers.gradleProperty("vulkanValidation").orNull?.let {
            systemProperty("radiant.vulkan.validation", it)
        }
    }
}

tasks.register<JavaExec>("RunClient") {
    group = "RadiantMCP"
    description = "Starts the Minecraft client."
}

tasks.register<JavaExec>("RunVulkanSmoke") {
    group = "RadiantMCP"
    description = "Validates Vulkan presentation, resize, and shutdown on Windows."
    mainClass.set("net.radiant.dev.VulkanSmoke")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    providers.gradleProperty("vulkanValidation").orNull?.let {
        systemProperty("radiant.vulkan.validation", it)
    }
}

tasks.register<JavaExec>("RunClientBenchmark") {
    group = "RadiantMCP"
    description = "Starts the isolated client with the benchmark suite and manual F8 capture."

    val benchmarkGameDir = rootProject.file("run/benchmark-game")
    doFirst {
        benchmarkGameDir.mkdirs()
    }
    args("--gameDir", benchmarkGameDir.absolutePath, "--assetsDir", "$minecraftDir/assets")
    systemProperty("radiant.benchmark", "true")
    mapOf(
        "benchmarkScenario" to "scenario",
        "benchmarkWarmup" to "warmup",
        "benchmarkSeconds" to "seconds",
        "benchmarkAutoStart" to "autoStart",
        "benchmarkFixedCapture" to "fixedCapture",
        "benchmarkEntityHitboxes" to "entityHitboxes",
        "benchmarkEntityDamage" to "entityDamage"
    ).forEach { (property, option) ->
        providers.gradleProperty(property).orNull?.let {
            systemProperty("radiant.benchmark.$option", it)
        }
    }
}

tasks.register<JavaExec>("RunClientNativeAgent") {
    group = "RadiantMCP"
    description = "Starts the client with the native image tracing agent."

    jvmArgs(
        "-Djava.library.path=natives",
        "-Dradiant.exerciseClasses",
        "-agentlib:native-image-agent=config-output-dir=../modules/dev/src/main/resources/META-INF/native-image"
    )
}

tasks.register<Copy>("ExtractLwjglNatives") {
    group = "RadiantMCP"
    description = "Extracts LWJGL native libraries"

    from(provider {
        lwjglExtract.files
            .filter { it.name.contains("natives") }
            .map { zipTree(it) }
    })

    include("**/*.dll", "**/*.dylib", "**/*.so")

    eachFile { relativePath = RelativePath(true, name) }
    includeEmptyDirs = false

    into(rootProject.file("run/natives"))
}
