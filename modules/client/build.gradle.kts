val lwjglModules: List<String> by rootProject.extra
val lwjglVer: String by rootProject.extra
val nettyVer: String by rootProject.extra
val slf4jVer: String by rootProject.extra

dependencies {
    // --- Networking ---
    listOf("netty-buffer", "netty-handler", "netty-transport", "netty-common", "netty-codec").forEach { module ->
        impl("io.netty:$module:$nettyVer", transitive = true)
    }

    listOf("linux-x86_64", "linux-aarch_64").forEach { arch ->
        impl("io.netty:netty-transport-native-epoll:$nettyVer:$arch", transitive = true)
    }

    // --- Graphics & Math ---
    lwjglModules.forEach { module ->
        impl("org.lwjgl:$module:$lwjglVer")
    }
    impl("org.joml:joml:1.10.8")

    // --- Collections & Utilities ---
    impl("com.google.guava:guava:33.4.8-jre", transitive = true)
    impl("it.unimi.dsi:fastutil:8.5.16")
    impl("com.ibm.icu:icu4j:77.1")
    impl("net.sf.jopt-simple:jopt-simple:5.0.4")

    // --- Mojang & JSON ---
    impl("com.mojang:authlib:3.18.38")
    impl("com.google.code.gson:gson:2.13.1")

    // --- Apache Commons ---
    impl("org.apache.commons:commons-lang3:3.18.0")
    impl("org.apache.commons:commons-compress:1.28.0")
    impl("org.apache.commons:commons-text:1.14.0")
    impl("commons-io:commons-io:2.20.0")
    impl("commons-codec:commons-codec:1.19.0")

    // --- Logger ---
    impl("org.slf4j:slf4j-api:$slf4jVer")
}

fun DependencyHandler.impl(notation: String, transitive: Boolean = false) {
    api(notation) {
        isTransitive = transitive
    }
}
