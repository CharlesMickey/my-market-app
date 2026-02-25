package ru.art.home.market.dto;

import lombok.Data;

@Data
public class PaymentBalanceDto {

    private Long balance;
    private String currency;
}
