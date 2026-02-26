package ru.art.home.payment.services;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import reactor.test.StepVerifier;
import ru.art.home.payment.model.Transaction;

class PaymentServiceTest {

    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentService(1000L);
    }

    @Test
    void processPayment_success() {
        StepVerifier.create(paymentService.processPayment(1L, 200L, "Test payment"))
                .assertNext(tx -> {
                    assertThat(tx.getStatus()).isEqualTo(Transaction.TransactionStatus.SUCCESS);
                    assertThat(tx.getBalanceBefore()).isEqualTo(1000L);
                    assertThat(tx.getBalanceAfter()).isEqualTo(800L);
                })
                .verifyComplete();
    }

    @Test
    void processPayment_insufficientFunds() {
        StepVerifier.create(paymentService.processPayment(1L, 2000L, "Too big"))
                .assertNext(tx -> {
                    assertThat(tx.getStatus())
                            .isEqualTo(Transaction.TransactionStatus.FAILED_INSUFFICIENT_FUNDS);
                    assertThat(tx.getBalanceBefore()).isEqualTo(1000L);
                })
                .verifyComplete();
    }

    @Test
    void processPayment_invalidAmount() {
        StepVerifier.create(paymentService.processPayment(1L, -100L, "Invalid"))
                .expectError(IllegalArgumentException.class)
                .verify();
    }
}
