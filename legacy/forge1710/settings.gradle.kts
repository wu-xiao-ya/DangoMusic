pluginManagement {
    repositories {
        maven("https://nexus.gtnewhorizons.com/repository/public/") {
            content { includeGroupByRegex("com\\.gtnewhorizons(\\..*)?") }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}
rootProject.name = "DangoMusic-Forge1710"
includeBuild("../../common")
