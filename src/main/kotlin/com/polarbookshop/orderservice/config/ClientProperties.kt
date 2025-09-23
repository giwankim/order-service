package com.polarbookshop.orderservice.config

import jakarta.validation.constraints.NotNull
import org.hibernate.validator.constraints.URL
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.validation.annotation.Validated
import java.net.URI

@Validated
@ConfigurationProperties(prefix = "polar")
data class ClientProperties(
    @param:URL @param:NotNull val catalogServiceUri: URI,
)
