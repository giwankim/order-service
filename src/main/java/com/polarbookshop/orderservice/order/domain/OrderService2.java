package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import com.polarbookshop.orderservice.order.event.OrderAcceptedMessage;
import com.polarbookshop.orderservice.order.event.OrderDispatchedMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService2 {

  private final BookClient bookClient;
  private final OrderRepository2 orderRepository;
  private final StreamBridge streamBridge;

  public Flux<Order2> getAllOrders() {
    return orderRepository.findAll();
  }

  @Transactional
  public Mono<Order2> submitOrder(String isbn, int quantity) {
    return bookClient
        .getBookByIsbn(isbn)
        .map(book -> buildAcceptedOrder(book, quantity))
        .defaultIfEmpty(buildRejectedOrder(isbn, quantity))
        .flatMap(orderRepository::save)
        .doOnNext(this::publishOrderAcceptedEvent);
  }

  public static Order2 buildAcceptedOrder(Book book, int quantity) {
    return Order2.create(
        book.getIsbn(),
        book.getTitle() + " - " + book.getAuthor(),
        book.getPrice(),
        quantity,
        OrderStatus.ACCEPTED);
  }

  public static Order2 buildRejectedOrder(String bookIsbn, int quantity) {
    return Order2.create(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
  }

  private void publishOrderAcceptedEvent(Order2 order) {
    if (!order.status().equals(OrderStatus.ACCEPTED)) {
      return;
    }
    OrderAcceptedMessage orderAcceptedMessage = new OrderAcceptedMessage(order.id());
    log.info("Sending order accepted event with id: {}", order.id());
    boolean result = streamBridge.send("acceptOrder-out-0", orderAcceptedMessage);
    log.info("Result of sending data for order with id {}: {}", order.id(), result);
  }

  public Flux<Order2> consumeOrderDispatchedEvent(Flux<OrderDispatchedMessage> flux) {
    // can further optimize by skipping the operation if order is already DISPATCHED
    return flux.flatMap(message -> orderRepository.findById(message.getOrderId()))
        .map(OrderService2::buildDispatchedOrder)
        .flatMap(orderRepository::save);
  }

  private static Order2 buildDispatchedOrder(Order2 order) {
    return Order2.builder()
        .id(order.id())
        .bookIsbn(order.bookIsbn())
        .bookName(order.bookName())
        .bookPrice(order.bookPrice())
        .quantity(order.quantity())
        .status(OrderStatus.DISPATCHED)
        .createdDate(order.createdDate())
        .lastModifiedDate(order.lastModifiedDate())
        .version(order.version())
        .build();
  }
}
