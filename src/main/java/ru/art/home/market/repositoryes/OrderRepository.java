package ru.art.home.market.repositoryes;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import ru.art.home.market.model.Order;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, Long> {
}
