package ru.art.home.payment.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import reactor.core.publisher.Mono;
import ru.art.home.payment.model.*;
import ru.art.home.payment.services.PaymentService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PaymentControllerTest {

    private PaymentService paymentService;
    private PaymentController paymentController;
    private Jwt testJwt;

    @BeforeEach
    void setUp() {
        paymentService = mock(PaymentService.class);
        paymentController = new PaymentController(paymentService);
        testJwt = mock(Jwt.class);
        when(testJwt.getClaimAsString("client_id")).thenReturn("client123");
    }

    @Test
    void testGetBalanceSuccess() {
        Balance balance = new Balance();
        balance.setAmount(5000L);
        balance.setCurrency("RUB");

        when(paymentService.getBalance("client123")).thenReturn(Mono.just(balance));

        var responseMono = paymentController.getBalance(Mono.just(testJwt));

        BalanceResponse response = responseMono.block().getBody();

        assertNotNull(response);
        assertEquals(5000L, response.getBalance());
        assertEquals("RUB", response.getCurrency());

        verify(paymentService, times(1)).getBalance("client123");
    }

    @Test
    void testProcessPaymentSuccess() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setAmount(1000L);
        request.setDescription("Test payment");

        Transaction transaction = Transaction.createSuccess(
                1L, 1000L, 5000L, 4000L, "Test payment"
        );

        when(paymentService.processPayment("client123", 1L, 1000L, "Test payment"))
                .thenReturn(Mono.just(transaction));

        var responseMono = paymentController.processPayment(request, Mono.just(testJwt));

        PaymentResponse response = responseMono.block().getBody();

        assertNotNull(response);
        assertTrue(response.getSuccess());
        assertEquals(4000L, response.getNewBalance());
        assertEquals("Payment processed successfully", response.getMessage());
    }

    @Test
    void testProcessPaymentInsufficientFunds() {
        PaymentRequest request = new PaymentRequest();
        request.setOrderId(1L);
        request.setAmount(10000L);

        Transaction failedTx = Transaction.createFailed(
                1L, 10000L, 5000L,
                Transaction.TransactionStatus.FAILED_INSUFFICIENT_FUNDS,
                "Insufficient funds"
        );

        when(paymentService.processPayment("client123", 1L, 10000L, null))
                .thenReturn(Mono.just(failedTx));

        var responseMono = paymentController.processPayment(request, Mono.just(testJwt));

        PaymentResponse response = responseMono.block().getBody();

        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertEquals(5000L, response.getNewBalance());
        assertEquals("Insufficient funds", response.getMessage());
    }

    @Test
    void testProcessPaymentInvalidRequest() {
        PaymentRequest request = new PaymentRequest();

        var responseMono = paymentController.processPayment(request, Mono.just(testJwt));

        PaymentResponse response = responseMono.block().getBody();

        assertNotNull(response);
        assertFalse(response.getSuccess());
        assertTrue(response.getMessage().contains("Invalid request"));
    }
}