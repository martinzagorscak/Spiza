package com.mzagorscak.spiza.domain.model

/** Texts that differ only in case or surrounding spaces ("Milk", " milk ") name the same item, so the list never holds both. */
fun List<ArticleItem>.findByText(text: String): ArticleItem? {
    val normalizedText = text.trim().lowercase()
    return firstOrNull { it.text.trim().lowercase() == normalizedText }
}
