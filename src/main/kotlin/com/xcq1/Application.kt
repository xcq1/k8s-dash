package com.xcq1

import com.xcq1.plugins.configureRouting
import io.ktor.server.engine.*
import io.ktor.server.netty.*

fun main(k8sHost: String?) {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting(k8sHost)
    }.start(wait = true)
}
