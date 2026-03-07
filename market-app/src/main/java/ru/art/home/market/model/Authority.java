package ru.art.home.market.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Table("authorities")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Authority {

    @Id
    private Long id;
    private String username;
    private String authority;
}
