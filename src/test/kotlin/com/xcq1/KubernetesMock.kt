package com.xcq1

import io.fabric8.kubernetes.api.model.apps.StatefulSetBuilder
import io.fabric8.kubernetes.api.model.apps.StatefulSetListBuilder
import io.fabric8.mockwebserver.DefaultMockServer

fun main() {
    val server = DefaultMockServer()

    val stateOfSpec = mutableMapOf<String, Int>()
    for (specReplicas in 0..1) {
        for (statusReplicas in 0..1) {
            for (statusReadyReplicas in 0..1) {
                stateOfSpec["$specReplicas$statusReplicas$statusReadyReplicas"] = specReplicas
            }
        }
    }

    server.expect().withPath("/apis/apps/v1/statefulsets").andReply(200) {
        StatefulSetListBuilder()
            .run {
                for (specReplicas in 0..1) {
                    for (statusReplicas in 0..1) {
                        for (statusReadyReplicas in 0..1) {
                            this.addNewItem()
                                .withNewMetadata().withNamespace("default").withName("server-$specReplicas$statusReplicas$statusReadyReplicas")
                                .withResourceVersion("1").endMetadata()
                                .withNewSpec().withReplicas(stateOfSpec["$specReplicas$statusReplicas$statusReadyReplicas"]).endSpec()
                                .withNewStatus().withReplicas(statusReplicas).withReadyReplicas(statusReadyReplicas).endStatus()
                                .endItem()
                        }
                    }
                }
                this.build()
            }
    }.always()

    for (specReplicas in 0..1) {
        for (statusReplicas in 0..1) {
            for (statusReadyReplicas in 0..1) {
                server.expect().withPath("/apis/apps/v1/statefulsets/server-$specReplicas$statusReplicas$statusReadyReplicas").andReply(200) {
                    StatefulSetBuilder()
                        .withNewMetadata().withNamespace("default").withName("server-$specReplicas$statusReplicas$statusReadyReplicas").withResourceVersion("1")
                        .endMetadata()
                        .withNewSpec().withReplicas(stateOfSpec["$specReplicas$statusReplicas$statusReadyReplicas"]).endSpec()
                        .withNewStatus().withReplicas(statusReplicas).withReadyReplicas(statusReadyReplicas).endStatus()
                        .build()
                }.always()

                server.expect().patch().withPath("/apis/apps/v1/namespaces/default/statefulsets/server-$specReplicas$statusReplicas$statusReadyReplicas")
                    .andReply(204) {
                        val value = if ("\"value\":1" in it.body.toString()) 1 else 0
                        stateOfSpec["$specReplicas$statusReplicas$statusReadyReplicas"] = value
                        ""
                    }.always()
            }
        }
    }


    server.start()
    println("Mock K8s running at ${server.hostName}:${server.port}")
    main("http://${server.hostName}:${server.port}")
}