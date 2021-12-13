package com.xcq1.plugins

import io.fabric8.kubernetes.client.DefaultKubernetesClient
import io.ktor.application.*
import io.ktor.auth.*
import io.ktor.html.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.util.*
import kotlinx.css.*
import kotlinx.css.properties.*
import kotlinx.html.*

lateinit var client: DefaultKubernetesClient

fun Application.configureRouting(k8sHost: String?) {
    client = k8sHost?.let { DefaultKubernetesClient(it) } ?: DefaultKubernetesClient()

    authentication {
        basic(name = "basic") {
            realm = "k8s-dash"
            validate { credentials ->
                if (credentials.name == "admin" && credentials.password == (System.getenv("BASIC_AUTH") ?: "admin")) {
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
                        paddingLeft = 32.px
                        fontFamily = "Roboto, Tahoma, Arial, sans-serif"
                        color = Color.white
                    }
                    rule("h1.page-title") {
                        color = Color.white
                    }
                    rule("li") {
                        fontSize = 1.4.em
                        lineHeight = LineHeight("1.6")
                    }

                    // material-icons
                    rule(".material-icons") {
                        fontFamily = "Material Icons"
                        fontWeight = FontWeight.normal
                        fontStyle = FontStyle.normal
                        fontSize = 24.px
                        display = Display.inlineBlock
                        lineHeight = LineHeight("1")
                        textTransform = TextTransform.none
                        letterSpacing = LinearDimension.auto
                        wordWrap = WordWrap.normal
                        whiteSpace = WhiteSpace.nowrap
                        direction = Direction.ltr
                        color = Color.lightSlateGray
                    }
                    rule(".marginleft") {
                        marginLeft = 1.em
                    }

                    rule(".reddish") {
                        color = Color("#f77")
                    }
                    rule(".greenish") {
                        color = Color("#7f7")
                    }

                    // switch/slider

                    rule(".switch") {
                        position = Position.relative
                        display = Display.inlineBlock
                        width = 2.em
                        height = 1.em
                        marginLeft = 1.em
                    }
                    rule(".switch input") {
                        opacity = 0
                        width = 0.px
                        height = 0.px
                    }
                    rule(".slider") {
                        position = Position.absolute
                        cursor = Cursor.pointer
                        top = 0.px
                        left = 0.px
                        right = 0.px
                        bottom = 0.px
                        backgroundColor = Color("#ccc")
                        transition(duration = 0.4.s)
                        borderRadius = 1.em
                    }
                    rule(".slider:before") {
                        position = Position.absolute
                        content = "".quoted
                        height = .7.em
                        width = .7.em
                        left = 4.px
                        bottom = 4.px
                        backgroundColor = Color.white
                        transition(duration = 0.4.s)
                        borderRadius = 50.pct
                    }
                    rule("input:checked + .slider") {
                        backgroundColor = Color("#2196F3")
                    }
                    rule("input:focus + .slider") {
                        boxShadow(color = Color("#2196F3"), blurRadius = 1.px)
                    }
                    rule("input:checked + .slider:before") {
                        transform { translateX(1.em) }
                    }
                }
            }

            post("/update") {
                val stsToEdit = context.parameters["sts"] ?: ""
                val bringUp = context.parameters["start"] == "true"
                client.apps().statefulSets().withName(stsToEdit).edit {
                    it.apply {
                        spec.replicas = if (bringUp) 1 else 0
                    }
                }
            }

            get("/") {
                val statefulSets = client.apps().statefulSets().list()
                call.respondHtml {
                    head {
                        link(rel = "stylesheet", href = "/styles.css", type = "text/css")
                        link(rel = "stylesheet", href = "https://fonts.googleapis.com/icon?family=Material+Icons")
                        script(type = "text/javascript") {
                            unsafe {
                                +"""function update(sts, start) {
                               |var xhr = new XMLHttpRequest();
                               |xhr.open("POST", "/update?sts=" + sts + "&start=" + start, false);
                               |xhr.send();
                               |location.reload();
                               |}""".trimMargin()
                            }
                        }
                    }
                    body {
                        h1(classes = "page-title") {
                            +"k8s-dash"
                            a(href = "/") {
                                span(classes = "material-icons") {
                                    +"refresh"
                                }
                            }
                        }

                        ul {
                            statefulSets.items.forEach { sts ->
                                val podPresent = sts.status.replicas > 0
                                val podReady = sts.status.readyReplicas?.let { it > 0 } ?: false

                                li {
                                    +sts.metadata.name
                                    span {
                                        when {
                                            !podPresent && !podReady -> span(classes = "material-icons") { +"hide_source" }
                                            !podPresent && podReady -> span(classes = "material-icons reddish") { +"flag" }
                                            podPresent && !podReady -> span(classes = "material-icons") { +"hourglass_empty" }
                                            podPresent && podReady -> span(classes = "material-icons greenish") { +"public" }
                                        }

                                    }
                                    span(classes = "marginleft") {
                                        when {
                                            !podPresent && !podReady -> +"Stopped"
                                            !podPresent && podReady -> +"Error"
                                            podPresent && !podReady -> +"Starting"
                                            podPresent && podReady -> +"Running"
                                        }
                                    }
                                    label(classes = "switch") {
                                        input(type = InputType.checkBox) {
                                            checked = sts.spec.replicas > 0
                                            onChange = "update(\"${sts.metadata.name.escapeHTML()}\", ${if (sts.spec.replicas > 0) "false" else "true"})"
                                        }
                                        span(classes = "slider round") { }
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
