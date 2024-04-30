plugins {
    id("java")
}

group = "com.github.tectonic"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

repositories {
    mavenCentral()
    maven { url = uri("https://libraries.minecraft.net") }
}

dependencies {
    implementation(group = "oshi-project", name = "oshi-core", version = "1.1")
    implementation(group = "net.java.dev.jna", name = "jna", version = "3.4.0")
    implementation(group = "net.java.dev.jna", name = "platform", version = "3.4.0")
    implementation(group = "com.ibm.icu", name = "icu4j", version = "51.2")
    implementation(group = "net.sf.jopt-simple", name = "jopt-simple", version = "4.6")
    implementation(group = "com.paulscode", name = "codecjorbis", version = "20101023")
    implementation(group = "com.paulscode", name = "codecwav", version = "20101023")
    implementation(group = "com.paulscode", name = "libraryjavasound", version = "20101123")
    implementation(group = "com.paulscode", name = "librarylwjglopenal", version = "20100824")
    implementation(group = "com.paulscode", name = "soundsystem", version = "20120107")
    implementation(group = "io.netty", name = "netty-all", version = "4.0.23.Final")
    implementation(group = "com.google.guava", name = "guava", version = "17.0")
    implementation(group = "org.apache.commons", name = "commons-lang3", version = "3.3.2")
    implementation(group = "commons-io", name = "commons-io", version = "2.4")
    implementation(group = "commons-codec", name = "commons-codec", version = "1.9")
    implementation(group = "net.java.jinput", name = "jinput", version = "2.0.5")
    implementation(group = "net.java.jutils", name = "jutils", version = "1.0.0")
    implementation(group = "com.google.code.gson", name = "gson", version = "2.2.4")
    implementation(group = "org.apache.commons", name = "commons-compress", version = "1.8.1")
    implementation(group = "org.apache.httpcomponents", name = "httpclient", version = "4.3.3")
    implementation(group = "commons-logging", name = "commons-logging", version = "1.1.3")
    implementation(group = "org.apache.httpcomponents", name = "httpcore", version = "4.3.2")
    implementation(group = "org.apache.logging.log4j", name = "log4j-api", version = "2.22.0")
    implementation(group = "org.apache.logging.log4j", name = "log4j-core", version = "2.22.0")
    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl", version = "2.9.3")
    implementation(group = "org.lwjgl.lwjgl", name = "lwjgl_util", version = "2.9.3")
    implementation(group = "com.mojang", name = "authlib", version = "1.5.21")
    implementation(group = "tv.twitch", name = "twitch", version = "6.5")
}

val appDataDir: String = System.getenv("APPDATA")

tasks.register<JavaExec>("RunClient") {
    group = "GradleMCP"
    description = "Runs the client"
    classpath(sourceSets.getByName("main").runtimeClasspath)

    workingDir = file("$appDataDir\\.minecraft")
    args("--version", "${project.name}-1.8.9")
    args("--assetsDir", "assets")
    args("--assetIndex", "1.8")
    args("--accessToken", "0")
    args("--userProperties", "{}")

    systemProperty("java.library.path", "${projectDir}/natives")
    systemProperty("log4j2.formatMsgNoLookups", "true")

    mainClass = "net.minecraft.client.main.Main"
}