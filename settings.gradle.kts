rootProject.name = "Radiant"

val osName = System.getProperty("os.name") as String
val osArch = System.getProperty("os.arch") as String

fun String.isArm() = startsWith("armv8") || this == "aarch64" || this == "arm64"
fun String.isPpc() = startsWith("ppc") || startsWith("powerpc")
fun String.isRiscv() = startsWith("riscv")

val lwjglNatives = "natives-${
    when {
        osName == "FreeBSD" -> "freebsd"
        osName.startsWith("Linux") || osName.startsWith("Unix") -> when {
            osArch.isArm() -> "linux-arm64"
            osArch.isPpc() -> "linux-ppc64le"
            osArch.isRiscv() -> "linux-riscv64"
            else -> "linux"
        }

        osName.startsWith("Mac OS X") || osName.startsWith("Darwin") ->
            "macos${if (osArch.isArm()) "-arm64" else ""}"

        osName.startsWith("Windows") ->
            "windows${if (osArch.isArm()) "-arm64" else ""}"

        else -> error("Unsupported platform: $osName $osArch")
    }
}"

gradle.rootProject {
    extra["lwjglNatives"] = lwjglNatives
    extra["lwjglModules"] = listOf(
        "lwjgl", "lwjgl-glfw", "lwjgl-opengl", "lwjgl-openal", "lwjgl-stb"
    )
}

file("modules").listFiles()?.filter { it.isDirectory }?.forEach {
    include(it.name)
    project(":${it.name}").projectDir = it
}
