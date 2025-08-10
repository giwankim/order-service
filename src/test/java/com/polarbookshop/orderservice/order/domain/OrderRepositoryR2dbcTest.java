package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.TestcontainersConfiguration;
import com.polarbookshop.orderservice.config.DataConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.test.StepVerifier;

@DataR2dbcTest
@Import({DataConfig.class, TestcontainersConfiguration.class})
@Testcontainers
record OrderRepositoryR2dbcTest(OrderRepository orderRepository) {

  @Test
  void findOrderByIdWhenNotExisting() {
    orderRepository.findById(394L).as(StepVerifier::create).expectNextCount(0L).verifyComplete();
  }

  @Test
  void createRejectedOrder() {
    Order rejectedOrder = OrderService.createRejectedOrder("1234567890", 3);
    orderRepository
        .save(rejectedOrder)
        .as(StepVerifier::create)
        .expectNextMatches(order -> order.status().equals(OrderStatus.REJECTED))
        .verifyComplete();
  }
}
