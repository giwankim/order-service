package com.polarbookshop.orderservice.book;

import java.io.IOException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@TestMethodOrder(MethodOrderer.Random.class)
class BookClientTest {

  MockWebServer mockWebServer;
  BookClient bookClient;

  @BeforeEach
  void setUp() throws IOException {
    mockWebServer = new MockWebServer();
    mockWebServer.start();

    WebClient webClient =
        WebClient.builder().baseUrl(mockWebServer.url("/").uri().toString()).build();
    bookClient = new BookClient(webClient);
  }

  @AfterEach
  void tearDown() throws IOException {
    if (mockWebServer != null) {
      mockWebServer.close();
    }
  }

  @Test
  void whenBookExistsThenReturnBook() {
    String isbn = "1234567890";

    MockResponse mockResponse =
        new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(
                """
                {
                  "isbn": "%s",
                  "title": "Title",
                  "author": "Author",
                  "price": 9.90,
                  "publisher": "Polarsophia"
                }
            """
                    .formatted(isbn));
    mockWebServer.enqueue(mockResponse);

    Mono<Book> book = bookClient.getBookByIsbn(isbn);

    StepVerifier.create(book).expectNextMatches(b -> b.isbn().equals(isbn)).verifyComplete();
  }

  @Test
  void whenBookNotExistsThenReturnEmpty() {
    String isbn = "1234567891";

    MockResponse mockResponse =
        new MockResponse()
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setResponseCode(404);
    mockWebServer.enqueue(mockResponse);

    StepVerifier.create(bookClient.getBookByIsbn(isbn)).expectNextCount(0L).verifyComplete();
  }
}
