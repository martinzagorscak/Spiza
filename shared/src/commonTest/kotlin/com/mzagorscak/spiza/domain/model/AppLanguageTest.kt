package com.mzagorscak.spiza.domain.model

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AppLanguageTest {
    @Test
    fun fromTag_returnsTheLanguageWithThatTag() {
        assertThat(AppLanguage.fromTag("hr")).isEqualTo(AppLanguage.Croatian)
        assertThat(AppLanguage.fromTag("en")).isEqualTo(AppLanguage.English)
    }

    // region Error handling

    @Test
    fun fromTag_returnsDefault_whenTagIsUnknownOrMissing() {
        assertThat(AppLanguage.fromTag("de")).isEqualTo(AppLanguage.Default)
        assertThat(AppLanguage.fromTag(null)).isEqualTo(AppLanguage.Default)
    }

    // endregion
}
