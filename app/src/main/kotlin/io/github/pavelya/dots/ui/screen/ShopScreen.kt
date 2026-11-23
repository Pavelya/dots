package io.github.pavelya.dots.ui.screen

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dots.campaign.Cosmetic
import dots.engine.GameState
import dots.engine.Pos
import io.github.pavelya.dots.R
import io.github.pavelya.dots.data.LookPart
import io.github.pavelya.dots.data.PieceStyleId
import io.github.pavelya.dots.data.RingEffectId
import io.github.pavelya.dots.data.ThemeId
import io.github.pavelya.dots.shop.BuyOutcome
import io.github.pavelya.dots.shop.Owned
import io.github.pavelya.dots.shop.Selling
import io.github.pavelya.dots.shop.StoreState
import io.github.pavelya.dots.ui.theme.Palette

/** The Shop's tabs, in the wireframes' order, each with the looks it sells, the default first. */
enum class ShopTab(val key: String, @param:StringRes val title: Int) {
    THEMES("themes", R.string.shop_themes),
    PIECES("pieces", R.string.settings_pieces),
    RINGS("rings", R.string.settings_rings),
    ;

    val items: List<LookPart>
        get() = when (this) {
            THEMES -> ThemeId.entries
            PIECES -> PieceStyleId.entries
            RINGS -> RingEffectId.entries
        }

    companion object {
        fun of(key: String?): ShopTab = entries.firstOrNull { it.key == key } ?: THEMES
    }
}

/** The tab that sells [part]. */
val LookPart.tab: ShopTab
    get() = when (this) {
        is ThemeId -> ShopTab.THEMES
        is PieceStyleId -> ShopTab.PIECES
        is RingEffectId -> ShopTab.RINGS
    }

/** The name of a theme, a piece style or a ring effect, as the Shop and Settings call it. */
@StringRes
fun lookName(part: LookPart): Int = when (part) {
    is ThemeId -> themeName(part)
    is PieceStyleId -> pieceStyleName(part)
    is RingEffectId -> ringEffectName(part)
}

/** Its one line, as on its card in Settings. */
@StringRes
private fun lookLine(part: LookPart): Int = when (part) {
    is ThemeId -> themeLine(part)
    is PieceStyleId -> pieceStyleLine(part)
    is RingEffectId -> ringEffectLine(part)
}

/**
 * What the Shop shows the player (docs/design/money.md; flows.md, "Shop and rewarded ads"): what
 * Google Play says ([store]), the cosmetics the player [owned], their campaign [stars], the
 * [theme], [pieces] and [rings] in use, and the earned items whose preview is not opened yet
 * ([fresh], marked New).
 */
data class ShopView(
    val store: StoreState,
    val owned: Owned,
    val stars: Int,
    val theme: ThemeId = ThemeId.DEFAULT,
    val pieces: PieceStyleId = PieceStyleId.DEFAULT,
    val rings: RingEffectId = RingEffectId.DEFAULT,
    val fresh: Set<Cosmetic> = emptySet(),
) {
    /** Whether [part] is in use. */
    fun inUse(part: LookPart): Boolean = part == theme || part == pieces || part == rings

    /** What [part]'s tile and preview say about it. */
    fun stateOf(part: LookPart): ItemState {
        if (inUse(part)) return ItemState.InUse
        val cosmetic = part.cosmetic ?: return ItemState.Have
        if (cosmetic in owned.earned) return ItemState.Earned(cosmetic in fresh)
        if (cosmetic in owned.bought) return ItemState.Have
        val price = store.price(cosmetic) ?: return ItemState.EarnOnly
        return ItemState.ForSale(price)
    }
}

/** How an item stands for the player: the words on its tile and its preview's button. */
sealed interface ItemState {
    /** In use: the theme, piece style or ring effect the game draws with. */
    data object InUse : ItemState

    /** Owned, not in use: bought, or the default of its kind. */
    data object Have : ItemState

    /** Earned by playing, not in use; [isNew] until its preview is opened. */
    data class Earned(val isNew: Boolean) : ItemState

    /** For sale at [price], and earnable. */
    data class ForSale(val price: String) : ItemState

    /** Only earnable: where Google Play can't sell, or hasn't said its price yet. */
    data object EarnOnly : ItemState
}

/**
 * The Shop (flows.md, "Shop and rewarded ads"; the wireframes' Shop frames, D-28 and D-29): a full
 * screen on squared paper with three tabs, Themes, Pieces and Rings ([tab], [onTab]). Each tab has
 * a tile per item, the default first, each on the Shop's small board: a theme drawn by that theme,
 * a piece style or ring effect in the theme in use. A tile says In use, Owned or Earned (marked
 * New until the preview is opened), or shows the price and the stars that earn the item, with the
 * player's own count. Where Google Play can't sell, a line says every item is earned with stars,
 * and there are no prices and no Restore purchases. A tile opens its preview ([onItem]); Restore
 * purchases asks Google Play again ([onRestore]).
 */
