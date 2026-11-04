plugins {
    id("java-library")
}

dependencies {
    val nettyVer = libs.versions.netty.get()
    val lwjglVer = libs.versions.lwjgl.get()

    @Suppress("UNCHECKED_CAST")
    val lwjglModules = rootProject.extra["lwjglModules"] as List<String>

    // --- Networking (Netty Core & Native Transports) ---
    val nettyModules = listOf("buffer", "handler", "transport", "common", "codec")
    val nettyNatives = listOf("linux-x86_64", "linux-aarch_64", "osx-x86_64", "osx-aarch_64")

    nettyModules.forEach { module ->
        impl("io.netty:netty-$module:$nettyVer", transitive = true)
    }

    nettyNatives.forEach { arch ->
        val prefix = if (arch.startsWith("linux")) "epoll" else "kqueue"
        impl("io.netty:netty-transport-native-$prefix:$nettyVer:$arch", transitive = true)
    }

    // --- Graphics & Math ---
    lwjglModules.forEach { module ->
        impl("org.lwjgl:$module:$lwjglVer")
    }
    impl(libs.joml)

    // --- Collections & Utilities ---
    impl(libs.guava, transitive = true)
    impl(libs.fastutil)
    impl(libs.icu4j)
    impl(libs.jopt.simple)

    // --- Mojang & JSON ---
    impl(libs.authlib)
    impl(libs.gson)

    // --- Apache Commons ---
    impl(libs.commons.lang3)
    impl(libs.commons.compress)
    impl(libs.commons.text)
    impl(libs.commons.io)
    impl(libs.commons.codec)

    // --- Logger ---
    impl(libs.slf4j.api)
}

fun DependencyHandler.impl(notation: String, transitive: Boolean = false) {
    api(notation) {
        isTransitive = transitive
    }
}

fun DependencyHandler.impl(notation: Provider<*>, transitive: Boolean = false) {
    api(notation) {
        isTransitive = transitive
    }
}
