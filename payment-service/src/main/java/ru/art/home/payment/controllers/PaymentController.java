package ru.art.home.payment.controllers;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;
import ru.art.home.payment.model.BalanceResponse;
import ru.art.home.payment.model.ErrorResponse;
import ru.art.home.payment.model.PaymentRequest;
import ru.art.home.payment.model.PaymentResponse;
import ru.art.home.payment.model.Transaction;
import ru.art.home.payment.services.PaymentService;

import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final PaymentService paymentService;

    @GetMapping("/balance")
    public Mono<ResponseEntity<BalanceResponse>> getBalance(@AuthenticationPrincipal Mono<Jwt> jwt) {
        log.debug("GET /api/v1/balance called");

        return jwt.flatMap(token -> {
            String clientId = token.getClaimAsString("client_id");
            log.debug("Request from client: {}", clientId);

            return paymentService.getBalance(clientId)
                    .map(balance -> {
                        BalanceResponse response = new BalanceResponse();
                        response.setBalance(balance.getAmount());
                        response.setCurrency(balance.getCurrency());
                        log.debug("Returning balance: {} for client: {}", balance.getAmount(), clientId);
                        return ResponseEntity.ok(response);
                    });
        }).onErrorResume(e -> {
            log.error("Error getting balance", e);
            ErrorResponse error = new ErrorResponse();
            error.setCode("INTERNAL_ERROR");
            error.setMessage(e.getMessage());
            error.setTimestamp(OffsetDateTime.now());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build());
        });
    }

    @PostMapping("/payments")
    public Mono<ResponseEntity<PaymentResponse>> processPayment(
            @RequestBody PaymentRequest request,
            @AuthenticationPrincipal Mono<Jwt> jwt) {

        log.info("POST /api/v1/payments called with request: {}", request);

        if (request == null || request.getAmount() == null || request.getOrderId() == null) {
            PaymentResponse response = new PaymentResponse();
            response.setSuccess(false);
            response.setMessage("Invalid request: amount and orderId are required");
            return Mono.just(ResponseEntity.badRequest().body(response));
        }

        return jwt.flatMap(token -> {
            String clientId = token.getClaimAsString("client_id");
            log.debug("Payment request from client: {}", clientId);

            return paymentService.processPayment(
                    clientId,
                    request.getOrderId(),
                    request.getAmount(),
                    request.getDescription()
            ).map(transaction -> {
                PaymentResponse response = new PaymentResponse();

                if (transaction.getStatus() == Transaction.TransactionStatus.SUCCESS) {
                    response.setSuccess(true);
                    response.setTransactionId(transaction.getId());
                    response.setNewBalance(transaction.getBalanceAfter());
                    response.setMessage("Payment processed successfully");
                    log.info("Payment successful: {} for client: {}", transaction.getId(), clientId);
                    return ResponseEntity.ok(response);
                } else {
                    response.setSuccess(false);
                    response.setTransactionId(transaction.getId());
                    response.setNewBalance(transaction.getBalanceBefore());
                    response.setMessage(transaction.getDescription());
                    log.warn("Payment failed for client {}: {}", clientId, transaction.getDescription());
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
                }
            });
        }).onErrorResume(e -> {
            log.error("Error processing payment", e);
            PaymentResponse response = new PaymentResponse();
            response.setSuccess(false);
            response.setMessage("Internal server error: " + e.getMessage());
            return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(response));
        });
    }
}
