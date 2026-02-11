package ru.art.home.market.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Item {
    @Id
    private Long id;
    private String title;
    private String description;
    private String imgPath;
    private Long price;
}
