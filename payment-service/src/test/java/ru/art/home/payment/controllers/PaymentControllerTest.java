package ru.art.home.payment.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.art.home.payment.model.PaymentRequest;
import ru.art.home.payment.model.Transaction;
import ru.art.home.payment.services.PaymentService;

import static org.mockito.Mockito.*;

@WebFluxTest(PaymentController.class)
class PaymentControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private PaymentService paymentService;

    @Test
    void processPayment_success() {

        Transaction transaction = new Transaction();
        transaction.setId("tx-123");
        transaction.setStatus(Transaction.TransactionStatus.SUCCESS);
        transaction.setBalanceBefore(500L);
        transaction.setBalanceAfter(300L);
        transaction.setDescription("OK");

        when(paymentService.processPayment(100L, 200L, null))
                .thenReturn(Mono.just(transaction));

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(100L);
        request.setAmount(200L);

        webTestClient.post()
                .uri("/api/v1/payments")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.transactionId").isEqualTo("tx-123")
                .jsonPath("$.newBalance").isEqualTo(300);

        verify(paymentService).processPayment(100L, 200L, null);
    }

    @Test
    void processPayment_insufficientFunds() {

        Transaction transaction = new Transaction();
        transaction.setId("tx-456");
        transaction.setStatus(Transaction.TransactionStatus.FAILED_OTHER);
        transaction.setBalanceBefore(100L);
        transaction.setBalanceAfter(100L);
        transaction.setDescription("Insufficient funds");

        when(paymentService.processPayment(100L, 200L, null))
                .thenReturn(Mono.just(transaction));

        PaymentRequest request = new PaymentRequest();
        request.setOrderId(100L);
        request.setAmount(200L);

        webTestClient.post()
                .uri("/api/v1/payments")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message").isEqualTo("Insufficient funds");

        verify(paymentService).processPayment(100L, 200L, null);
    }

    @Test
    void processPayment_invalidRequest() {

        PaymentRequest request = new PaymentRequest(); // пустой

        webTestClient.post()
                .uri("/api/v1/payments")
                .bodyValue(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.message")
                .isEqualTo("Invalid request: amount and orderId are required");

        verifyNoInteractions(paymentService);
    }
}
