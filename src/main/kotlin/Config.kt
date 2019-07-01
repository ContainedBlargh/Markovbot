import org.ini4j.Ini
import java.io.File
import java.lang.Exception
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

object Config {
    class ConfigurationException(message: String) : Exception(message)

    private val ini: Ini = Ini(File("./config.ini"))

    val API_TOKEN = ini.get("API", "TOKEN")
        ?: throw ConfigurationException("API_TOKEN not defined!")

    val MARKOV_DIR = Paths.get(ini.get("MARKOV", "DIR"))
        ?: throw ConfigurationException("MARKOV_DIR not defined!")
    val MARKOV_SCRIPT = ini.get("MARKOV", "SCRIPT")
        ?: throw ConfigurationException("MARKOV_SCRIPT not defined!")
    val MARKOV_PYTHON_EXECUTABLE: String = ini.get("MARKOV", "PYTHON_EXECUTABLE")
        ?: throw ConfigurationException("\"MARKOV_PYTHON_EXECUTABLE is not defined!")
}