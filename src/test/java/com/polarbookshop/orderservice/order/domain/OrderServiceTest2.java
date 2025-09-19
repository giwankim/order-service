package com.polarbookshop.orderservice.order.domain;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.notFound;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.polarbookshop.orderservice.TestcontainersConfiguration;
import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.wiremock.spring.EnableWireMock;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.NONE,
    properties = "polar.catalog-service-url=http://localhost:${wiremock.server.port}")
@Testcontainers
@Import({TestChannelBinderConfiguration.class, TestcontainersConfiguration.class})
@EnableWireMock
class OrderServiceTest2 {

  @Autowired
  OrderService2 orderService;

  @Autowired
  OrderRepository2 orderRepository;

  @Autowired
  OutputDestination output;

  @Autowired
  ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll().block();
    WireMock.reset();
  }

  @Test
  void submitOrder() throws IOException {
    String isbn = "1234567890";
    Book book = new Book(isbn, "Title", "Author", 9.90);
    stubFor(
        get(urlEqualTo("/books/" + isbn))
            .willReturn(okJson(objectMapper.writeValueAsString(book))));

    orderService
        .submitOrder(isbn, 3)
        .as(StepVerifier::create)
        .assertNext(
            order -> {
              assertThat(order.id()).isNotNull();
              assertThat(order.bookIsbn()).isEqualTo(isbn);
              assertThat(order.bookName()).isEqualTo("Title - Author");
              assertThat(order.bookPrice()).isEqualTo(9.90);
              assertThat(order.quantity()).isEqualTo(3);
              assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);
            })
        .verifyComplete();

    OrderAcceptedMessage orderAcceptedMessage =
        objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class);
    assertThat(orderAcceptedMessage.orderId()).isNotNull();
  }

  @Test
  void submitOrder_bookDoesNotExist() {
    String isbn = "1234567899";
    stubFor(get(urlEqualTo("/books/" + isbn)).willReturn(notFound()));

    orderService
        .submitOrder(isbn, 99)
        .as(StepVerifier::create)
        .assertNext(
            order -> {
              assertThat(order.id()).isNotNull();
              assertThat(order.bookIsbn()).isEqualTo(isbn);
              assertThat(order.quantity()).isEqualTo(99);
              assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
            })
        .verifyComplete();

    assertThat(output.receive()).isNull();
  }

  @Test
  void consumeOrderDispatchedEvent() {
    String isbn = "1234567893";
    Book book = new Book(isbn, "Title", "Author", 9.90);
    Order2 acceptedOrder = orderRepository.save(OrderService2.buildAcceptedOrder(book, 3)).block();

    OrderDispatchedMessage dispatchedMessage = new OrderDispatchedMessage(acceptedOrder.id());
    orderService
        .consumeOrderDispatchedEvent(Flux.just(dispatchedMessage))
        .as(StepVerifier::create)
        .assertNext(
            order -> {
              assertThat(order.id()).isEqualTo(acceptedOrder.id());
              assertThat(order.status()).isEqualTo(OrderStatus.DISPATCHED);
            })
        .verifyComplete();
  }
}
