import org.gradle.internal.os.OperatingSystem

val lwjglVer: String by rootProject.extra
val lwjglModules: List<String> by rootProject.extra
val lwjglNatives: String by rootProject.extra

val lwjglExtract: Configuration by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}

dependencies {
    implementation(project(":client"))

    lwjglModules.forEach { module ->
        val notation = "org.lwjgl:$module:$lwjglVer:$lwjglNatives"
        runtimeOnly(notation)
        lwjglExtract(notation)
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
    mainClass.set("net.radiant.start.DevStart")

    args(
        "--gameDir", minecraftDir,
        "--accessToken", "0",
        "--userProperties", "{}"
    )
}

tasks.register<JavaExec>("RunClient") {
    group = "RadiantMCP"
    description = "Starts the Minecraft client."
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
