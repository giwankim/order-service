package com.polarbookshop.orderservice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.web.OrderRequest;
import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, TestChannelBinderConfiguration.class})
@Testcontainers
class OrderServiceApplicationTests {

  @Autowired
  WebTestClient webClient;

  @MockitoBean
  BookClient bookClient;

  @Autowired
  OutputDestination output;

  @Autowired
  ObjectMapper objectMapper;

  @Test
  void whenGetOrdersThenReturn() throws IOException {
    String isbn = "1234567893";
    Book book = new Book(isbn, "Title", "Author", 9.90);
    when(bookClient.getBookByIsbn(isbn)).thenReturn(Mono.just(book));

    OrderRequest orderRequest = new OrderRequest(isbn, 1);
    Order order =
        webClient
            .post()
            .uri("/orders")
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();
    assertThat(order).isNotNull();
    assertThat(objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class))
        .isEqualTo(new OrderAcceptedMessage(order.id()));
  }

  @Test
  void whenPostRequestAndBookExistsThenOrderAccepted() throws IOException {
    String isbn = "1234567899";
    Book book = new Book(isbn, "Title", "Author", 9.90);
    when(bookClient.getBookByIsbn(isbn)).thenReturn(Mono.just(book));

    OrderRequest orderRequest = new OrderRequest(isbn, 3);
    Order order =
        webClient
            .post()
            .uri("/orders")
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();

    assertThat(order).isNotNull();
    assertThat(order.bookIsbn()).isEqualTo(isbn);
    assertThat(order.quantity()).isEqualTo(orderRequest.quantity());
    assertThat(order.bookName()).isEqualTo(book.title() + " - " + book.author());
    assertThat(order.bookPrice()).isEqualTo(book.price());
    assertThat(order.status()).isEqualTo(OrderStatus.ACCEPTED);

    OrderAcceptedMessage orderAcceptedMessage =
        objectMapper.readValue(output.receive().getPayload(), OrderAcceptedMessage.class);
    assertThat(orderAcceptedMessage).isEqualTo(new OrderAcceptedMessage(order.id()));
  }

  @Test
  void whenPostRequestAndBookNotExistsThenOrderRejected() {
    String isbn = "1234567894";
    when(bookClient.getBookByIsbn(isbn)).thenReturn(Mono.empty());

    OrderRequest orderRequest = new OrderRequest(isbn, 3);
    Order order =
        webClient
            .post()
            .uri("/orders")
            .bodyValue(orderRequest)
            .exchange()
            .expectStatus()
            .is2xxSuccessful()
            .expectBody(Order.class)
            .returnResult()
            .getResponseBody();

    assertThat(order).isNotNull();
    assertThat(order.bookIsbn()).isEqualTo(isbn);
    assertThat(order.quantity()).isEqualTo(orderRequest.quantity());
    assertThat(order.status()).isEqualTo(OrderStatus.REJECTED);
  }
}
