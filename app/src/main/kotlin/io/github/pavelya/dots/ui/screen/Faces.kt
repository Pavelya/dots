package io.github.pavelya.dots.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dots.campaign.Level
import dots.campaign.Strength
import io.github.pavelya.dots.R
import io.github.pavelya.dots.game.Mood
import io.github.pavelya.dots.ui.theme.FaceInks
import io.github.pavelya.dots.ui.theme.GameTheme

/** A boss's own faces (docs/design/illustrations.md): sure of itself, on its special turn, and beaten. */
@Immutable
data class BossFaces(val smug: FacePicture, val attacking: FacePicture, val beaten: FacePicture)

/**
 * One face in each theme's inks ([FaceInks]): Notebook's, cut from the owner's sheet, and the
 * Star chart and Neon arcade versions recoloured from it, each light and dark.
 */
@Immutable
data class FacePicture(val notebook: Picture, val starChart: Picture, val neonArcade: Picture) {
    fun of(inks: FaceInks): Picture = when (inks) {
        FaceInks.NOTEBOOK -> notebook
        FaceInks.STAR_CHART -> starChart
        FaceInks.NEON_ARCADE -> neonArcade
    }
}

/**
 * The opponent's faces, cut from the owner's sheets (docs/design/illustrations.md, "The computer"
 * and "The World 1 boss"): the computer's moods, its three strengths and the World 1 boss.
 *
 * The computer's plain look ([Mood.DETERMINED]) is its strength's face: sleepy for the beginner
 * and easy steps, calm for steady and normal, sharp with glasses for hard and expert. A boss with
 * its own faces is sure of itself until the player captures or wins, then beaten, and attacks on
 * its special turn. A boss without them keeps the computer's faces.
 */
object Faces {
    private val HAPPY = FacePicture(
        Picture(R.drawable.opponent_moods_happy, R.drawable.opponent_moods_happy_dark),
        Picture(R.drawable.opponent_moods_happy_star_chart, R.drawable.opponent_moods_happy_star_chart_dark),
        Picture(R.drawable.opponent_moods_happy_neon_arcade, R.drawable.opponent_moods_happy_neon_arcade_dark),
    )
    private val SMUG = FacePicture(
        Picture(R.drawable.opponent_moods_smug, R.drawable.opponent_moods_smug_dark),
        Picture(R.drawable.opponent_moods_smug_star_chart, R.drawable.opponent_moods_smug_star_chart_dark),
        Picture(R.drawable.opponent_moods_smug_neon_arcade, R.drawable.opponent_moods_smug_neon_arcade_dark),
    )
    private val UPSET = FacePicture(
        Picture(R.drawable.opponent_moods_upset, R.drawable.opponent_moods_upset_dark),
        Picture(R.drawable.opponent_moods_upset_star_chart, R.drawable.opponent_moods_upset_star_chart_dark),
        Picture(R.drawable.opponent_moods_upset_neon_arcade, R.drawable.opponent_moods_upset_neon_arcade_dark),
    )
    private val SULKY = FacePicture(
        Picture(R.drawable.opponent_moods_sulky, R.drawable.opponent_moods_sulky_dark),
        Picture(R.drawable.opponent_moods_sulky_star_chart, R.drawable.opponent_moods_sulky_star_chart_dark),
        Picture(R.drawable.opponent_moods_sulky_neon_arcade, R.drawable.opponent_moods_sulky_neon_arcade_dark),
    )
    private val SLEEPY = FacePicture(
        Picture(R.drawable.opponent_strengths_sleepy, R.drawable.opponent_strengths_sleepy_dark),
        Picture(R.drawable.opponent_strengths_sleepy_star_chart, R.drawable.opponent_strengths_sleepy_star_chart_dark),
        Picture(R.drawable.opponent_strengths_sleepy_neon_arcade, R.drawable.opponent_strengths_sleepy_neon_arcade_dark),
    )
    private val CALM = FacePicture(
        Picture(R.drawable.opponent_strengths_calm, R.drawable.opponent_strengths_calm_dark),
        Picture(R.drawable.opponent_strengths_calm_star_chart, R.drawable.opponent_strengths_calm_star_chart_dark),
        Picture(R.drawable.opponent_strengths_calm_neon_arcade, R.drawable.opponent_strengths_calm_neon_arcade_dark),
    )
    private val SHARP = FacePicture(
        Picture(R.drawable.opponent_strengths_sharp, R.drawable.opponent_strengths_sharp_dark),
        Picture(R.drawable.opponent_strengths_sharp_star_chart, R.drawable.opponent_strengths_sharp_star_chart_dark),
        Picture(R.drawable.opponent_strengths_sharp_neon_arcade, R.drawable.opponent_strengths_sharp_neon_arcade_dark),
    )

