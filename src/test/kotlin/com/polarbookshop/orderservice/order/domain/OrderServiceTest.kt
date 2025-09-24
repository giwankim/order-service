package com.polarbookshop.orderservice.order.domain

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import com.polarbookshop.orderservice.TestcontainersConfiguration
import com.polarbookshop.orderservice.book.Book
import com.polarbookshop.orderservice.book.BookClient
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono
import reactor.kotlin.test.test
import java.util.concurrent.atomic.AtomicReference

@SpringBootTest
@Import(TestChannelBinderConfiguration::class, TestcontainersConfiguration::class)
class OrderServiceTest(
    val orderService: OrderService,
    val output: OutputDestination,
    val objectMapper: ObjectMapper,
    val orderRepository: OrderRepository,
) {
    @MockkBean
    lateinit var bookClient: BookClient

    @AfterEach
    fun tearDown() {
        output.clear()
        orderRepository.deleteAll().block()
    }

    @Test
    fun `submit order`() {
        val isbn = "1234567890"
        val quantity = 3
        val book = Book(isbn = isbn, title = "Book Title", author = "Author", price = 9.90)
        every { bookClient.getBookByIsbn(isbn) } returns book.toMono()

        val savedOrder = AtomicReference<Order>()

        orderService
            .submitOrder(isbn, quantity)
            .test()
            .assertNext { order ->
                savedOrder.set(order)
                assertThat(order.status).isEqualTo(OrderStatus.ACCEPTED)
                assertThat(order.bookIsbn).isEqualTo(isbn)
                assertThat(order.quantity).isEqualTo(quantity)
                assertThat(order.bookName).isEqualTo("${book.title} - ${book.author}")
            }.verifyComplete()

        // order is saved to the database
        orderRepository
            .findById(savedOrder.get().id)
            .test()
            .assertNext { order -> assertThat(order.status).isEqualTo(OrderStatus.ACCEPTED) }
            .verifyComplete()

        // order accepted message is published
        val message =
            objectMapper.readValue<OrderAcceptedMessage>(output.receive().payload)
        assertThat(message).isEqualTo(OrderAcceptedMessage(savedOrder.get().id))
    }

    @Test
    fun `submit order but book does not exist then order is rejected`() {
        val isbn = "0000000000"
        val quantity = 2
        every { bookClient.getBookByIsbn(isbn) } returns Mono.empty()

        orderService
            .submitOrder(isbn, quantity)
            .test()
            .assertNext { order ->
                assertThat(order.status).isEqualTo(OrderStatus.REJECTED)
                assertThat(order.bookIsbn).isEqualTo(isbn)
            }.verifyComplete()

        // no message should be published
        assertThat(output.receive()).isNull()
    }

    @Test
    fun `consuming order dispatched event changes order status to dispatched`() {
        val book =
            Book(
                isbn = "1234567890",
                title = "Book Title",
                author = "Author",
                price = 9.90,
            )
        // given accepted order
        val order = orderRepository.save(Order.accepted(book, 3)).block()!!

        val messages = Flux.just(OrderDispatchedMessage(order.id))
        orderService
            .consumeOrderDispatchedEvent(messages)
            .test()
            .assertNext { dispatchedOrder ->
                assertThat(dispatchedOrder.status).isEqualTo(OrderStatus.DISPATCHED)
                assertThat(dispatchedOrder.id).isEqualTo(order.id)
            }.verifyComplete()
    }
}