@Composable
fun ShopScreen(
    view: ShopView,
    tab: ShopTab,
    onTab: (ShopTab) -> Unit,
    onItem: (LookPart) -> Unit,
    onRestore: () -> Unit,
    onBack: () -> Unit,
) {
    val style = Palette.current
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val tall = maxHeight >= 700.dp
        val earnOnly = view.store.selling == Selling.EARN_ONLY
        Column(Modifier.fillMaxSize().squaredPaper(style, margin = false)) {
            PageHeader(stringResource(R.string.screen_shop), onBack, stringResource(R.string.back))
            Tabs(tab, onTab, style)
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
            ) {
                if (earnOnly) {
                    Text(
                        stringResource(R.string.shop_earn_only),
                        style = NoteText.body.copy(fontSize = 14.sp, lineHeight = 20.sp),
                        color = style.pencil,
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(1.dp, RectangleShape, clip = false)
                            .background(style.card)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    tab.items.chunked(2).forEachIndexed { row, pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            pair.forEachIndexed { column, part ->
                                val turn = TILE_TURNS[(row * 2 + column) % TILE_TURNS.size]
                                ShopTile(part, view, turn, tall, style, Modifier.weight(1f)) { onItem(part) }
                            }
                            if (pair.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
                if (!earnOnly) {
                    Text(
                        stringResource(R.string.shop_restore),
                        style = ROW_TEXT.copy(fontSize = 14.sp, textDecoration = TextDecoration.Underline),
                        color = style.graphite,
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .heightIn(min = 48.dp)
                            .clickable(role = Role.Button, onClick = onRestore)
                            .wrapContentHeight()
                            .padding(horizontal = 8.dp),
                    )
                }
            }
        }
    }
}

/**
 * The tabs: the one shown is a tab of the page, open at the bottom; the others are words on the
 * paper. For TalkBack they are one group of tabs.
 */
@Composable
private fun Tabs(tab: ShopTab, onTab: (ShopTab) -> Unit, style: Palette) {
    val tabsLabel = stringResource(R.string.shop_kinds)
    Row(
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val line = 2.dp.toPx()
                drawRect(style.blue, topLeft = Offset(0f, size.height - line), size = Size(size.width, line))
            }
            .padding(horizontal = 16.dp)
            .selectableGroup()
            .semantics { contentDescription = tabsLabel },
    ) {
        for (each in ShopTab.entries) {
            val shown = each == tab
            val shape = RoundedCornerShape(topStart = 12.dp, topEnd = 14.dp)
            val look = if (shown) {
                Modifier
                    .background(style.paper, shape)
                    .toolOutline(style.blue, dashed = false, shape)
                    .drawWithContent {
                        drawContent()
                        // Open at the bottom, into the page below.
                        val line = 2.dp.toPx()
                        drawRect(style.paper, topLeft = Offset(line, size.height - line), size = Size(size.width - 2 * line, line))
                    }
                    .padding(horizontal = 14.dp)
            } else {
                Modifier.padding(horizontal = 12.dp)
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .height(48.dp)
                    .selectable(selected = shown, role = Role.Tab, onClick = { if (!shown) onTab(each) })
                    .then(look),
            ) {
                Text(
                    stringResource(each.title),
                    style = ROW_TEXT.copy(fontWeight = if (shown) FontWeight.ExtraBold else FontWeight.Bold),
                    color = if (shown) style.blue else style.graphite,
                )
            }
        }
    }
}

/** One item's tile: its small board, its name, and what it says about the item. */
@Composable
private fun ShopTile(part: LookPart, view: ShopView, turn: Float, tall: Boolean, style: Palette, modifier: Modifier, onClick: () -> Unit) {
    val state = view.stateOf(part)
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = modifier
            .rotate(turn)
            .shadow(2.dp, RectangleShape, clip = false)
            .background(style.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(8.dp),
    ) {
        BoxWithConstraints(Modifier.fillMaxWidth().clearAndSetSemantics {}, contentAlignment = Alignment.Center) {
            val side = if (tall) maxWidth else 72.dp
            ItemBoard(part, view, LOOK_SAMPLE, LOOK_SAMPLE_LAST, Modifier.size(side))
        }
        Text(stringResource(lookName(part)), style = TILE_NAME, color = style.pencil)
        val need = part.cosmetic?.stars ?: 0
        when (state) {
            ItemState.InUse -> Tag(stringResource(R.string.theme_in_use), style.red)
            ItemState.Have -> Tag(stringResource(R.string.shop_owned), style.graphite)
            is ItemState.Earned -> Tag(stringResource(R.string.shop_earned), style.highlighter, isNew = state.isNew)
            is ItemState.ForSale -> {
                Text(state.price, style = ROW_TEXT.copy(fontWeight = FontWeight.ExtraBold, lineHeight = 18.sp), color = style.pencil)
                TileProgress(view.stars, need, stringResource(R.string.shop_or_earn, view.stars, need), style)
            }
            ItemState.EarnOnly -> TileProgress(view.stars, need, stringResource(R.string.shop_earn, view.stars, need), style)
        }
    }
}

/** In use, Owned or Earned on a tile, with the New mark. */
@Composable
private fun Tag(text: String, ink: Color, isNew: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(text, style = NoteText.label.copy(fontSize = 12.sp, lineHeight = 18.sp, fontWeight = FontWeight.ExtraBold), color = ink)
        if (isNew) NewMark()
    }
}

