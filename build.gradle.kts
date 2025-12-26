extra["slf4jVer"] = project.property("slf4j_version") as String
extra["nettyVer"] = project.property("netty_version") as String
extra["lwjglVer"] = project.property("lwjgl_version") as String
extra["javafxVer"] = project.property("javafx_version") as String
extra["lwjglNatives"] = rootProject.extra["lwjgl_natives"] as String

group = "com.github.izikie"
version = "1.0"
description = "Optimized Minecraft Java Client for 1.8.9"

extra["lwjglModules"] = listOf(
    "lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-openal", "lwjgl-stb"
)

subprojects {
    apply(plugin = "java")
    apply(plugin = "java-library")

    extensions.configure(JavaPluginExtension::class.java) {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    repositories {
        mavenCentral()
        maven { url = uri("https://libraries.minecraft.net") }
        maven { url = uri("https://litarvan.github.io/maven") }
    }

    dependencies {
        "compileOnly"("org.jetbrains:annotations:26.0.2-1")
    }
}
