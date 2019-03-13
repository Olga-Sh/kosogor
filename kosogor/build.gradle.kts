import tanvd.kosogor.proxy.publishJar
import tanvd.kosogor.proxy.publishPlugin

group = "tanvd.kosogor"
version = "1.0.2-SNAPSHOT"

dependencies {
    compile(gradleKotlinDsl())
    compile(gradleApi())
    compile("org.jfrog.buildinfo", "build-info-extractor-gradle", "4.7.5")
    compile("com.jfrog.bintray.gradle", "gradle-bintray-plugin", "1.8.4")
    compile("com.github.jengelman.gradle.plugins", "shadow", "4.0.4")
    compile("com.gradle.publish", "plugin-publish-plugin", "0.10.1")
}

publishPlugin {
    id = "tanvd.kosogor"
    displayName = "kosogor"
    implementationClass = "tanvd.kosogor.KosogorPlugin"
    version = project.version.toString()

    info {
        website = "https://github.com/TanVD/kosogor"
        vcsUrl = "https://github.com/TanVD/kosogor"
        description = "Reasonable defaults and simplified Kotlin-DSL interfaces for everyday development"
        tags.addAll(listOf("default", "common", "kotlin", "jar", "shadowjar", "artifactory", "idea"))
    }
}

publishJar {
    publication {
        artifactId = "tanvd.kosogor.gradle.plugin"
    }
}

