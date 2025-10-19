package com.micrantha.bluebell.plugin.config

open class BluebellGraphqlConfig {
    var serviceName: String = ""
    var packagePath: String? = null
    var endpoint: String = "http://localhost:8080"
    var headers: Map<String, String> = emptyMap()
}
