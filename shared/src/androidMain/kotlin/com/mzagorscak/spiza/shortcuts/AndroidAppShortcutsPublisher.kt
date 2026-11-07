package com.mzagorscak.spiza.shortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Build
import android.os.PersistableBundle
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.mzagorscak.spiza.domain.model.AppLanguage
import com.mzagorscak.spiza.domain.model.LoyaltyCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.getString
import spiza.shared.generated.resources.Res
import spiza.shared.generated.resources.loyalty_card_details_card_missing
import java.util.Locale
import kotlin.math.roundToInt

private const val LOYALTY_CARD_SHORTCUT_ID_PREFIX = "loyalty_card_"
private const val EXTRA_COLOR_ARGB = "colorArgb"

// Adaptive icons are 108dp; launchers may crop everything outside the centred 72dp safe zone.
private const val ICON_SIZE_DP = 108
private const val INITIAL_TEXT_SIZE_DP = 40

private const val LUMINANCE_THRESHOLD = 0.5
private const val OPAQUE_ALPHA = 0xFF

internal class AndroidAppShortcutsPublisher(
    private val context: Context,
) : AppShortcutsPublisher {
    // Launcher shortcuts exist from API 25; below it ShortcutManagerCompat would only no-op.
    override val isSupported: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1

    override suspend fun publishLoyaltyCards(
        cards: List<LoyaltyCard>,
        existingCardIds: Set<Long>,
        language: AppLanguage,
    ) {
        if (isSupported.not()) return
        val cardMissingMessage = getLocalizedString(resource = Res.string.loyalty_card_details_card_missing, language = language)
        // ShortcutManager calls are binder IPC and the icons are drawn on a bitmap: keep both off the main thread.
        withContext(Dispatchers.IO) {
            val launcherActivity =
                checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName)?.component) {
                    "No launcher activity to target with app shortcuts"
                }
            val desired =
                cards.mapIndexed { index, card ->
                    card.toShortcut(rank = index, launcherActivity = launcherActivity)
                }
            val desiredIds = desired.mapTo(mutableSetOf()) { it.id }
            val dynamic = ShortcutManagerCompat.getDynamicShortcuts(context).filter { it.isLoyaltyCardShortcut() }
            val pinned =
                ShortcutManagerCompat
                    .getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                    .filter { it.isLoyaltyCardShortcut() }

            val removedIds = dynamic.map { it.id }.filterNot { it in desiredIds }
            if (removedIds.isNotEmpty()) {
                ShortcutManagerCompat.removeDynamicShortcuts(context, removedIds)
            }

            // A pinned shortcut cannot be removed by the app; disable it so tapping it explains why nothing opens.
            // One whose card still exists (only un-toggled) keeps working. Already disabled ones are disabled again
            // when their message is in another language, so a language change reaches them too.
            val orphanedPinnedIds =
                pinned
                    .filter { it.loyaltyCardId() !in existingCardIds }
                    .filter { it.isEnabled || it.disabledMessage?.toString() != cardMissingMessage }
                    .map { it.id }
            if (orphanedPinnedIds.isNotEmpty()) {
                ShortcutManagerCompat.disableShortcuts(context, orphanedPinnedIds, cardMissingMessage)
            }

            // Defensive: ids are not reused (autoGenerate is AUTOINCREMENT), but a disabled pinned shortcut whose card
            // exists again (e.g. a restored database) cannot be pushed until it is enabled.
            val reEnabled =
                desired.filter { shortcut -> pinned.any { it.id == shortcut.id && it.isEnabled.not() } }
            if (reEnabled.isNotEmpty()) {
                ShortcutManagerCompat.enableShortcuts(context, reEnabled)
            }

            desired
                .filterNot { shortcut -> dynamic.any { it.isSameAs(shortcut) } }
                .forEach { ShortcutManagerCompat.pushDynamicShortcut(context, it) }
        }
    }

    /**
     * Non-composable compose-resources lookups resolve against `Locale.getDefault()` and offer no public way to pass a
     * locale. `ProvideAppLanguage` keeps the default at the in-app language, but only once the UI is composed, and the
     * sync can run before that; applying the same language here first makes the lookup independent of that order.
     */
    private suspend fun getLocalizedString(
        resource: StringResource,
        language: AppLanguage,
    ): String {
        Locale.setDefault(Locale.forLanguageTag(language.tag))
        return getString(resource)
    }

    private fun LoyaltyCard.toShortcut(
        rank: Int,
        launcherActivity: ComponentName,
    ): ShortcutInfoCompat {
        val label = name.trim()
        val intent =
            Intent(Intent.ACTION_VIEW)
                .setComponent(launcherActivity)
                .putExtra(AppShortcutIntents.EXTRA_LOYALTY_CARD_ID, id)
                .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return ShortcutInfoCompat
            .Builder(context, shortcutId(id))
            .setShortLabel(label)
            .setLongLabel(label)
            .setRank(rank)
            .setIcon(createIcon(name = label, colorArgb = colorArgb))
            .setIntent(intent)
            // Lets an unchanged shortcut be recognised (icons cannot be read back) so it is not pushed again.
            .setExtras(PersistableBundle().apply { putInt(EXTRA_COLOR_ARGB, colorArgb) })
            .build()
    }

    private fun createIcon(
        name: String,
        colorArgb: Int,
    ): IconCompat {
        val density = context.resources.displayMetrics.density
        val sizePx = (ICON_SIZE_DP * density).roundToInt()
        val backgroundColor = ColorUtils.setAlphaComponent(colorArgb, OPAQUE_ALPHA)
        val bitmap = createBitmap(width = sizePx, height = sizePx)
        val canvas = Canvas(bitmap)
        canvas.drawColor(backgroundColor)

        val initial = name.initial()
        if (initial != null) {
            val paint =
                Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = if (ColorUtils.calculateLuminance(backgroundColor) > LUMINANCE_THRESHOLD) Color.BLACK else Color.WHITE
                    textSize = INITIAL_TEXT_SIZE_DP * density
                    textAlign = Paint.Align.CENTER
                    typeface = Typeface.DEFAULT_BOLD
                }
            val center = sizePx / 2f
            canvas.drawText(initial, center, center - (paint.descent() + paint.ascent()) / 2f, paint)
        }
        return IconCompat.createWithAdaptiveBitmap(bitmap)
    }
}

