package ru.art.home.market.repositoryes;

import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import ru.art.home.market.model.OrderItem;

@Repository
public interface OrderItemRepository extends R2dbcRepository<OrderItem, Long> {

    Flux<OrderItem> findAllByOrderId(Long orderId);
}
