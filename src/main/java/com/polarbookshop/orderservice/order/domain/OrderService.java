package com.polarbookshop.orderservice.order.domain;

import com.polarbookshop.orderservice.book.Book;
import com.polarbookshop.orderservice.book.BookClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class OrderService {

  private final BookClient bookClient;
  private final OrderRepository orderRepository;

  public Flux<Order> getAllOrders() {
    return orderRepository.findAll();
  }

  public Mono<Order> submitOrder(String isbn, int quantity) {
    return bookClient
        .getBookByIsbn(isbn)
        .map(book -> createAcceptedOrder(book, quantity))
        .defaultIfEmpty(createRejectedOrder(isbn, quantity))
        .flatMap(orderRepository::save);
  }

  public static Order createAcceptedOrder(Book book, int quantity) {
    return Order.create(
        book.isbn(),
        book.title() + " - " + book.author(),
        book.price(),
        quantity,
        OrderStatus.ACCEPTED);
  }

  public static Order createRejectedOrder(String bookIsbn, int quantity) {
    return Order.create(bookIsbn, null, null, quantity, OrderStatus.REJECTED);
  }
}
