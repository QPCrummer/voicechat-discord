import java.io.File

object Changelog {
    fun get(file: File): String {
        var sectionContents = ""
        var foundSection = false
        for (line in file.readLines()) {
            if (line.startsWith("##")) {
                if (line.contains(Properties.pluginVersion)) {
                    foundSection = true
                } else if (foundSection) {
                    break;
                }
            } else if (foundSection) {
                sectionContents += "$line\n"
            }
        }
        val stripped = sectionContents.trim()
        println("Changelog:\n$stripped")

        return stripped
    }
}