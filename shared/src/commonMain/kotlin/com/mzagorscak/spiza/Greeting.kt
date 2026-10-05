package com.mzagorscak.spiza

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = sayHello(platform.name)
}
