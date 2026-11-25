import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.minecraftforge.gradle.userdev.UserDevExtension
import java.util.Properties

buildscript {
    repositories {
        mavenCentral()
        maven("https://maven.minecraftforge.net")
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.1.77")
    }
}

plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "7.1.1"
}
apply(plugin = "net.minecraftforge.gradle")

val upstreamVersion = Properties().apply {
    file("../../version.properties").inputStream().use { load(it) }
}.getProperty("version")
val portVersion: String by project
val minecraftVersion = "1.12.2"

group = "me.sorihiko.dango"
version = portVersion

repositories { mavenCentral() }
configure<UserDevExtension> { mappings("stable", "39-1.12") }

val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
}
configurations["implementation"].extendsFrom(shade)
dependencies {
    add("minecraft", "net.minecraftforge:forge:1.12.2-14.23.5.2860")
    shade("me.sorihiko.dango:core:$upstreamVersion")
    shade("me.sorihiko.dango:ui:$upstreamVersion")
    shade(kotlin("stdlib"))
    shade("javazoom:jlayer:1.0.1")
    shade("org.jcraft:jorbis:0.0.17")
    shade("com.google.zxing:core:3.3.3")
}

val legacySources = file("../src/main/kotlin").toPath().normalize()
val versionSpecific = setOf("LegacyPlatform.kt", "LegacyChatCopy.kt", "LegacyMinecraft.kt")
kotlin {
    jvmToolchain(8)
    sourceSets.named("main") {
        kotlin.srcDir(legacySources)
        kotlin.exclude { it.name in versionSpecific && it.file.toPath().normalize().startsWith(legacySources) }
    }
}

tasks.processResources {
    from("../src/main/resources") {
        exclude("dangomusic-build.properties")
        filesMatching("assets/dangomusic/lang/*.lang") { name = name.lowercase() }
    }
    from("../../LICENSE") { into("META-INF/licenses") }
    inputs.property("version", portVersion)
    inputs.property("mcversion", minecraftVersion)
    filesMatching(listOf("mcmod.info", "version.properties", "dangomusic-build.properties")) {
        expand(mapOf("version" to portVersion, "mcversion" to minecraftVersion, "target" to minecraftVersion))
    }
}

val shadowJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    archiveFileName.set("DangoMusic-$portVersion+$minecraftVersion.jar")
    relocate("kotlin", "me.sorihiko.dango.shadow.kotlin")
    relocate("org.intellij.lang.annotations", "me.sorihiko.dango.shadow.annotations.intellij")
    relocate("org.jetbrains.annotations", "me.sorihiko.dango.shadow.annotations.jetbrains")
    relocate("javazoom", "me.sorihiko.dango.shadow.javazoom")
    relocate("com.jcraft", "me.sorihiko.dango.shadow.jcraft")
    relocate("com.google.zxing", "me.sorihiko.dango.shadow.zxing")
    exclude("module-info.class", "META-INF/versions/**", "META-INF/*.kotlin_module")
}

@Suppress("UNCHECKED_CAST")
(extensions.getByName("reobf") as NamedDomainObjectContainer<Any>).create("shadowJar")

tasks.jar { destinationDirectory.set(layout.buildDirectory.dir("intermediates")) }
tasks.named("reobfShadowJar") { dependsOn(shadowJar) }
tasks.assemble { dependsOn("reobfShadowJar") }
