package com.mzagorscak.spiza

enum class Platform {
    Android,
    Ios,
}

expect fun getPlatform(): Platform
