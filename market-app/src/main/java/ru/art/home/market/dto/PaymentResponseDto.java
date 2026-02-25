package ru.art.home.market.dto;

import lombok.Data;

@Data
public class PaymentResponseDto {

    private Boolean success;
    private String transactionId;
    private Long newBalance;
    private String message;
}
