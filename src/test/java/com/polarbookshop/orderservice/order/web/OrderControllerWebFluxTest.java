package com.polarbookshop.orderservice.order.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderService;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

@WebFluxTest(OrderController.class)
@RequiredArgsConstructor
class OrderControllerWebFluxTest {

  @Autowired WebTestClient webClient;

  @MockitoBean OrderService orderService;

  @Test
  void whenBookNotAvailableRejectOrder() {
    String isbn = "1234567890";
    int quantity = 3;

    Order expectedOrder = OrderService.createRejectedOrder(isbn, quantity);
    when(orderService.submitOrder(isbn, quantity)).thenReturn(Mono.just(expectedOrder));

    webClient
        .post()
        .uri("/orders")
        .bodyValue(new OrderRequest(isbn, quantity))
        .exchange()
        .expectStatus()
        .isOk()
        .expectBody(Order.class)
        .value(
            actualOrder -> {
              assertThat(actualOrder).isNotNull();
              assertThat(actualOrder.status()).isEqualTo(OrderStatus.REJECTED);
            });
  }
}
