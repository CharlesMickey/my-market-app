package ru.art.home.market.dto;

import lombok.Data;

@Data
public class CartItemUpdateRequest {

    private Long id;
    private String action;
}
