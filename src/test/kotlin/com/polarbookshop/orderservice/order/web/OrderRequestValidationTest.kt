package com.polarbookshop.orderservice.order.web

import jakarta.validation.Validation
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderRequestValidationTest {
    val validator = Validation.buildDefaultValidatorFactory().validator

    @Test
    fun `all fields are correct`() {
        val orderRequest = OrderRequest(isbn = "1234567890", quantity = 3)
        val violations = validator.validate(orderRequest)
        assertThat(violations).isEmpty()
    }

    @Test
    fun `isbn not defined fails`() {
        val orderRequest = OrderRequest(isbn = "", quantity = 3)
        val violations = validator.validate(orderRequest)
        assertThat(violations).hasSize(1)
        val violation = violations.first()
        assertThat(violation.propertyPath.toString()).isEqualTo("isbn")
        assertThat(violation.message).isEqualTo("The book ISBN must be defined.")
    }

    @Test
    fun `quantity lower than min fails`() {
        val orderRequest = OrderRequest(isbn = "1234567890", quantity = 0)
        val violations = validator.validate(orderRequest)
        assertThat(violations).hasSize(1)
        val violation = violations.first()
        assertThat(violation.propertyPath.toString()).isEqualTo("quantity")
        assertThat(violation.message).isEqualTo("You must order at least 1 item.")
    }

    @Test
    fun `quantity greater than max fails`() {
        val orderRequest = OrderRequest(isbn = "1234567890", quantity = 7)
        val violations = validator.validate(orderRequest)
        assertThat(violations).hasSize(1)
        val violation = violations.first()
        assertThat(violation.propertyPath.toString()).isEqualTo("quantity")
        assertThat(violation.message).isEqualTo("You cannot order more than 5 items.")
    }
}
