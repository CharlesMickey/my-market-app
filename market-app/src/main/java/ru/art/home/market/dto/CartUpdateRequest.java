package ru.art.home.market.dto;

import lombok.Data;

@Data
public class CartUpdateRequest {

    private Long id;
    private String action;
    private String search;
    private String sort = "NO";
    private Integer pageNumber = 1;
    private Integer pageSize = 5;
}
