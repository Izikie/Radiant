plugins {
    `java-library`
}

group = "com.github.izikie"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

extra["lwjglVer"] = project.property("lwjgl_version") as String
extra["nettyVer"] = project.property("netty_version") as String
extra["slf4jVer"] = project.property("slf4j_version") as String
extra["javafxVer"] = project.property("javafx_version") as String

subprojects {
    apply<JavaLibraryPlugin>()

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<JavaCompile>().configureEach {
        options.encoding = "UTF-8"
        options.release.set(25)
        options.isIncremental = true
    }

    repositories {
        mavenCentral()
        maven("https://libraries.minecraft.net")
    }

    dependencies {
        "compileOnly"("org.jetbrains:annotations:26.0.2-1")
    }
}
