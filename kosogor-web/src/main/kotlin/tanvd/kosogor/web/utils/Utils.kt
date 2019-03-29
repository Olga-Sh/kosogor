package tanvd.kosogor.web.utils

import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/** Apply a plugin if it is not already applied. */
internal fun Project.applyPluginSafely(id: String) {
    if (!plugins.hasPlugin(id)) {
        apply(plugin = id)
    }
}

internal val Project.fullName: String
    get() = ":$name"

