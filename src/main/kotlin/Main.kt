import org.javacord.api.DiscordApiBuilder

object Main {
    @JvmStatic
    fun main(args: Array<String>) {
        val apiFut = DiscordApiBuilder().setToken(Config.API_TOKEN).login()
        Markovbot(apiFut)
    }
}