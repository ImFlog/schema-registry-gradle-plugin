package com.github.imflog.schema.registry.security

import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.MapProperty

open class SslExtension(objects: ObjectFactory) {

    val configs: MapProperty<String, Any> = objects.mapProperty(String::class.java, Any::class.java).apply {
        convention(mapOf())
    }
}
