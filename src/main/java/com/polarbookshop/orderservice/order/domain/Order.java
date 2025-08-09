package com.polarbookshop.orderservice.order.domain;

import java.time.Instant;
import lombok.Builder;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Table;

@Table(name = "ORDERS")
@Builder
public record Order(
    @Id Long id,
    String bookIsbn,
    String bookName,
    Double bookPrice,
    Integer quantity,
    OrderStatus status,
    @CreatedDate Instant createdDate,
    @LastModifiedDate Instant lastModifiedDate,
    @Version int version) {
  public static Order create(
      String bookIsbn, String bookName, Double bookPrice, Integer quantity, OrderStatus status) {
    return Order.builder()
        .id(null)
        .bookIsbn(bookIsbn)
        .bookName(bookName)
        .bookPrice(bookPrice)
        .quantity(quantity)
        .status(status)
        .createdDate(null)
        .lastModifiedDate(null)
        .version(0)
        .build();
  }
}
