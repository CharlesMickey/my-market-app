package ru.art.home.market.client;

import java.time.Duration;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.PaymentBalanceDto;
import ru.art.home.market.dto.PaymentRequestDto;
import ru.art.home.market.dto.PaymentResponseDto;
import ru.art.home.market.exception.PaymentServiceUnavailableException;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceClient {

    private final WebClient paymentWebClient;
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    public Mono<PaymentBalanceDto> getBalance() {
        log.debug("Requesting balance from payment service");

        return paymentWebClient.get()
                .uri("/api/v1/balance")
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .bodyToMono(PaymentBalanceDto.class)
                .timeout(TIMEOUT)
                .doOnSuccess(balance -> log.debug("Received balance: {}", balance))
                .doOnError(error -> log.error("Failed to get balance", error))
                .onErrorResume(this::handleBalanceError);
    }

    public Mono<PaymentResponseDto> processPayment(PaymentRequestDto request) {
        log.debug("Processing payment: orderId={}, amount={}", request.orderId(), request.amount());

        return paymentWebClient.post()
                .uri("/api/v1/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PaymentResponseDto.class)
                .timeout(TIMEOUT)
                .doOnSuccess(response -> log.debug("Payment response: {}", response))
                .doOnError(error -> log.error("Failed to process payment", error))
                .onErrorResume(this::handlePaymentError);
    }

    public Mono<Boolean> isServiceAvailable() {
        return paymentWebClient.get()
                .uri("/actuator/health")
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> true)
                .timeout(TIMEOUT)
                .onErrorReturn(false);
    }

    private Mono<PaymentBalanceDto> handleBalanceError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            if (wcre.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE
                    || wcre.getStatusCode() == HttpStatus.INTERNAL_SERVER_ERROR) {
                return Mono.error(new PaymentServiceUnavailableException(
                        "Payment service is unavailable", error));
            }
        }
        return Mono.error(new PaymentServiceUnavailableException(
                "Failed to connect to payment service", error));
    }

    private Mono<PaymentResponseDto> handlePaymentError(Throwable error) {
        if (error instanceof WebClientResponseException) {
            WebClientResponseException wcre = (WebClientResponseException) error;
            if (wcre.getStatusCode() == HttpStatus.BAD_REQUEST) {
                try {
                    PaymentResponseDto response = wcre.getResponseBodyAs(PaymentResponseDto.class);
                    if (response != null) {
                        return Mono.just(response);
                    }
                } catch (Exception e) {
                    log.warn("Could not extract error response", e);
                }
            }
        }
        return Mono.error(new PaymentServiceUnavailableException(
                "Payment service error: " + error.getMessage(), error));
    }
}
