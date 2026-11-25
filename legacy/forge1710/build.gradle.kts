import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.gtnewhorizons.retrofuturagradle.mcp.ReobfuscatedJar
import java.util.Properties

plugins {
    id("com.gtnewhorizons.retrofuturagradle") version "1.4.9"
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

fun readVersion(path: String, key: String): String = Properties().apply {
    file(path).inputStream().use { load(it) }
}.getProperty(key)
val portVersion = readVersion("../forge112/gradle.properties", "portVersion")
val upstreamVersion = readVersion("../../version.properties", "version")
val minecraftVersion = "1.7.10"

group = "me.sorihiko.dango"
version = portVersion
repositories { mavenCentral() }
minecraft { mcVersion.set(minecraftVersion) }

val shade by configurations.creating {
    isCanBeResolved = true
    isCanBeConsumed = false
    attributes {
        attribute(Usage.USAGE_ATTRIBUTE, objects.named(Usage.JAVA_RUNTIME))
        attribute(LibraryElements.LIBRARY_ELEMENTS_ATTRIBUTE, objects.named(LibraryElements.JAR))
    }
}
configurations.implementation.get().extendsFrom(shade)
dependencies {
    shade("me.sorihiko.dango:core:$upstreamVersion")
    shade("me.sorihiko.dango:ui:$upstreamVersion")
    shade(kotlin("stdlib"))
    shade("javazoom:jlayer:1.0.1")
    shade("org.jcraft:jorbis:0.0.17")
    shade("com.google.zxing:core:3.3.3")
}

val sharedSources = file("../src/main/kotlin").toPath().normalize()
val platformSources = file("src/main/kotlin").toPath()
kotlin {
    jvmToolchain(8)
    sourceSets.named("main") {
        kotlin.srcDir(sharedSources)
        kotlin.exclude {
            val path = it.file.toPath().normalize()
            !it.isDirectory && path.startsWith(sharedSources) &&
                platformSources.resolve(sharedSources.relativize(path)).toFile().isFile
        }
    }
}

tasks.processResources {
    from("../src/main/resources") { exclude("mcmod.info", "dangomusic-build.properties") }
    from("../../LICENSE") { into("META-INF/licenses") }
    inputs.property("version", portVersion)
    inputs.property("mcversion", minecraftVersion)
    filesMatching(listOf("mcmod.info", "version.properties", "dangomusic-build.properties")) {
        expand(mapOf("version" to portVersion, "mcversion" to minecraftVersion, "target" to minecraftVersion))
    }
}

val shadedJar = tasks.named<ShadowJar>("shadowJar") {
    configurations = listOf(shade)
    destinationDirectory.set(layout.buildDirectory.dir("intermediates"))
    archiveClassifier.set("shaded-dev")
    relocate("kotlin", "me.sorihiko.dango.shadow.kotlin")
    relocate("org.intellij.lang.annotations", "me.sorihiko.dango.shadow.annotations.intellij")
    relocate("org.jetbrains.annotations", "me.sorihiko.dango.shadow.annotations.jetbrains")
    relocate("javazoom", "me.sorihiko.dango.shadow.javazoom")
    relocate("com.jcraft", "me.sorihiko.dango.shadow.jcraft")
    relocate("com.google.zxing", "me.sorihiko.dango.shadow.zxing")
    exclude("module-info.class", "META-INF/versions/**", "META-INF/*.kotlin_module")
}
tasks.jar { destinationDirectory.set(layout.buildDirectory.dir("intermediates")) }
tasks.named<ReobfuscatedJar>("reobfJar") {
    inputJar.set(shadedJar.flatMap { it.archiveFile })
    destinationDirectory.set(layout.buildDirectory.dir("libs"))
    archiveFileName.set("DangoMusic-$portVersion+$minecraftVersion.jar")
}
