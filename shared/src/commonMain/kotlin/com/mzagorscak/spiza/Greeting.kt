package com.mzagorscak.spiza

import co.touchlab.kermit.Logger

class Greeting {
    private val logger = Logger.withTag("Greeting")
    private val platform = getPlatform()

    fun greet(): String {
        val greetingText = sayHello(platform.name)
        logger.d { "Greeting requested: $greetingText" }
        return greetingText
    }
}
