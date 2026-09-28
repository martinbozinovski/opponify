package com.opponify

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OpponifyApplication

fun main(args: Array<String>) {
    runApplication<OpponifyApplication>(*args)
}
