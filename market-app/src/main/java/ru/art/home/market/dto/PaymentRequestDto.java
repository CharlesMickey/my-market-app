package ru.art.home.market.dto;

public record PaymentRequestDto(
        Long orderId,
        Long amount,
        String description
        ) {

}
