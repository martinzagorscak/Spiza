package com.mzagorscak.spiza.domain.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isLessThan
import kotlin.test.Test

class ArticleItemOrderingTest {
    private fun item(position: Double) = ArticleItem(id = "$position", text = "", isChecked = false, position = position)

    private val siblings = listOf(item(1.0), item(2.0), item(4.0))

    @Test
    fun appendPosition_isAfterLast_whenListHasItems() = assertThat(ArticleItemOrdering.appendPosition(siblings)).isGreaterThan(4.0)

    @Test
    fun appendPosition_isPositive_whenListIsEmpty() = assertThat(ArticleItemOrdering.appendPosition(emptyList())).isGreaterThan(0.0)

    @Test
    fun positionAt_isBetweenNeighbours_whenIndexIsInTheMiddle() = assertThat(ArticleItemOrdering.positionAt(siblings, 2)).isEqualTo(3.0)

    @Test
    fun positionAt_isBeforeFirst_whenIndexIsZero() = assertThat(ArticleItemOrdering.positionAt(siblings, 0)).isLessThan(1.0)

    @Test
    fun positionAt_isAfterLast_whenIndexIsSize() = assertThat(ArticleItemOrdering.positionAt(siblings, 3)).isGreaterThan(4.0)

    @Test
    fun positionAt_isClampedToTheEnds_whenIndexIsOutOfRangeOrListIsEmpty() {
        assertThat(ArticleItemOrdering.positionAt(siblings, -1)).isEqualTo(ArticleItemOrdering.positionAt(siblings, 0))
        val end = ArticleItemOrdering.positionAt(siblings, siblings.size)
        assertThat(ArticleItemOrdering.positionAt(siblings, siblings.size + 1)).isEqualTo(end)
        assertThat(ArticleItemOrdering.positionAt(emptyList(), 0)).isGreaterThan(0.0)
    }
}
