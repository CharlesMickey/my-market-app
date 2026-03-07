package ru.art.home.market.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("carts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Cart {
    @Id
    private Long id;
    private Long userId;
    private Long itemId;
    private Integer count;
}