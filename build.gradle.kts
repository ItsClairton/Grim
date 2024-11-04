plugins {
    id("java")
    id("com.gradleup.shadow") version "8.3.2"
    id("io.freefair.lombok") version "8.7.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "ac.grim.grimac"
version = "2.3.67"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // Spigot
    maven("https://repo.viaversion.com") // ViaVersion
    maven("https://repo.aikar.co/content/groups/aikar/") // ACF
    maven("https://nexus.scarsz.me/content/repositories/releases") // Configuralize
    maven("https://repo.opencollab.dev/maven-snapshots/") // Floodgate
    maven("https://repo.opencollab.dev/maven-releases/") // Cumulus (for Floodgate)
    mavenCentral() // FastUtil
    maven("https://jitpack.io/")
}

dependencies {
    implementation("com.github.itsclairton.packetevents:packetevents-spigot:d6a8d4b7fc")
    implementation("co.aikar:acf-paper:0.5.1-SNAPSHOT")
    implementation("it.unimi.dsi:fastutil:8.5.14")
    implementation("github.scarsz:configuralize:1.4.0")

    implementation("com.github.ItsClairton:GrimAPI:e691d1ae36")

    implementation("org.jetbrains:annotations:24.1.0")
    compileOnly("org.geysermc.floodgate:api:2.0-SNAPSHOT")
    compileOnly("org.spigotmc:spigot-api:1.18.2-R0.1-SNAPSHOT")
    compileOnly("com.viaversion:viaversion-api:5.0.4-SNAPSHOT")
    //
    compileOnly("io.netty:netty-all:4.1.85.Final")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    shadowJar {
        minimize()
        archiveFileName.set("${project.name}.jar")

        if (!gradle.startParameter.taskNames.contains("runServer")) { // You can't use hotswap with relocate =(
            relocate("io.github.retrooper.packetevents", "ac.grim.grimac.shaded.io.github.retrooper.packetevents")
            relocate("com.github.retrooper.packetevents", "ac.grim.grimac.shaded.com.github.retrooper.packetevents")
            relocate("co.aikar.commands", "ac.grim.grimac.shaded.acf")
            relocate("co.aikar.locale", "ac.grim.grimac.shaded.locale")
            relocate("github.scarsz.configuralize", "ac.grim.grimac.shaded.configuralize")
            relocate("com.github.puregero", "ac.grim.grimac.shaded.com.github.puregero")
            relocate("com.google.code.gson", "ac.grim.grimac.shaded.gson")
            relocate("alexh", "ac.grim.grimac.shaded.maps")
            relocate("it.unimi.dsi.fastutil", "ac.grim.grimac.shaded.fastutil")
            relocate("net.kyori", "ac.grim.grimac.shaded.kyori")
            relocate("okhttp3", "ac.grim.grimac.shaded.okhttp3")
            relocate("okio", "ac.grim.grimac.shaded.okio")
            relocate("org.yaml.snakeyaml", "ac.grim.grimac.shaded.snakeyaml")
            relocate("org.json", "ac.grim.grimac.shaded.json")
            relocate("org.intellij", "ac.grim.grimac.shaded.intellij")
            relocate("org.jetbrains", "ac.grim.grimac.shaded.jetbrains")
        }
    }

    processResources {
        inputs.property("version", project.version)

        val version = project.version
        filesMatching("plugin.yml") {
            expand(mapOf("version" to version))
        }
    }

    runServer {
        minecraftVersion("1.8.8")
        jvmArgs("-XX:+AllowEnhancedClassRedefinition",
            "-Dfile.encoding=UTF-8",
            "-Dio.netty.leakDetection.level=paranoid",
            "-Xmx1G",
            "-Xms1G")

        downloadPlugins {
            hangar("ViaVersion", "5.1.1")
            hangar("ViaBackwards", "5.1.1")
            hangar("ViaRewind", "4.0.3")

            url("https://ci.lucko.me/job/spark/455/artifact/spark-bukkit/build/libs/spark-1.10.109-bukkit.jar")
        }
    }

}