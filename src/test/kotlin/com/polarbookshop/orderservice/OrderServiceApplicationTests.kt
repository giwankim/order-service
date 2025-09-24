package com.polarbookshop.orderservice

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.tomakehurst.wiremock.client.WireMock
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.notFound
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.stubFor
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.polarbookshop.orderservice.book.Book
import com.polarbookshop.orderservice.order.domain.Order
import com.polarbookshop.orderservice.order.domain.OrderRepository
import com.polarbookshop.orderservice.order.domain.OrderStatus
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage
import com.polarbookshop.orderservice.order.web.OrderRequest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.cloud.stream.binder.test.OutputDestination
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.test.web.reactive.server.expectBodyList
import org.testcontainers.junit.jupiter.Testcontainers
import org.wiremock.spring.EnableWireMock

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = ["polar.catalog-service-uri=http://localhost:\${wiremock.server.port}"],
)
@Import(TestcontainersConfiguration::class, TestChannelBinderConfiguration::class)
@Testcontainers
@EnableWireMock
class OrderServiceApplicationTests(
    val webClient: WebTestClient,
    val output: OutputDestination,
    val objectMapper: ObjectMapper,
    val orderRepository: OrderRepository,
) {

    @BeforeEach
    fun setUp() {
        orderRepository.deleteAll().block()
        WireMock.reset()
        output.clear()
    }

    @Test
    fun `get orders returns all orders`() {
        val isbn = "1234567893"
        val book = Book(isbn = isbn, title = "Title", author = "Author", price = 9.90)
        stubFor(
            get(urlEqualTo("/books/$isbn"))
                .willReturn(okJson(objectMapper.writeValueAsString(book))),
        )
        // submit order
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

        // submitted order should be one of the results
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
        stubFor(
            get(urlEqualTo("/books/$isbn"))
                .willReturn(okJson(objectMapper.writeValueAsString(book))),
        )

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
        stubFor(get(urlEqualTo("/books/$isbn")).willReturn(notFound()))

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
