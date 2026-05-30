pluginManagement {
    plugins {
        val indra = "3.1.3"
        id("net.kyori.indra") version indra
        id("net.kyori.indra.publishing") version indra
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.9.0"
}

rootProject.name = "simple-voice-chat-minestom"

