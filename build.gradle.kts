plugins {
    `java-library`
}

group = "com.github.izikie"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

extra["lwjglVer"] = libs.versions.lwjgl.get()
extra["nettyVer"] = libs.versions.netty.get()
extra["slf4jVer"] = libs.versions.slf4j.get()
extra["javafxVer"] = libs.versions.javafx.get()

allprojects {
    plugins.withType<JavaPlugin> {
        extensions.configure<JavaPluginExtension> {
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
}
