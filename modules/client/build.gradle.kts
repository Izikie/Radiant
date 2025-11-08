plugins {
    id("java")
    id("java-library")
}

val lwjglModules: List<String> by rootProject.extra
val lwjglVer: String by rootProject.extra
val nettyVer: String by rootProject.extra
val slf4jVer: String by rootProject.extra

dependencies {
    val nettyModules = listOf("netty-buffer", "netty-handler", "netty-transport", "netty-common", "netty-codec")
    nettyModules.forEach { module ->
        impl(group = "io.netty", name = module, version = nettyVer, isTransitive = true)
    }
    listOf("linux-x86_64", "linux-aarch_64").forEach { c ->
        impl(
            group = "io.netty",
            name = "netty-transport-native-epoll",
            version = nettyVer,
            classifier = c,
            isTransitive = true,
        )
    }

    impl(group = "com.ibm.icu", name = "icu4j", version = "77.1")

    impl(group = "com.mojang", name = "authlib", version = "3.18.38")

    impl(group = "com.google.guava", name = "guava", version = "33.4.8-jre", isTransitive = true)
    impl(group = "com.google.code.gson", name = "gson", version = "2.13.1")

    lwjglModules.forEach { module ->
        impl(group = "org.lwjgl", name = module, version = lwjglVer)
    }

    impl(group = "org.apache.commons", name = "commons-lang3", version = "3.18.0")
    impl(group = "org.apache.commons", name = "commons-compress", version = "1.28.0")
    impl(group = "org.apache.commons", name = "commons-text", version = "1.14.0")

    impl(group = "org.slf4j", name = "slf4j-api", version = slf4jVer)

    impl(group = "commons-io", name = "commons-io", version = "2.20.0")
    impl(group = "commons-codec", name = "commons-codec", version = "1.19.0")

    impl(group = "net.sf.jopt-simple", name = "jopt-simple", version = "5.0.4")

    impl(group = "fr.litarvan", name = "openauth", version = "1.1.6")

    impl(group = "it.unimi.dsi", name = "fastutil", version = "8.5.16")

    impl(group = "org.joml", name = "joml", version = "1.10.8")
}

fun DependencyHandler.impl(
    group: String,
    name: String,
    version: String,
    classifier: String? = null,
    isTransitive: Boolean = false
) {
    api(group = group, name = name, version = version) {
        this.isTransitive = isTransitive
        if (classifier != null) {
            artifact {
                this.classifier = classifier
            }
        }
    }
}
