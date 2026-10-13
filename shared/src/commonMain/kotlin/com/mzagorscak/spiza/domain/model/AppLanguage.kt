package com.mzagorscak.spiza.domain.model

enum class AppLanguage(
    val tag: String,
) {
    English("en"),
    Croatian("hr"),
    ;

    companion object {
        val Default = English

        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: Default
    }
}
