package com.polarbookshop.orderservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.ninjasquad.springmockk.MockkBean
import com.polarbookshop.orderservice.book.Book
import com.polarbookshop.orderservice.book.BookClient
import com.polarbookshop.orderservice.order.domain.Order
import com.polarbookshop.orderservice.order.domain.OrderStatus
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage
import com.polarbookshop.orderservice.order.web.OrderRequest
import io.mockk.every
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(TestcontainersConfiguration::class, TestChannelBinderConfiguration::class)
@Testcontainers
class OrderServiceApplicationTests(
    val webClient: WebTestClient,
    val output: OutputDestination,
    val objectMapper: ObjectMapper,
) {
    @MockkBean
    lateinit var bookClient: BookClient

    @Test
    fun `get orders returns all orders`() {
        val isbn = "1234567893"
        val book = Book(isbn = isbn, title = "Title", author = "Author", price = 9.90)
        every { bookClient.getBookByIsbn(isbn) } returns book.toMono()

        val order =
            webClient
                .post()
                .uri("/orders")
                .bodyValue(OrderRequest(isbn, 1))
                .exchange()
                .expectStatus()
                .is2xxSuccessful
                .expectBody<Order>()
                .returnResult()
                .responseBody!!

        // confirm order accepted message is sent
        assertThat(objectMapper.readValue<OrderAcceptedMessage>(output.receive().payload))
            .isEqualTo(OrderAcceptedMessage(order.id))

        // submitted order should be returned
        val orders =
            webClient
                .get()
                .uri("/orders")
                .exchange()
                .expectStatus()
                .isOk
                .expectBodyList<Order>()
                .returnResult()
                .responseBody!!
        assertThat(orders).extracting<String>(Order::bookIsbn).contains(isbn)
    }

    @Test
    fun `submit order and book exists then order is accepted`() {
        val isbn = "1234567899"
        val book = Book(isbn = isbn, title = "Title", author = "Author", price = 9.90)
        every { bookClient.getBookByIsbn(isbn) } returns book.toMono()

        val orderRequest = OrderRequest(isbn, 3)
        val order =
            webClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .is2xxSuccessful
                .expectBody<Order>()
                .returnResult()
                .responseBody!!

        // submitted order has correct details
        assertThat(order.bookIsbn).isEqualTo(isbn)
        assertThat(order.quantity).isEqualTo(orderRequest.quantity)
        assertThat(order.bookName).isEqualTo(book.title + " - " + book.author)
        assertThat(order.bookPrice).isEqualTo(book.price)
        assertThat(order.status).isEqualTo(OrderStatus.ACCEPTED)

        // confirm order accepted message is sent
        assertThat(objectMapper.readValue<OrderAcceptedMessage>(output.receive().payload))
            .isEqualTo(OrderAcceptedMessage(order.id))
    }

    @Test
    fun `submit order and book does not exist then order is rejected`() {
        val isbn = "1234567894"
        every { bookClient.getBookByIsbn(isbn) } returns Mono.empty()

        val orderRequest = OrderRequest(isbn, 3)
        val order =
            webClient
                .post()
                .uri("/orders")
                .bodyValue(orderRequest)
                .exchange()
                .expectStatus()
                .is2xxSuccessful
                .expectBody<Order>()
                .returnResult()
                .responseBody!!

        assertThat(order.bookIsbn).isEqualTo(isbn)
        assertThat(order.quantity).isEqualTo(orderRequest.quantity)
        assertThat(order.status).isEqualTo(OrderStatus.REJECTED)
    }
}
