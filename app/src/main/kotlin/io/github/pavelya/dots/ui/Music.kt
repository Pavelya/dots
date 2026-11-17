package io.github.pavelya.dots.ui

import dots.campaign.CampaignMap
import dots.campaign.Level
import io.github.pavelya.dots.audio.Track

/**
 * The music of the screen at [route] (docs/design/sound.md, "Music"): the map's track on the map
 * and everything over it, the game's own ([game]) on the board, none on the loading screen. Rules
 * opens over the map or over a game, and keeps the music that was [playing].
 */
internal fun musicFor(route: String?, game: Track?, playing: Track?): Track? = when (route) {
    null, Routes.LOADING -> null
    Routes.GAME -> game
    Routes.RULES -> playing
    else -> Track.MAP
}

/**
 * The music of a game: a boss level's track, or the track of the level's world. The daily puzzle
 * and free play ([vsComputer], no [level]) have the track of the world the player has reached
 * ([map]); two players have the notebook's.
 */
internal fun gameMusic(level: Level?, vsComputer: Boolean, map: CampaignMap?): Track = when {
    level != null && level.rule != null -> Track.BOSS
    level != null -> Track.ofWorld(level.world)
    !vsComputer -> Track.NOTEBOOK
    else -> Track.ofWorld(map?.worlds?.lastOrNull { it.isOpen }?.world?.number ?: 1)
}
