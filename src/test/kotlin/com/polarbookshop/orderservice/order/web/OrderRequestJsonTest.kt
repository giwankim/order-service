package com.polarbookshop.orderservice.order.web

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester

@JsonTest
class OrderRequestJsonTest(
    val json: JacksonTester<OrderRequest>,
) {
    @Test
    fun deserialize() {
        val content =
            """
            {
                "isbn": "1234567890",
                "quantity": 1
            }
            """.trimIndent()
        assertThat(json.parse(content)).isEqualTo(OrderRequest("1234567890", 1))
    }
}