/** New, in white on a red pill: an earned item whose preview is not opened yet. */
@Composable
private fun NewMark() {
    Text(
        stringResource(R.string.shop_new),
        style = NoteText.label.copy(fontSize = 10.sp, lineHeight = 16.sp, fontWeight = FontWeight.ExtraBold),
        color = Color.White,
        modifier = Modifier.background(Palette.current.red, RoundedCornerShape(8.dp)).padding(horizontal = 6.dp),
    )
}

/** A tile's stars: a bar, then "Or earn: 112 / 150 ★", read by TalkBack as the text. */
@Composable
private fun TileProgress(have: Int, need: Int, text: String, style: Palette) {
    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
        StarsBar(have, need, 6.dp, style)
        Text(text, style = NoteText.label.copy(fontSize = 11.sp, lineHeight = 14.sp, fontWeight = FontWeight.ExtraBold), color = style.highlighter)
    }
}

/** How far the player's stars ([have]) are towards [need], in the earning ink. */
@Composable
private fun StarsBar(have: Int, need: Int, height: Dp, style: Palette) {
    val part = if (need <= 0) 1f else (have.toFloat() / need).coerceIn(0f, 1f)
    Box(
        Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2))
            .background(style.highlighter.copy(alpha = 0.18f))
            .clearAndSetSemantics {},
    ) {
        if (part > 0f) Box(Modifier.fillMaxWidth(part).fillMaxHeight().background(style.highlighter))
    }
}

/**
 * The Cosmetic preview (flows.md, Shop; the wireframes' Cosmetic preview and its D-28 states): the
 * item on a sample board, its line, a chip with its price or how it stands, how to earn it, and one
 * button: Buy ([onBuy]), or Use it ([onUse]) for an item owned, or In use. Where Google Play can't
 * sell, only how to earn it. After a Buy the preview says whether the payment is pending, was
 * cancelled, or failed; a failure offers Retry.
 */
