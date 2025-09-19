package com.polarbookshop.orderservice.order.web

import com.ninjasquad.springmockk.MockkBean
import com.polarbookshop.orderservice.order.domain.Order
import com.polarbookshop.orderservice.order.domain.OrderService
import com.polarbookshop.orderservice.order.domain.OrderStatus
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(OrderController::class)
class OrderControllerTests(
    val webClient: WebTestClient,
) {
    @MockkBean
    private lateinit var orderService: OrderService

    @Test
    fun `when book is not available reject order`() {
        val isbn = "1234567890"
        val quantity = 3
        val rejectedOrder = Order.rejected(isbn, quantity)
        every { orderService.submitOrder(isbn, quantity) } returns Mono.just(rejectedOrder)

        webClient
            .post()
            .uri("/orders")
            .bodyValue(OrderRequest(isbn, quantity))
            .exchange()
            .expectStatus()
            .isOk
            .expectBody(Order::class.java)
            .value { order ->
                assertThat(order).isNotNull
                assertThat(order.status).isEqualTo(OrderStatus.REJECTED)
            }
    }
}
