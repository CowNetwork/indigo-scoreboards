package network.cow.mc.indigo.scoreboards

import network.cow.indigo.client.spigot.api.event.PermissionUpdateEvent
import network.cow.indigo.client.spigot.api.event.RolesUpdateEvent
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent

/**
 * @author Tobias Büser
 */
class ScoreboardListener(private val plugin: IndigoScoreboardsPlugin) : Listener {

    @EventHandler
    fun onRoleUpdate(event: RolesUpdateEvent) {
        val updateEntries = event.entries

        updateEntries.forEach { plugin.updateTeam(it.role, it.action) }
    }

    @EventHandler
    fun onPermissionUpdate(event: PermissionUpdateEvent) {
        val player = event.player
        val indigoUser = plugin.indigoService.getUser(player.uniqueId) ?: return

        val previousDisplayed = plugin.displayedRoles[player.uniqueId]
        val newDisplayed = plugin.displayableRoles[indigoUser.getTopRole()?.id]

        if (previousDisplayed == newDisplayed) {
            // do nothing if the previous is equal to the new role
            return
        }

        previousDisplayed?.team?.removeEntry(player.name)
        if (newDisplayed != null) {
            newDisplayed.team.addEntry(player.name)

            plugin.displayedRoles[player.uniqueId] = newDisplayed
        }
    }

    @EventHandler
    fun onQuit(event: PlayerQuitEvent) {
        val player = event.player
        val previousDisplayed = plugin.displayedRoles[player.uniqueId] ?: return

        previousDisplayed.team.removeEntry(player.name)
        plugin.displayedRoles.remove(player.uniqueId)
    }

}
