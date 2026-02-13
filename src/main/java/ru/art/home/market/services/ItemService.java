package ru.art.home.market.services;

import java.util.Comparator;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.model.Item;
import ru.art.home.market.repositoryes.ItemRepository;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Flux<ItemDto> getItems(String search, Map<Long, Integer> cartItems, String sort, int pageNumber, int pageSize) {
        Flux<Item> itemsFlux;

        if (search != null && !search.isBlank()) {
            itemsFlux = itemRepository.findBySearch(search);
        } else {
            itemsFlux = itemRepository.findAll();
        }

        if ("ALPHA".equals(sort)) {
            itemsFlux = itemsFlux.sort(Comparator.comparing(Item::getTitle, String.CASE_INSENSITIVE_ORDER));
        } else if ("PRICE".equals(sort)) {
            itemsFlux = itemsFlux.sort(Comparator.comparingLong(Item::getPrice));
        }

        int skip = (pageNumber - 1) * pageSize;
        itemsFlux = itemsFlux.skip(skip).take(pageSize);

        return itemsFlux.map(item -> {
            ItemDto dto = new ItemDto();
            dto.setId(item.getId());
            dto.setTitle(item.getTitle());
            dto.setDescription(item.getDescription());
            dto.setImgPath(item.getImgPath());
            dto.setPrice(item.getPrice());
            dto.setCount(cartItems.getOrDefault(item.getId(), 0));
            return dto;
        });
    }

    public Mono<ItemDto> getItemById(Long id, Map<Long, Integer> cartItems) {
        return itemRepository.findById(id)
                .map(item -> toDto(item, cartItems));
    }

    private ItemDto toDto(Item item, Map<Long, Integer> cartItems) {
        ItemDto dto = new ItemDto();
        dto.setId(item.getId());
        dto.setTitle(item.getTitle());
        dto.setDescription(item.getDescription());
        dto.setImgPath(item.getImgPath());
        dto.setPrice(item.getPrice());
        dto.setCount(cartItems.getOrDefault(item.getId(), 0));
        return dto;
    }

    private Comparator<Item> getComparator(String sort) {
        if ("ALPHA".equalsIgnoreCase(sort)) {
            return Comparator.comparing(Item::getTitle, String.CASE_INSENSITIVE_ORDER);
        } else if ("PRICE".equalsIgnoreCase(sort)) {
            return Comparator.comparingLong(Item::getPrice);
        } else {
            return Comparator.comparing(Item::getId);
        }
    }
}
