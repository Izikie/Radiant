group = "com.github.izikie"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

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
