package com.polarbookshop.orderservice.order.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.test.json.JsonContent;

@JsonTest
record OrderJsonTest(JacksonTester<Order> json) {

  @Test
  void serialize() throws IOException {
    Order order =
        Order.builder()
            .id(394L)
            .bookIsbn("1234567890")
            .bookName("Book Name")
            .bookPrice(9.90)
            .quantity(1)
            .status(OrderStatus.ACCEPTED)
            .createdDate(Instant.now())
            .lastModifiedDate(Instant.now())
            .version(21)
            .build();

    JsonContent<Order> jsonContent = json.write(order);

    assertThat(jsonContent).extractingJsonPathNumberValue("@.id").isEqualTo(order.id().intValue());
    assertThat(jsonContent).extractingJsonPathStringValue("@.bookIsbn").isEqualTo(order.bookIsbn());
    assertThat(jsonContent).extractingJsonPathStringValue("@.bookName").isEqualTo(order.bookName());
    assertThat(jsonContent)
        .extractingJsonPathNumberValue("@.bookPrice")
        .isEqualTo(order.bookPrice());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.quantity").isEqualTo(order.quantity());
    assertThat(jsonContent)
        .extractingJsonPathStringValue("@.status")
        .isEqualTo(order.status().toString());
    assertThat(jsonContent)
        .extractingJsonPathStringValue("@.createdDate")
        .isEqualTo(order.createdDate().toString());
    assertThat(jsonContent)
        .extractingJsonPathStringValue("@.lastModifiedDate")
        .isEqualTo(order.lastModifiedDate().toString());
    assertThat(jsonContent).extractingJsonPathNumberValue("@.version").isEqualTo(order.version());
  }
}
