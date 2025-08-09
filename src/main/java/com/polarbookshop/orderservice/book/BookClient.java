package com.polarbookshop.orderservice.book;

import java.time.Duration;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

@Component
@RequiredArgsConstructor
public class BookClient {

  public static final String BOOKS_API = "/books/{isbn}";
  private final WebClient webClient;

  public Mono<Book> getBookByIsbn(String isbn) {
    return webClient
        .get()
        .uri(BOOKS_API, isbn)
        .retrieve()
        .bodyToMono(Book.class)
        .timeout(Duration.ofSeconds(3), Mono.empty())
        .onErrorResume(WebClientResponseException.class, e -> Mono.empty())
        .retryWhen(Retry.backoff(3, Duration.ofMillis(100)))
        .onErrorResume(Exception.class, e -> Mono.empty());
  }
}
