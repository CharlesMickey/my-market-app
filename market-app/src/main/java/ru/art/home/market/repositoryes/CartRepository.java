package ru.art.home.market.repositoryes;

import org.springframework.data.r2dbc.repository.Modifying;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.model.Cart;

@Repository
public interface CartRepository extends ReactiveCrudRepository<Cart, Long> {

    Flux<Cart> findAllByUserId(Long userId);

    Mono<Cart> findByUserIdAndItemId(Long userId, Long itemId);

    @Modifying
    @Query("DELETE FROM carts WHERE user_id = :userId AND item_id = :itemId")
    Mono<Void> deleteByUserIdAndItemId(Long userId, Long itemId);

    @Modifying
    @Query("DELETE FROM carts WHERE user_id = :userId")
    Mono<Void> deleteAllByUserId(Long userId);
}
