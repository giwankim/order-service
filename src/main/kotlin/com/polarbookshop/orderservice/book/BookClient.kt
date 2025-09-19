package com.polarbookshop.orderservice.book

import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.onErrorResume
import reactor.util.retry.Retry
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

private const val BOOKS_ROOT_API = "/books"

@Component
class BookClient(
    private val webClient: WebClient,
) {
    fun getBookByIsbn(isbn: String): Mono<Book> =
        webClient
            .get()
            .uri("$BOOKS_ROOT_API/{isbn}", isbn)
            .retrieve()
            .bodyToMono(Book::class.java)
            .timeout(3.seconds.toJavaDuration(), Mono.empty())
            .onErrorResume(WebClientResponseException.NotFound::class.java) { Mono.empty() }
            .retryWhen(Retry.backoff(3, 100.milliseconds.toJavaDuration()))
            .onErrorResume(Exception::class) { Mono.empty() }
}
