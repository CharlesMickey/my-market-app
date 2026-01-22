package ru.art.home.market.repositoryes;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import ru.art.home.market.model.Order;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
}
