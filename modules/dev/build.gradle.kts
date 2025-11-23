plugins {
    id("java")
    id("java-library")
}

val lwjglVer: String by rootProject.extra
val lwjglModules: List<String> by rootProject.extra
val lwjglNatives: String by rootProject.extra

val lwjglExtract by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.runtimeOnly.get())
}

val nettyExtract by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    extendsFrom(configurations.implementation.get())
}

dependencies {
    implementation(project(":client"))

    lwjglModules.forEach { module ->
        runtimeOnly(group = "org.lwjgl", name = module, version = lwjglVer, classifier = lwjglNatives)
        lwjglExtract(group = "org.lwjgl", name = module, version = lwjglVer, classifier = lwjglNatives)
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

tasks.register<JavaExec>("RunClient") {
    group = "RadiantMCP"
    description = "Starts the Minecraft client."

    dependsOn("ExtractLwjglNatives")

    doFirst {
        workingDir = rootProject.file("run")
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
    }

    classpath = sourceSets["main"].runtimeClasspath

    mainClass.set("net.radiant.start.DevStart")

    args = listOf(
        "--gameDir", minecraftDir,
        "--accessToken", "0",
        "--userProperties", "{}"
    )
}

tasks.register<JavaExec>("RunClientNativeAgent") {
    group = "RadiantMCP"
    description = "Starts the Minecraft client with the native image tracing agent attached. This won't work if ran in debug mode."

    dependsOn("ExtractLwjglNatives")

    doFirst {
        workingDir = rootProject.file("run")
        if (!workingDir.exists()) {
            workingDir.mkdirs()
        }
    }

    classpath = sourceSets["main"].runtimeClasspath

    mainClass.set("net.radiant.start.DevStart")

    args = listOf(
        "--gameDir", minecraftDir,
        "--accessToken", "0",
        "--userProperties", "{}"
    )

    jvmArgs = listOf(
        "-Djava.library.path=natives",
        "-Dradiant.exerciseClasses",
        "-agentlib:native-image-agent=config-output-dir=../modules/dev/src/main/resources/META-INF/native-image"
    )
}

tasks.register<Copy>("ExtractLwjglNatives") {
    group = "RadiantMCP"
    description = "Extracts LWJGL native libraries"

    from(lwjglExtract.resolve()
        .filter { it.name.contains("natives") }
        .map { zipTree(it) }
    )

    include("**/*.dll", "**/*.dylib", "**/*.so")

    eachFile {
        relativePath = RelativePath(true, name)
    }

    includeEmptyDirs = false

    into("${rootProject.projectDir}/run/natives")
}
