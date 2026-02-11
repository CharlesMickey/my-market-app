package ru.art.home.market.model;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

@Table("orders")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    private Long id;
    private LocalDateTime createdAt;
    private Long totalSum;
}
