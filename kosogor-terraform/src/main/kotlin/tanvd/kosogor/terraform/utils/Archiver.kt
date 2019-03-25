package tanvd.kosogor.terraform.utils

import org.codehaus.plexus.archiver.tar.TarGZipUnArchiver
import org.codehaus.plexus.archiver.zip.ZipUnArchiver
import org.codehaus.plexus.logging.console.ConsoleLogger
import java.io.File

enum class Archiver(val extension: String) {
    ZIP("zip") {
        override fun unarchive(from: File, to: File) {
            to.mkdirs()
            ZipUnArchiver(from).apply {
                enableLogging(ConsoleLogger(5, "Archiver"))
                sourceFile = from
                destDirectory = to
            }.extract()
        }
    },
    TARGZ("tar.gz") {
        override fun unarchive(from: File, to: File) {
            to.mkdirs()
            TarGZipUnArchiver(from).apply {
                enableLogging(ConsoleLogger(5, "Archiver"))
                sourceFile = from
                destDirectory = to
            }.extract()
        }
    };

    abstract fun unarchive(from: File, to: File)
}
