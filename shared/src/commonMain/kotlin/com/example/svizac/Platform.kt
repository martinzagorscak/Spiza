package com.example.svizac

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform