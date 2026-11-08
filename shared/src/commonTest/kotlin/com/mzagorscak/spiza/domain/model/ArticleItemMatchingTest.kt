package com.mzagorscak.spiza.domain.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import com.mzagorscak.spiza.fakes.item
import kotlin.test.Test

class ArticleItemMatchingTest {
    private val items = listOf(item(id = "milk", text = "Milk", position = 1.0), item(id = "bread", text = " Bread ", position = 2.0))

    @Test
    fun findByText_findsTheItem_whenTextDiffersOnlyInCaseOrSurroundingSpaces() {
        assertThat(items.findByText(" MILK ")?.id).isEqualTo("milk")
        assertThat(items.findByText("bread")?.id).isEqualTo("bread")
    }

    @Test
    fun findByText_returnsNull_whenOnlyPartOfTheTextMatches() {
        assertThat(items.findByText("Oat milk")).isNull()
    }
}
