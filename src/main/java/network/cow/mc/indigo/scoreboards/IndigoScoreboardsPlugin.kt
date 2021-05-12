package network.cow.mc.indigo.scoreboards

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import network.cow.grape.Grape
import network.cow.indigo.client.spigot.api.IndigoService
import network.cow.indigo.client.spigot.api.event.RolesUpdateEvent
import network.cow.mooapis.indigo.v1.Role
import org.bukkit.Bukkit
import org.bukkit.ChatColor
import org.bukkit.Color
import org.bukkit.event.Listener
import org.bukkit.plugin.java.JavaPlugin
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
        val roleConfigMap = mutableMapOf<String, IndigoScoreboardsConfig.Role>()
        this.config.getList("teams")?.forEach {
            it as Map<*, *>

            val roleName = it["role"]?.toString()
            val prefix = it.getOrDefault("prefix", "").toString()
            val userColor = it.getOrDefault("color", "FFFFFF").toString()
            val suffix = it.getOrDefault("suffix", "").toString()

            if (roleName == null) return@forEach

            val roleConfig = IndigoScoreboardsConfig.Role(
                roleName, prefix, userColor, suffix,
            )
            roleConfigMap[roleName] = roleConfig
        }

        this.scoreboardsConfig = IndigoScoreboardsConfig(
            this.config.getString("defaultColor", "FFFFFF")!!,
            roleConfigMap
        )

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

    private fun applyConfigToTeam(team: Team, role: Role) {
        val roleConfig = this.scoreboardsConfig.roleConfigs[role.name] ?: IndigoScoreboardsConfig.Role(
            role.name, role.name + " ", "", ""
        )

        val color = roleConfig.userColor.ifEmpty { role.color }
        val bukkitColor = color.toNamedTextColor()

        team.color(bukkitColor)

        if (roleConfig.prefix.isNotEmpty()) {
            val prefix = roleConfig.prefix.replace("%roleColor%", role.color.toNamedTextColor().toChatColor().toString())
            team.prefix(Component.text(prefix))
        }
        if (roleConfig.suffix.isNotEmpty()) {
            val suffix = roleConfig.suffix.replace("%roleColor%", role.color.toNamedTextColor().toChatColor().toString())
            team.suffix(Component.text(suffix))
        }
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

                val teamName = this.getTeamName(role)
                val team = scoreboard.registerNewTeam(teamName)
                this.applyConfigToTeam(team, role)

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

                this.applyConfigToTeam(team, role)
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

    private fun String.toNamedTextColor(): NamedTextColor {
        val color = this.toColor()
        return NamedTextColor.nearestTo(TextColor.color(color.red, color.green, color.blue))
    }

    private fun NamedTextColor.toChatColor(): ChatColor {
        return when (this) {
            NamedTextColor.BLACK -> ChatColor.BLACK
            NamedTextColor.DARK_BLUE -> ChatColor.DARK_BLUE
            NamedTextColor.DARK_GREEN -> ChatColor.DARK_GREEN
            NamedTextColor.DARK_AQUA -> ChatColor.DARK_AQUA
            NamedTextColor.DARK_RED -> ChatColor.DARK_RED
            NamedTextColor.DARK_PURPLE -> ChatColor.DARK_PURPLE
            NamedTextColor.GOLD -> ChatColor.GOLD
            NamedTextColor.GRAY -> ChatColor.GRAY
            NamedTextColor.DARK_GRAY -> ChatColor.DARK_GRAY
            NamedTextColor.BLUE -> ChatColor.BLUE
            NamedTextColor.GREEN -> ChatColor.GREEN
            NamedTextColor.AQUA -> ChatColor.AQUA
            NamedTextColor.RED -> ChatColor.RED
            NamedTextColor.LIGHT_PURPLE -> ChatColor.LIGHT_PURPLE
            NamedTextColor.YELLOW -> ChatColor.YELLOW
            NamedTextColor.WHITE -> ChatColor.WHITE
            else -> ChatColor.RESET
        }
    }

}
