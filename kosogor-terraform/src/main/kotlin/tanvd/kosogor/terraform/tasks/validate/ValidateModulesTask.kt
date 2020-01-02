package tanvd.kosogor.terraform.tasks.validate

import com.beust.klaxon.Klaxon
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import tanvd.kosogor.terraform.PackageInfo
import tanvd.kosogor.terraform.terraformDsl
import tanvd.kosogor.terraform.utils.*
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption.COPY_ATTRIBUTES

/**
 * Task validates correctness of terraform modules.
 *
 * It uses terraform itself to check correctness.
 *
 * Basically, inside each module will be added `infra.tf`
 * file with specified in DSL aws provider and `terraform validate`
 * will be executed for this module.
 *
 * Task guarantees syntax correctness of module, and it's
 * compliance to specified version of aws provider.
 */
open class ValidateModulesTask : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
        project.afterEvaluate {
            dependsOn(GlobalTask.tfDownload)
        }
    }

    @TaskAction
    fun validateModules() {
        project.projectDir
                .walk()
                .filter { it.absolutePath.endsWith("/package.json") }
                .forEach { file ->
                    val workingDir = file.parentFile

                    val providerFile = addProviderIfNeeded(workingDir)

                    val packageInfo = Klaxon().parse<PackageInfo>(file.readText())!!

                    try {
                        init(workingDir)
                        validate(workingDir)
                    } finally {
                        File(file.parentFile, ".terraform").deleteRecursively()
                        providerFile?.delete()
                    }
                    println("Validated module ${packageInfo.group}:${packageInfo.name}:${packageInfo.version}")
                }
    }

    private fun init(workingDir: File) {
        val curInitDir = File(workingDir, ".terraform")
        if (terraformDsl.validater.cacheInitPlugins && GlobalFile.tfInitDir.exists() && !curInitDir.exists()) {
            curInitDir.mkdirs()
            File(GlobalFile.tfInitDir, "plugins").walk().forEach {
                val targetPath = File(curInitDir.absolutePath + it.absolutePath.replace(GlobalFile.tfInitDir.absolutePath, "")).toPath()
                Files.copy(it.toPath(), targetPath, COPY_ATTRIBUTES)
            }
        }
        val retInit = CommandLine.execute(GlobalFile.tfBin.absolutePath, listOf("init"), workingDir, true)
        if (retInit != 0) {
            error("Terraform init failed")
        }

        if (terraformDsl.validater.cacheInitPlugins && !GlobalFile.tfInitDir.exists()) {
            GlobalFile.tfInitDir.mkdirs()
            File(curInitDir, "plugins").walk().forEach {
                val targetPath = File(GlobalFile.tfInitDir.absolutePath + it.absolutePath.replace(curInitDir.absolutePath, "")).toPath()
                Files.copy(it.toPath(), targetPath, COPY_ATTRIBUTES)
            }
        }
    }

    private fun validate(workingDir: File) {
        val tfVersion = terraformDsl.config.tfVersion.replace(".", "").toInt()
        val args = when (tfVersion < 1200) {
            true -> listOf("validate", "-check-variables=false")
            else -> listOf("validate")
        }
        val retValidate = CommandLine.execute(GlobalFile.tfBin.absolutePath, args,
                workingDir, true)
        if (retValidate != 0) {
            error("Terraform validate failed")
        }
    }

    private fun addProviderIfNeeded(workingDir: File): File? {
        val allFiles = workingDir.listFiles()
        val alreadyGotProvider = allFiles.any {
            it.isFile && it.absolutePath.endsWith(".tf") &&
                    it.readText().contains(Regex("provider\\s*\"aws\""))
        }
        return if (!alreadyGotProvider) {
            val name = "provider"
            var index = 0
            while (allFiles.map { it.name }.any { it == "${name}_$index.tf" }) {
                index++
            }
            File(workingDir, "${name}_$index.tf").apply {
                writeText(
                        """
                        | provider "aws" {
                        |   region = "${terraformDsl.config.awsRegion}"
                        |   profile = "${terraformDsl.config.awsProfile}"
                        |   version = "${terraformDsl.config.awsProvider}"
                        | }
                        """.trimMargin()
                )
            }
        } else null
    }
}
