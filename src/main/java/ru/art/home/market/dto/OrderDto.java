package ru.art.home.market.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;

@Data
public class OrderDto {

    private Long id;
    private LocalDateTime createdAt;
    private Long totalSum;
    private List<OrderItemDto> items;
}
