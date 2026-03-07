package ru.art.home.market.repositoryes;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import ru.art.home.market.model.Authority;

@Repository
public interface AuthorityRepository extends ReactiveCrudRepository<Authority, Long> {

    Flux<Authority> findByUsername(String username);
}
