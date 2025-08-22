package com.polarbookshop.orderservice.order.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.polarbookshop.orderservice.TestcontainersConfiguration;
import com.polarbookshop.orderservice.order.domain.Order;
import com.polarbookshop.orderservice.order.domain.OrderRepository;
import com.polarbookshop.orderservice.order.domain.OrderStatus;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.binder.test.InputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.context.annotation.Import;
import org.springframework.integration.support.MessageBuilder;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@SpringBootTest
@Import({TestChannelBinderConfiguration.class, TestcontainersConfiguration.class})
@Testcontainers
class OrderFunctionsTest {

  @Autowired InputDestination input;

  @Autowired OrderRepository orderRepository;

  @BeforeEach
  void setUp() {
    orderRepository.deleteAll().block();
  }

  @Test
  void dispatchOrder() {
    Order order =
        orderRepository
            .save(Order.create("12345678890", "Book", 9.90, 3, OrderStatus.ACCEPTED))
            .block();

    var inputMessage = MessageBuilder.withPayload(new OrderDispatchedMessage(order.id())).build();
    input.send(inputMessage);

    Mono<Order> eventuallyDispatched =
        orderRepository
            .findById(order.id())
            .filter(o -> o.status().equals(OrderStatus.DISPATCHED))
            .switchIfEmpty(Mono.empty())
            .repeatWhenEmpty(repeat -> repeat.delayElements(Duration.ofMillis(50)))
            .timeout(Duration.ofSeconds(3));

    eventuallyDispatched
        .as(StepVerifier::create)
        .assertNext(
            dispatched -> {
              assertThat(dispatched.id()).isEqualTo(order.id());
              assertThat(dispatched.status()).isEqualTo(OrderStatus.DISPATCHED);
            })
        .verifyComplete();
  }

  @Test
  void dispatchOrder_orderNotFound() {
    var inputMessage = MessageBuilder.withPayload(new OrderDispatchedMessage(999L)).build();
    input.send(inputMessage);

    Mono<Long> eventuallyCount = Mono.delay(Duration.ofMillis(300)).then(orderRepository.count());

    eventuallyCount.as(StepVerifier::create).expectNext(0L).verifyComplete();
  }
}
