package com.polarbookshop.orderservice.order.web

import com.polarbookshop.orderservice.order.domain.Order
import com.polarbookshop.orderservice.order.domain.OrderStatus
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.json.JsonTest
import org.springframework.boot.test.json.JacksonTester
import java.time.Instant

@JsonTest
class OrderJsonTest(
    val json: JacksonTester<Order>,
) {
    @Test
    fun serialize() {
        val order =
            Order(
                id = 394L,
                bookIsbn = "1234567890",
                bookName = "Book Name",
                bookPrice = 9.90,
                quantity = 1,
                status = OrderStatus.ACCEPTED,
                createdDate = Instant.now(),
                lastModifiedDate = Instant.now(),
                version = 21,
            )
        val jsonContent = json.write(order)
        assertThat(jsonContent).extractingJsonPathNumberValue("@.id").isEqualTo(order.id.toInt())
        assertThat(jsonContent)
            .extractingJsonPathStringValue("@.bookIsbn")
            .isEqualTo(order.bookIsbn)
        assertThat(jsonContent)
            .extractingJsonPathStringValue("@.bookName")
            .isEqualTo(order.bookName)
        assertThat(jsonContent)
            .extractingJsonPathNumberValue("@.bookPrice")
            .isEqualTo(order.bookPrice)
        assertThat(jsonContent)
            .extractingJsonPathNumberValue("@.quantity")
            .isEqualTo(order.quantity)
        assertThat(jsonContent)
            .extractingJsonPathStringValue("@.status")
            .isEqualTo(order.status.toString())
        assertThat(jsonContent)
            .extractingJsonPathStringValue("@.createdDate")
            .isEqualTo(order.createdDate.toString())
        assertThat(jsonContent)
            .extractingJsonPathStringValue("@.lastModifiedDate")
            .isEqualTo(order.lastModifiedDate.toString())
        assertThat(jsonContent)
            .extractingJsonPathNumberValue("@.version")
            .isEqualTo(order.version)
    }
}
