package com.polarbookshop.orderservice.order.domain;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final OrderRepository orderRepository;

  public Flux<Order> getAllOrders() {
    return orderRepository.findAll();
  }

  public Mono<Order> submitOrder(String isbn, int quantity) {
    return Mono.just(rejectedOrder(isbn, quantity)).flatMap(orderRepository::save);
  }

  public static Order rejectedOrder(String bookIsbn, int quantity) {
    return Order.create(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
  }
}
