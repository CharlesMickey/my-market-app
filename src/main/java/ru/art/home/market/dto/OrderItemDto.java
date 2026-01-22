package ru.art.home.market.dto;

import lombok.Data;

@Data
public class OrderItemDto {

    private Long id;
    private String title;
    private Long price;
    private Integer count;
}