    /** World 1's boss, the Ink Bottle. */
    val INK_BOTTLE = BossFaces(
        smug = FacePicture(
            Picture(R.drawable.boss_world_1_smug, R.drawable.boss_world_1_smug_dark),
            Picture(R.drawable.boss_world_1_smug_star_chart, R.drawable.boss_world_1_smug_star_chart_dark),
            Picture(R.drawable.boss_world_1_smug_neon_arcade, R.drawable.boss_world_1_smug_neon_arcade_dark),
        ),
        attacking = FacePicture(
            Picture(R.drawable.boss_world_1_attacking, R.drawable.boss_world_1_attacking_dark),
            Picture(R.drawable.boss_world_1_attacking_star_chart, R.drawable.boss_world_1_attacking_star_chart_dark),
            Picture(R.drawable.boss_world_1_attacking_neon_arcade, R.drawable.boss_world_1_attacking_neon_arcade_dark),
        ),
        beaten = FacePicture(
            Picture(R.drawable.boss_world_1_beaten, R.drawable.boss_world_1_beaten_dark),
            Picture(R.drawable.boss_world_1_beaten_star_chart, R.drawable.boss_world_1_beaten_star_chart_dark),
            Picture(R.drawable.boss_world_1_beaten_neon_arcade, R.drawable.boss_world_1_beaten_neon_arcade_dark),
        ),
    )

    /** The pictures are cut at 4 pixels per dp for a face 108 dp tall (make_pictures.py). */
    const val PX_PER_DP = 4f
    val FULL_SIZE = 108.dp

    /** The faces of [level]'s boss, if it is a boss level and its boss has them. */
    fun boss(level: Level?): BossFaces? = if (level?.rule != null && level.world == 1) INK_BOTTLE else null

    /** The computer's face for how strong it plays; calm when the strength is not known. */
    fun strength(strength: Strength?): FacePicture = when {
        strength == null -> CALM
        strength <= Strength.EASY -> SLEEPY
        strength <= Strength.NORMAL -> CALM
        else -> SHARP
    }

    /** The face for [mood], against the computer at [strength] or a [boss] with its own faces. */
    fun of(mood: Mood, strength: Strength?, boss: BossFaces? = null): FacePicture = if (boss != null) {
        when (mood) {
            Mood.UPSET, Mood.SULKY -> boss.beaten
            Mood.ATTACKING -> boss.attacking
            Mood.HAPPY, Mood.SMUG, Mood.DETERMINED -> boss.smug
        }
    } else {
        when (mood) {
            Mood.HAPPY -> HAPPY
            Mood.SMUG, Mood.ATTACKING -> SMUG
            Mood.UPSET -> UPSET
            Mood.SULKY -> SULKY
            Mood.DETERMINED -> strength(strength)
        }
    }
}

/**
 * A face in a square [size] dp across, in the [inks] of the theme it is drawn in (the game screen
 * follows the theme; the map's notes pass Notebook's), light or dark as the theme draws. Every face is
 * drawn at one scale, [Faces.FULL_SIZE] for a full-size square, so the blot stays the same size
 * from mood to mood and a sleeping cap or a crown only reaches further out.
 */
@Composable
fun Face(picture: FacePicture, size: Dp, modifier: Modifier = Modifier, inks: FaceInks = GameTheme.current.faceInks) {
    val image: ImageBitmap = ImageBitmap.imageResource(picture.of(inks).of(GameTheme.isDark))
    val dpPerPx = size / Faces.FULL_SIZE / Faces.PX_PER_DP
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Image(
            image,
            contentDescription = null,
            filterQuality = FilterQuality.High,
            modifier = Modifier.size(width = dpPerPx.dp * image.width, height = dpPerPx.dp * image.height),
        )
    }
}
