package ru.art.home.market.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemDto {

    private Long id = -1L;
    private String title = "";
    private String description = "";
    private String imgPath = "";
    private Long price = 0L;
    private Integer count = 0;
}
