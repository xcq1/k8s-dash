package com.xcq1.plugins

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.fabric8.kubernetes.client.KubernetesClient
import io.ktor.routing.*
import io.ktor.http.*
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.html.*
import io.ktor.response.*
import io.ktor.request.*
import kotlinx.css.*
import kotlinx.html.*

fun Application.configureRouting() {

    authentication {
        basic(name = "basic") {
            realm = "k8s-dash"
            validate { credentials ->
                if (credentials.name == "admin" && credentials.password == System.getenv("BASIC_AUTH")) {
                    UserIdPrincipal(credentials.name)
                } else {
                    null
                }
            }
        }
    }

    routing {
        authenticate("basic") {
            get("/styles.css") {
                call.respondCss {
                    body {
                        backgroundColor = Color.darkBlue
                        margin(0.px)
                    }
                    rule("h1.page-title") {
                        color = Color.white
                    }
                }
            }

            get("/") {
                val client = DefaultKubernetesClient()
                val statefulSets = client.apps().statefulSets().list()
                call.respondHtml {
                    head {
                        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                    }
                    body {
                        h1(classes = "page-title") {
                            ul {
                                li {
                                    statefulSets.items.forEach {
                                        +(it.metadata.name + ": " + it.spec.replicas)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

suspend inline fun ApplicationCall.respondCss(builder: CSSBuilder.() -> Unit) {
    this.respondText(CSSBuilder().apply(builder).toString(), ContentType.Text.CSS)
}
