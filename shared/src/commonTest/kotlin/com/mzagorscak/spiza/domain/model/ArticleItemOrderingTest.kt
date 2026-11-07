package com.mzagorscak.spiza.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleItemOrderingTest {
    private fun item(position: Double) = ArticleItem(id = "$position", text = "", isChecked = false, position = position)

    private val siblings = listOf(item(1.0), item(2.0), item(4.0))

    @Test
    fun appendPosition_isAfterLast_whenListHasItems() = assertTrue(ArticleItemOrdering.appendPosition(siblings) > 4.0)

    @Test
    fun appendPosition_isPositive_whenListIsEmpty() = assertTrue(ArticleItemOrdering.appendPosition(emptyList()) > 0.0)

    @Test
    fun positionAt_isBetweenNeighbours_whenIndexIsInTheMiddle() = assertEquals(3.0, ArticleItemOrdering.positionAt(siblings, 2))

    @Test
    fun positionAt_isBeforeFirst_whenIndexIsZero() = assertTrue(ArticleItemOrdering.positionAt(siblings, 0) < 1.0)

    @Test
    fun positionAt_isAfterLast_whenIndexIsSize() = assertTrue(ArticleItemOrdering.positionAt(siblings, 3) > 4.0)

    @Test
    fun positionAt_isClampedToTheEnds_whenIndexIsOutOfRangeOrListIsEmpty() {
        assertEquals(ArticleItemOrdering.positionAt(siblings, 0), ArticleItemOrdering.positionAt(siblings, -1))
        assertEquals(ArticleItemOrdering.positionAt(siblings, siblings.size), ArticleItemOrdering.positionAt(siblings, siblings.size + 1))
        assertTrue(ArticleItemOrdering.positionAt(emptyList(), 0) > 0.0)
    }
}
