package com.mzagorscak.spiza

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
