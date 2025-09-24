package com.polarbookshop.orderservice.order.event

import com.polarbookshop.orderservice.order.domain.Order
import com.polarbookshop.orderservice.order.domain.OrderService
import com.polarbookshop.orderservice.order.domain.OrderStatus
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import reactor.core.publisher.Flux
import java.time.Instant
import java.util.concurrent.atomic.AtomicBoolean

@ExtendWith(MockKExtension::class)
class OrderFunctionsTest {
    @MockK
    lateinit var orderService: OrderService

    @Test
    fun `dispatch order`() {
        val orderId = 121L
        val messages = Flux.just(OrderDispatchedMessage(orderId))
        val subscribed = AtomicBoolean()
        val orders =
            Flux
                .just(
                    Order(
                        bookIsbn = "1234567890",
                        bookName = "The Hobbit",
                        bookPrice = 9.90,
                        quantity = 1,
                        status = OrderStatus.DISPATCHED,
                        id = orderId,
                        createdDate = Instant.now(),
                        lastModifiedDate = Instant.now(),
                        version = 21,
                    ),
                ).doOnSubscribe { subscribed.set(true) }
        every { orderService.consumeOrderDispatchedEvent(any()) } returns orders

        OrderFunctions().dispatchOrder(orderService).accept(messages)

        verify(exactly = 1) { orderService.consumeOrderDispatchedEvent(any()) }
        assertThat(subscribed).isTrue
    }
}