@Composable
fun CosmeticPreviewScreen(part: LookPart, view: ShopView, onBuy: () -> Unit, onUse: () -> Unit, onBack: () -> Unit) {
    val style = Palette.current
    val state = view.stateOf(part)
    val name = stringResource(lookName(part))
    val need = part.cosmetic?.stars ?: 0
    val have = view.stars
    val outcome = part.cosmetic?.let { view.store.outcomes[it] }?.takeIf { state is ItemState.ForSale || state == ItemState.EarnOnly }
    BoxWithConstraints(Modifier.fillMaxSize().background(style.paper)) {
        val side = minOf(maxWidth - 32.dp, maxHeight - 320.dp).coerceAtLeast(120.dp)
        Column(Modifier.fillMaxSize()) {
            PageHeader(name, onBack, stringResource(R.string.back_to_shop))
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
            ) {
                ItemBoard(part, view, PREVIEW_SAMPLE, PREVIEW_LAST, Modifier.size(side), stringResource(R.string.preview_sample, name))
                Text(stringResource(lookLine(part)), style = PREVIEW_TEXT, color = style.pencil, textAlign = TextAlign.Center)
                when {
                    outcome == BuyOutcome.PENDING -> Chip(stringResource(R.string.preview_pending_chip), style.graphite)
                    state == ItemState.InUse -> Chip(stringResource(R.string.theme_in_use), style.graphite)
                    state == ItemState.Have -> Chip(stringResource(R.string.shop_owned), style.graphite)
                    state is ItemState.Earned -> Chip(
                        if (have >= need) pluralStringResource(R.plurals.preview_earned_with, need, need) else stringResource(R.string.shop_earned),
                        style.highlighter,
                        isNew = state.isNew,
                    )
                    state is ItemState.ForSale -> Chip(state.price, style.graphite)
                    else -> Chip(pluralStringResource(R.plurals.preview_stars_of, need, have, need), style.highlighter)
                }
            }
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
            ) {
                if (state is ItemState.ForSale || state == ItemState.EarnOnly) {
                    val line = if (state is ItemState.ForSale) {
                        pluralStringResource(R.plurals.preview_or_earn, need, need, have)
                    } else {
                        pluralStringResource(R.plurals.preview_earn_only, need, need, have, (need - have).coerceAtLeast(0))
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 4.dp)) {
                        Text(line, style = PREVIEW_TEXT, color = style.pencil, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                        StarsBar(have, need, 8.dp, style)
                    }
                }
                val said = when (outcome) {
                    BuyOutcome.PENDING -> R.string.preview_pending
                    BuyOutcome.CANCELLED -> R.string.preview_cancelled
                    BuyOutcome.FAILED -> R.string.preview_failed
                    null -> null
                }
                if (said != null) {
                    Text(stringResource(said), style = PREVIEW_TEXT, color = style.graphite, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                }
                when {
                    state is ItemState.ForSale && outcome != BuyOutcome.PENDING -> BigButton(
                        if (outcome == BuyOutcome.FAILED) stringResource(R.string.result_retry) else stringResource(R.string.preview_buy, state.price),
                        style,
                        onBuy,
                        Modifier.fillMaxWidth(),
                    )
                    state == ItemState.Have || state is ItemState.Earned -> BigButton(stringResource(R.string.preview_use), style, onUse, Modifier.fillMaxWidth())
                    state == ItemState.InUse -> InUseButton(style)
                }
            }
        }
    }
}

/** The chip under the preview's board: the price, Owned, In use, Earned with its New mark, or the stars. */
@Composable
private fun Chip(text: String, ink: Color, isNew: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier
            .height(30.dp)
            .drawBehind {
                val line = 1.5.dp.toPx()
                drawRoundRect(
                    ink,
                    topLeft = Offset(line / 2, line / 2),
                    size = Size(size.width - line, size.height - line),
                    cornerRadius = CornerRadius(size.height / 2),
                    style = Stroke(line),
                )
            }
            .padding(horizontal = 12.dp),
    ) {
        Text(text, style = NoteText.label.copy(fontSize = 13.sp, fontWeight = FontWeight.ExtraBold), color = ink)
        if (isNew) NewMark()
    }
}

/** In use, where the main button would be: a dashed outline that does nothing. */
@Composable
private fun InUseButton(style: Palette) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .toolOutline(style.graphite, dashed = true, BIG_BUTTON_SHAPE),
    ) {
        Text(stringResource(R.string.theme_in_use), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = style.graphite)
    }
}

/**
 * [part] on a small board, [state] with its [last] move marked: a theme drawn by itself with its
 * own pieces and ring, a piece style or ring effect in the theme in use.
 */
@Composable
private fun ItemBoard(part: LookPart, view: ShopView, state: GameState, last: Pos, modifier: Modifier, description: String = "") {
    when (part) {
        is ThemeId -> LookBoard(state, last, part, modifier, description = description)
        is PieceStyleId -> LookBoard(state, last, view.theme, modifier, pieces = part, description = description)
        is RingEffectId -> LookBoard(state, last, view.theme, modifier, rings = part, description = description)
    }
}

/**
 * The Cosmetic preview's sample board (the wireframes' Cosmetic preview): 7 x 7 points, a red ring
 * of six round two blue pieces, a red piece and three blue pieces loose, and Blue's last move
 * ([PREVIEW_LAST]) marked.
 */
internal val PREVIEW_SAMPLE: GameState = GameState.fromAscii(
    """
    .......
    ....BR.
    ..RR...
    .RBBR..
    ..R..B.
    .......
    .......
    """,
).play(Pos(3, 4)).state.play(Pos(1, 5)).state

/** The last move on [PREVIEW_SAMPLE]: Blue's piece at the lower left. */
internal val PREVIEW_LAST = Pos(1, 5)

/** How far each tile is turned, in degrees, in the wireframe's order. */
private val TILE_TURNS = listOf(-1f, 1f, 0.5f)

/** A tile's name: Caveat, 24 sp. */
private val TILE_NAME: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.display.copy(fontSize = 24.sp, lineHeight = 24.sp)

/** The preview's lines: 14 sp. */
private val PREVIEW_TEXT: TextStyle
    @Composable @ReadOnlyComposable
    get() = NoteText.body.copy(fontSize = 14.sp, lineHeight = 20.sp)
