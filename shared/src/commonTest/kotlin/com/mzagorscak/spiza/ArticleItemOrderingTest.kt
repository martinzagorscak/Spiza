package com.mzagorscak.spiza

import com.mzagorscak.spiza.domain.model.ArticleItem
import com.mzagorscak.spiza.domain.model.ArticleItemOrdering
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ArticleItemOrderingTest {
    private fun item(position: Double) = ArticleItem(id = "$position", text = "", isChecked = false, position = position)

    private val siblings = listOf(item(1.0), item(2.0), item(4.0))

    @Test
    fun appendGoesAfterLast() = assertTrue(ArticleItemOrdering.appendPosition(siblings) > 4.0)

    @Test
    fun appendToEmptyList() = assertTrue(ArticleItemOrdering.appendPosition(emptyList()) > 0.0)

    @Test
    fun middleIsBetweenNeighbours() = assertEquals(3.0, ArticleItemOrdering.positionAt(siblings, 2))

    @Test
    fun firstIsBeforeFirst() = assertTrue(ArticleItemOrdering.positionAt(siblings, 0) < 1.0)

    @Test
    fun lastIsAfterLast() = assertTrue(ArticleItemOrdering.positionAt(siblings, 3) > 4.0)
}
