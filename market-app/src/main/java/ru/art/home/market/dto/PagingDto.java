package ru.art.home.market.dto;

import lombok.Data;

@Data
public class PagingDto {

    private int pageSize;
    private int pageNumber;
    private boolean hasPrevious;
    private boolean hasNext;
}
