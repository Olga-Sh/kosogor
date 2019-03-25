package tanvd.kosogor.terraform.tasks.lint

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import tanvd.kosogor.terraform.TerraformDsl
import tanvd.kosogor.terraform.terraformDsl
import tanvd.kosogor.terraform.utils.*
import java.io.File

open class LintRootTask : DefaultTask() {
    init {
        outputs.upToDateWhen { false }
        project.afterEvaluate {
            dependsOn(GlobalTask.prepareJars, GlobalTask.prepareRemotes, GlobalTask.tfLintDownload, GlobalTask.tfDownload)
        }
    }

    @get:Input
    lateinit var root: File

    @TaskAction
    fun lintDir() {
        when (terraformDsl.linter.linter) {
            TerraformDsl.Linter.LinterType.Terraform -> {
                val retInit = CommandLine.execute(GlobalFile.tfBin.absolutePath, listOf("init"), root)
                if (retInit != 0) {
                    error("Terraform init failed (during linting)")
                }

                val retPlan = CommandLine.execute(GlobalFile.tfBin.absolutePath, listOf("plan"), root)
                if (retPlan != 0) {
                    error("Terraform plan failed (during linting)")
                }
            }
            TerraformDsl.Linter.LinterType.TfLint -> {
                val retLint = CommandLine.execute(GlobalFile.tfLintBin.absolutePath, emptyList(), root)
                if (retLint != 0) {
                    error("TfLint failed")
                }
            }
        }
    }
}
