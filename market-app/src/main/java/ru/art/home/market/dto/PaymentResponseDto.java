package ru.art.home.market.dto;

public record PaymentResponseDto(
        Boolean success,
        String transactionId,
        Long newBalance,
        String message
        ) {

}
