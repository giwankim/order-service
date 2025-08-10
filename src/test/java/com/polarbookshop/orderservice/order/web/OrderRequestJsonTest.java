package com.polarbookshop.orderservice.order.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.json.JacksonTester;

@JsonTest
record OrderRequestJsonTest(JacksonTester<OrderRequest> json) {

  @Test
  void deserialize() throws IOException {
    String content =
        """
        {
          "isbn": "1234567890",
          "quantity": 1
        }
        """;
    assertThat(json.parse(content))
        .usingRecursiveComparison()
        .isEqualTo(new OrderRequest("1234567890", 1));
  }
}
