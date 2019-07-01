import org.javacord.api.DiscordApi
import org.javacord.api.entity.channel.TextChannel
import org.javacord.api.entity.message.MessageAuthor
import org.javacord.api.entity.permission.PermissionsBuilder
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.Future

class Markovbot(apiFut: Future<DiscordApi?>) {
    private val api = apiFut.get() ?: throw NullPointerException("Discord API not initialized!")

    private fun mention(author: MessageAuthor) = author.asUser().get().nicknameMentionTag

    private fun showAvailableChains(channel: TextChannel, author: MessageAuthor, message: String) {
        val prefix = message.split(" ")
            .drop(1)
            .foldRight(".*") { s, acc -> s + acc }.trim()
        val files = Config.MARKOV_DIR.toFile()
            .listFiles { f ->
                val nameMatches = f.nameWithoutExtension.matches(Regex(prefix))
                nameMatches && f.extension == "json"
            }
            .map { f -> f.nameWithoutExtension }
            .joinToString("\n")
        channel.sendMessage("${mention(author)}\n```\n$files\n```")
    }

    private fun pollMarkovChain(channel: TextChannel, author: MessageAuthor, message: String) {
        val command = message.split(" ")
        val chain = command.drop(1).first()
        val chainWithExtension = "$chain.json"
        val chainPath = Paths.get("${Config.MARKOV_DIR}${File.separator}$chainWithExtension")
        val exists = Files.exists(chainPath)
        if (!exists) {
            displayError(channel, author, "Could not find markov chain with name: '$chain'.")
            return
        }
        val amount = command.drop(2).first().toIntOrNull()
        if (amount == null) {
            displayError(
                channel, author, "Could not parse poll amount as integer, " +
                        "given: ${command.drop(2).first()}"
            )
            return
        }
        try {
            val cmd = "${Config.MARKOV_PYTHON_EXECUTABLE} ${Config.MARKOV_SCRIPT} " +
                    "${Config.MARKOV_DIR}${File.separator}$chainWithExtension $amount"
            val p = Runtime.getRuntime().exec(cmd)
            val errorReader = BufferedReader(InputStreamReader(p.errorStream))
            val esj = StringJoiner(System.getProperty("line.separator"))
            val reader = BufferedReader(InputStreamReader(p.inputStream))
            val sj = StringJoiner(System.getProperty("line.separator"))
            reader.lines().iterator().forEachRemaining { sj.add(it) }
            errorReader.lines().iterator().forEachRemaining { esj.add(it) }
            p.waitFor()
            p.destroy()
            val output = sj.toString()
            val errors = esj.toString()
            if (errors.isNotBlank()) {
                displayError(channel, author, errors)
            } else {
                channel.sendMessage("${mention(author)}\n```\n$output\n```")
            }
        } catch (e: Exception) {
            displayError(channel, author, e.message!!)
        }
    }

    private fun displayError(channel: TextChannel, author: MessageAuthor, error: String) {
        channel.sendMessage("${mention(author)} Error:\n```$error```\n")
    }

    init {
        api.addMessageCreateListener { event ->
            val author = event.messageAuthor!!
            val messageContent = event.messageContent!!
            val channel = event.channel!!
            if (event.message.mentionedUsers.none { u -> u.isYourself } || !messageContent.startsWith("<@")) {
                return@addMessageCreateListener
            }
            val msg = messageContent
                .split(" ")
                .drop(1)
                .joinToString(" ").trim()
            when {
                msg.toLowerCase().startsWith("ls") -> showAvailableChains(channel, author, msg)
                msg.toLowerCase().startsWith("poll") -> pollMarkovChain(channel, author, msg)
            }
        }
        println("Invite me to your server: ${api.createBotInvite(PermissionsBuilder().setAllAllowed().build())}")
    }
}