/** Intent contract between the shortcuts published here and the launcher activity (`MainActivity`) that handles them. */
object AppShortcutIntents {
    const val EXTRA_LOYALTY_CARD_ID = "com.mzagorscak.spiza.extra.LOYALTY_CARD_ID"
}

private fun shortcutId(cardId: Long): String = "$LOYALTY_CARD_SHORTCUT_ID_PREFIX$cardId"

private fun ShortcutInfoCompat.isLoyaltyCardShortcut(): Boolean = id.startsWith(LOYALTY_CARD_SHORTCUT_ID_PREFIX)

private fun ShortcutInfoCompat.loyaltyCardId(): Long? = id.removePrefix(LOYALTY_CARD_SHORTCUT_ID_PREFIX).toLongOrNull()

private fun ShortcutInfoCompat.isSameAs(other: ShortcutInfoCompat): Boolean =
    id == other.id &&
        isEnabled &&
        shortLabel.toString() == other.shortLabel.toString() &&
        rank == other.rank &&
        extras?.getInt(EXTRA_COLOR_ARGB) == other.extras?.getInt(EXTRA_COLOR_ARGB)

/** First character (whole code point, so emoji survive) of the name, uppercased; none for a blank name. */
private fun String.initial(): String? {
    if (isEmpty()) return null
    val codePoint = codePointAt(0)
    return String(Character.toChars(codePoint)).uppercase()
}
