package com.mzagorscak.spiza.ui.model

import com.mzagorscak.spiza.domain.model.ArticleItem

data class PresentableArticleItem(
    val id: String,
    val text: String,
    val isChecked: Boolean,
)

fun ArticleItem.toPresentable() = PresentableArticleItem(id = id, text = text, isChecked = isChecked)
