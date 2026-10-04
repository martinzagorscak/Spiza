package com.example.svizac

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
