package com.mzagorscak.spiza.domain.model

private const val POSITION_STEP = 1024.0

/** Fractional ordering: moving an item rewrites only its own position, never its neighbours'. */
object ArticleItemOrdering {
    /** A position before every item in [items]; positions may become negative, which Firestore and the ordering allow. */
    fun prependPosition(items: List<ArticleItem>): Double = items.minOfOrNull { it.position }?.minus(POSITION_STEP) ?: POSITION_STEP

    /** [siblings] is the ordered list the item is dropped into, without the moved item itself. */
    fun positionAt(
        siblings: List<ArticleItem>,
        index: Int,
    ): Double {
        val clamped = index.coerceIn(0, siblings.size)
        val before = siblings.getOrNull(clamped - 1)?.position
        val after = siblings.getOrNull(clamped)?.position
        return when {
            before != null && after != null -> (before + after) / 2
            before != null -> before + POSITION_STEP
            after != null -> after - POSITION_STEP
            else -> POSITION_STEP
        }
    }
}
