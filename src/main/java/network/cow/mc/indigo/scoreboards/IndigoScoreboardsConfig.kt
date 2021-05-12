package network.cow.mc.indigo.scoreboards

/**
 * @author Tobias Büser
 */
const val SCOREBOARD_TEAMNAME_PREFIX = "r_"

data class IndigoScoreboardsConfig(val defaultColor: String, val roleConfigs: Map<String, Role>) {

    data class Role(val name: String, val prefix: String, val userColor: String, val suffix: String)

}
