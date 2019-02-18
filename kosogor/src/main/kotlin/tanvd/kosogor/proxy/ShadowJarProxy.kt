package tanvd.kosogor.proxy

import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenArtifact
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.withType
import tanvd.kosogor.utils.applyPluginSafely
import java.io.File


class ShadowJarProxy(private val project: Project) {
    data class ShadowJarConfig(
            /** Name to use for result archive. Version will not be added. */
            var archiveName: String? = null,
            /** Main class of shadow jar to add to Manifest */
            var mainClass: String? = null,
            /** Destination directory of shadow jar file*/
            var destinationDir: File? = null
    )

    fun artifact(pub: MavenPublication): MavenArtifact = pub.artifact(project.tasks.withType<ShadowJar>().single())

    internal val jarConfig = ShadowJarConfig()
    fun jar(configure: ShadowJarConfig.() -> Unit) {
        jarConfig.configure()
    }
}

/**
 * Provides simple interface to shadowJar plugin through proxy
 *
 * Will apply `com.github.johnrengelman.shadow` if it is not already applied
 */
fun Project.shadowJar(configure: ShadowJarProxy.() -> Unit): ShadowJarProxy {
    val config = ShadowJarProxy(this).apply { configure() }

    applyPluginSafely("com.github.johnrengelman.shadow")

    tasks.withType<ShadowJar> {
        config.jarConfig.archiveName?.let {
            archiveFileName.set(it)
        }
        config.jarConfig.mainClass?.let { cls ->
            manifest {
                it.attributes(mapOf("Main-Class" to cls))
            }
        }
        config.jarConfig.destinationDir?.let {
            destinationDirectory.set(it)
        } ?: destinationDirectory.set(File(project.rootProject.projectDir, "build/shadow"))
    }

    return config
}
