package com.polarbookshop.orderservice.order.domain

import com.polarbookshop.orderservice.TestcontainersConfiguration
import com.polarbookshop.orderservice.config.DataConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest
import org.springframework.context.annotation.Import
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier

@DataR2dbcTest
@Import(DataConfig::class, TestcontainersConfiguration::class)
@Testcontainers
class OrderRepositoryTest(
    val orderRepository: OrderRepository,
) {
    @Test
    fun `find order by id when it does not exist`() {
        StepVerifier
            .create(orderRepository.findById(394L))
            .expectNextCount(0L)
            .verifyComplete()
    }

    @Test
    fun `create rejected order`() {
        val rejectedOrder = Order.rejected("1234567890", 3)
        StepVerifier
            .create(orderRepository.save(rejectedOrder))
            .expectNextMatches { it.status == OrderStatus.REJECTED }
            .verifyComplete()
    }
}
