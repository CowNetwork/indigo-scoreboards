package network.cow.mc.indigo.scoreboards

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import network.cow.grape.Grape
import network.cow.indigo.client.spigot.api.IndigoService
import network.cow.indigo.client.spigot.api.event.RolesUpdateEvent
import network.cow.mooapis.indigo.v1.Role
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scoreboard.Scoreboard
import org.bukkit.scoreboard.Team
import java.util.UUID

/**
 * @author Tobias Büser
 */
class IndigoScoreboardsPlugin : JavaPlugin(), Listener {

    lateinit var indigoService: IndigoService
    private lateinit var scoreboardsConfig: IndigoScoreboardsConfig

    internal val displayableRoles = mutableMapOf<String, ScoreboardRole>()
    internal val displayedRoles = mutableMapOf<UUID, ScoreboardRole>()

    override fun onEnable() {
        this.scoreboardsConfig = IndigoScoreboardsConfig(this.config.getString("defaultColor", "000000")!!)

        Bukkit.getPluginManager().registerEvents(ScoreboardListener(this), this)

        Grape.getInstance().get(IndigoService::class.java).thenAccept { service ->
            this.indigoService = service
            logger.info("Loaded service from Grape.")

            Bukkit.getScheduler().runTask(this, Runnable {
                this.reloadDisplayableRoles()
            })
        }
    }

    private fun reloadDisplayableRoles() {
        displayableRoles.clear()

        // clean teams
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard
        scoreboard.teams.filter { it.name.startsWith(SCOREBOARD_TEAMNAME_PREFIX) }.forEach {
            it.unregister()
        }

        indigoService.listRoles().forEach {
            this.updateTeam(it, RolesUpdateEvent.Action.ADD)
        }
    }

    private fun createTeam(scoreboard: Scoreboard, role: Role): Team {
        val teamName = this.getTeamName(role)
        val team = scoreboard.registerNewTeam(teamName)

        val color = role.color.toColor()
        val bukkitColor = NamedTextColor.nearestTo(TextColor.color(color.red, color.green, color.blue))

        team.color(NamedTextColor.GRAY)
        team.prefix(Component.text(role.name + " ", bukkitColor))

        return team
    }

    private fun getTeamName(role: Role): String {
        return "${SCOREBOARD_TEAMNAME_PREFIX}${role.priority}_${role.id.take(8)}".take(16)
    }

    internal fun updateTeam(role: Role, action: RolesUpdateEvent.Action) {
        val scoreboard = Bukkit.getScoreboardManager().mainScoreboard

        when (action) {
            RolesUpdateEvent.Action.ADD -> {
                val displayableRole = this.displayableRoles[role.id]
                if (displayableRole != null) {
                    // already exist
                    return
                }

                val team = this.createTeam(scoreboard, role)

                val scoreboardRole = ScoreboardRole(role, team)
                displayableRoles[role.id] = scoreboardRole
            }
            RolesUpdateEvent.Action.REMOVE -> {
                val displayableRole = this.displayableRoles[role.id] ?: return
                displayableRole.team.unregister()

                displayableRoles.remove(role.id)
            }
            RolesUpdateEvent.Action.UPDATE -> {
                val displayableRole = this.displayableRoles[role.id] ?: return
                val team = displayableRole.team

                if (displayableRole.role.name != role.name) {
                    // role changed, we delete the previous team and add a new one
                    this.updateTeam(role, RolesUpdateEvent.Action.REMOVE)
                    this.updateTeam(role, RolesUpdateEvent.Action.ADD)
                    return
                }

                val color = role.color.toColor()
                val bukkitColor = NamedTextColor.nearestTo(TextColor.color(color.red, color.green, color.blue))

                team.color(NamedTextColor.GRAY)
                team.prefix(Component.text(role.name + " ", bukkitColor))
            }
        }
    }

    private fun String.toColor(): Color {
        if (this.startsWith("#")) return this.drop(1).toColor()
        if (this.length < 6) {
            return Color.WHITE
        }

        return try {
            Color.fromRGB(
                Integer.valueOf(substring(0, 2), 16),
                Integer.valueOf(substring(2, 4), 16),
                Integer.valueOf(substring(4, 6), 16)
            )
        } catch (ex: Exception) {
            Color.WHITE
        }
    }

}
