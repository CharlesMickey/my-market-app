package ru.art.home.market.repositoryes;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Flux;
import ru.art.home.market.model.Item;

@Repository
public interface ItemRepository extends ReactiveCrudRepository<Item, Long> {

    default Flux<Item> findBySearch(String search) {
        String lowerSearch = search.toLowerCase();
        return findAll()
                .filter(item -> item.getTitle().toLowerCase().contains(lowerSearch)
                        || item.getDescription().toLowerCase().contains(lowerSearch));
    }
}
