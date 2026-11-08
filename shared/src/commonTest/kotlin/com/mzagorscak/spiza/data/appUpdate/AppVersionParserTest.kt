package com.mzagorscak.spiza.data.appUpdate

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class AppVersionParserTest {
    @Test
    fun parseAppVersion_encodesMajorMinorPatch_whenAllComponentsPresent() {
        assertThat(parseAppVersion("1.2.3")).isEqualTo(10_203L)
    }

    @Test
    fun parseAppVersion_treatsMissingComponentsAsZero_whenMinorOrPatchMissing() {
        assertThat(parseAppVersion("2")).isEqualTo(20_000L)
        assertThat(parseAppVersion("1.0")).isEqualTo(10_000L)
    }

    @Test
    fun parseAppVersion_ignoresSuffix_whenBuildSuffixPresent() {
        assertThat(parseAppVersion("1.2.3-qa")).isEqualTo(10_203L)
        assertThat(parseAppVersion("1.2.3 (5)")).isEqualTo(10_203L)
        assertThat(parseAppVersion("1.2-beta")).isEqualTo(10_200L)
    }

    @Test
    fun parseAppVersion_ignoresSurroundingWhitespace() {
        assertThat(parseAppVersion(" 1.2.3 ")).isEqualTo(10_203L)
    }

    // region Error handling

    @Test
    fun parseAppVersion_returnsNull_whenNotAVersion() {
        assertThat(parseAppVersion("")).isNull()
        assertThat(parseAppVersion("abc")).isNull()
        assertThat(parseAppVersion("v1.2.3")).isNull()
    }

    @Test
    fun parseAppVersion_returnsNull_whenMinorOrPatchAbove99() {
        assertThat(parseAppVersion("1.100.0")).isNull()
        assertThat(parseAppVersion("1.0.100")).isNull()
    }

    @Test
    fun parseAppVersion_returnsNull_whenMoreThanThreeComponentsOrTrailingDot() {
        assertThat(parseAppVersion("1.2.3.4")).isNull()
        assertThat(parseAppVersion("1.2.")).isNull()
    }

    @Test
    fun parseAppVersion_returnsNull_whenMajorOverflows() {
        assertThat(parseAppVersion("99999999999.0.0")).isNull()
    }

    // endregion
}
