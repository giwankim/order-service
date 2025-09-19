package com.polarbookshop.orderservice.book

import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestMethodOrder
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import reactor.test.StepVerifier

@TestMethodOrder(MethodOrderer.Random::class)
class BookClientTest {
    lateinit var mockServer: MockWebServer
    lateinit var bookClient: BookClient

    @BeforeEach
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        val webClient =
            WebClient
                .builder()
                .baseUrl(mockServer.url("/").toString())
                .build()
        bookClient = BookClient(webClient)
    }

    @AfterEach
    fun tearDown() {
        mockServer.shutdown()
    }

    @Test
    fun `when book exists then return book`() {
        val isbn = "1234567890"

        val mockResponse =
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(
                    """
                    {
                      "isbn": "$isbn",
                      "title": "Title",
                      "author": "Author",
                      "price": 9.90,
                      "publisher": "Polarsophia"
                    }
                    """.trimIndent(),
                )
        mockServer.enqueue(mockResponse)

        StepVerifier
            .create(bookClient.getBookByIsbn(isbn))
            .expectNextMatches { it.isbn == isbn }
            .verifyComplete()
    }

    @Test
    fun `when book does not exist then return empty`() {
        val isbn = "1234567891"

        val mockResponse =
            MockResponse()
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(HttpStatus.NOT_FOUND.value())
        mockServer.enqueue(mockResponse)

        StepVerifier
            .create(bookClient.getBookByIsbn(isbn))
            .expectNextCount(0L)
            .verifyComplete()
    }
}
