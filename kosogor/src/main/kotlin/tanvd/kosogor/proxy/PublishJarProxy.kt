package tanvd.kosogor.proxy

import groovy.lang.GroovyObject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.AbstractCopyTask
import org.gradle.api.tasks.bundling.Jar
import org.gradle.kotlin.dsl.delegateClosureOf
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.task
import org.jfrog.gradle.plugin.artifactory.dsl.PublisherConfig
import tanvd.kosogor.utils._artifactory
import tanvd.kosogor.utils._publishing
import tanvd.kosogor.utils._sourceSets
import tanvd.kosogor.utils.applyPluginSafely

class PublishJarProxy {
    data class JarConfig(
            /** Name of maven publication to create */
            var publication: String = "jarPublication",
            /** Components to add to publication */
            var components: MavenPublication.(Project) -> Unit = { from(it.components.getByName("java")) }
    )

    internal var jarEnable: Boolean = true
    internal val jarConfig = JarConfig()
    fun jar(configure: JarConfig.() -> Unit) {
        jarEnable = true
        jarConfig.configure()
    }


    data class SourcesConfig(
            /** Name of jar task for sources to create */
            var task: String = "sourcesJar",
            /** Components to add to sources jar */
            var components: AbstractCopyTask.(Project) -> Unit = { from(it._sourceSets["main"]!!.allSource) }
    )

    internal var sourcesEnable: Boolean = true
    internal val sourcesConfig = SourcesConfig()
    fun sources(configure: SourcesConfig.() -> Unit) {
        sourcesEnable = true
        sourcesConfig.configure()
    }


    data class ArtifactoryConfig(
            /**
             * URL of artifactory server
             * If not set, will be taken from System environment param `artifactory_url`
             */
            var artifactoryUrl: String? = System.getenv("artifactory_url"),
            /**
             * Maven repo on artifactory server
             * If not set, will be taken from System environment param `artifactory_repo`
             */
            var artifactoryRepo: String? = System.getenv("artifactory_repo"),
            /**
             * Artifactory user name to use
             * If not set, will be taken from System environment param `artifactory_user`
             */
            var artifactoryUser: String? = System.getenv("artifactory_user"),
            /**
             * Artifactory secret key to use
             * If not set, will be taken from System environment param `artifactory_key`
             */
            var artifactoryKey: String? = System.getenv("artifactory_key"),
            /** Should published artifact include pom.xml */
            var publishPom: Boolean = true
    )

    internal var artifactoryEnable: Boolean = false
    internal val artifactoryConfig = ArtifactoryConfig()
    fun artifactory(configure: ArtifactoryConfig.() -> Unit) {
        artifactoryEnable = true
        artifactoryConfig.configure()
    }
}

/**
 * Provides simple interface to jar, maven-publish and artifactory plugin through proxy
 *
 * Will apply `maven-publish` if it is not already applied and `jar { ... }` is used
 * Will apply `com.jfrog.artifactory` if it is not already applied and `artifactory { ... }` is used
 */
fun Project.publishJar(configure: PublishJarProxy.() -> Unit): PublishJarProxy {
    val config = PublishJarProxy().apply { configure() }

    if (config.sourcesEnable) {
        task<Jar>(config.sourcesConfig.task) {
            archiveClassifier.set("sources")
            config.sourcesConfig.components(this, project)
        }
    }

    if (config.jarEnable) {
        applyPluginSafely("maven-publish")
        _publishing {
            publications.create(
                    config.jarConfig.publication,
                    MavenPublication::class.java,
                    Action<MavenPublication> { t ->
                        config.jarConfig.components(t, project)
                        if (config.sourcesEnable) {
                            t.artifact(tasks[config.sourcesConfig.task])
                        }
                    })
        }
    }

    if (config.artifactoryEnable) {
        applyPluginSafely("com.jfrog.artifactory")
        _artifactory {
            setContextUrl(config.artifactoryConfig.artifactoryUrl)

            publish(delegateClosureOf<PublisherConfig> {
                repository(delegateClosureOf<GroovyObject> {
                    setProperty("repoKey", config.artifactoryConfig.artifactoryRepo)
                    setProperty("username", config.artifactoryConfig.artifactoryUser)
                    setProperty("password", config.artifactoryConfig.artifactoryKey)
                    setProperty("maven", true)
                })
                defaults(delegateClosureOf<GroovyObject> {
                    setProperty("publishArtifacts", true)
                    setProperty("publishPom", config.artifactoryConfig.publishPom)
                    invokeMethod("publications", config.jarConfig.publication)
                })
            })
        }
    }
    return config
}
