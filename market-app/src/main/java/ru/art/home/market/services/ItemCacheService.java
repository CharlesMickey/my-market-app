package ru.art.home.market.services;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.model.Item;
import ru.art.home.market.repositoryes.ItemRepository;

import java.time.Duration;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ItemCacheService {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private final ItemRepository itemRepository;
    private final ObjectMapper objectMapper;

    private static final String ITEM_KEY_PREFIX = "item:";
    private static final String ITEMS_LIST_KEY = "items:all";
    private static final String ITEM_KEY_PATTERN = ITEM_KEY_PREFIX + "*";
    private static final Duration CACHE_TTL = Duration.ofMinutes(2);

    public Mono<ItemDto> getItemById(Long id, Map<Long, Integer> cartItems) {
        String cacheKey = ITEM_KEY_PREFIX + id;

        return redisTemplate.opsForValue().get(cacheKey)
                .flatMap(obj -> {
                    Item item;
                    if (obj instanceof Item) {
                        item = (Item) obj;
                    } else {
                        try {
                            item = objectMapper.convertValue(obj, Item.class);
                        } catch (Exception e) {
                            log.error("Failed to convert cached object to Item for key: {}", cacheKey, e);
                            return Mono.empty();
                        }
                    }
                    return Mono.just(item);
                })
                .switchIfEmpty(
                        itemRepository.findById(id)
                                .flatMap(item -> redisTemplate.opsForValue()
                                .set(cacheKey, item, CACHE_TTL)
                                .thenReturn(item))
                )
                .map(item -> toDto(item, cartItems));
    }

    public Flux<ItemDto> getItemsWithCart(String search, String sort, int pageNumber, int pageSize, Map<Long, Integer> cartItems) {
        return getItems(search, sort, pageNumber, pageSize)
                .map(item -> {
                    item.setCount(cartItems.getOrDefault(item.getId(), 0));
                    return item;
                });
    }

    public Flux<ItemDto> getItems(String search, String sort, int pageNumber, int pageSize) {
        Flux<Item> itemsFlux;

        if (search != null && !search.isBlank()) {
            itemsFlux = searchItems(search);
        } else {
            itemsFlux = getAllItems();
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
            dto.setCount(0);
            return dto;
        });
    }

    public Flux<Item> getAllItems() {
        return redisTemplate.opsForValue().get(ITEMS_LIST_KEY)
                .flatMapMany(obj -> {
                    if (obj instanceof List) {
                        List<?> list = (List<?>) obj;
                        if (!list.isEmpty() && list.get(0) instanceof Item) {
                            return Flux.fromIterable((List<Item>) list);
                        } else {
                            try {
                                List<Item> items = objectMapper.convertValue(obj, new TypeReference<List<Item>>() {
                                });
                                return Flux.fromIterable(items);
                            } catch (Exception e) {
                                log.error("Failed to convert list to Items", e);
                                return Flux.empty();
                            }
                        }
                    }
                    return Flux.empty();
                })
                .switchIfEmpty(
                        itemRepository.findAll()
                                .collectList()
                                .flatMapMany(items -> redisTemplate.opsForValue()
                                .set(ITEMS_LIST_KEY, items, CACHE_TTL)
                                .thenMany(Flux.fromIterable(items))
                                )
                );
    }

    public Flux<Item> searchItems(String search) {
        return getAllItems()
                .filter(item -> search == null || search.isBlank()
                || item.getTitle().toLowerCase().contains(search.toLowerCase())
                || item.getDescription().toLowerCase().contains(search.toLowerCase()));
    }

    public Mono<Void> invalidateCache() {
        return redisTemplate.keys(ITEM_KEY_PATTERN)
                .collectList()
                .flatMapMany(keys -> {
                    if (keys.isEmpty()) {
                        return Flux.empty();
                    }
                    return redisTemplate.delete(keys.toArray(new String[0]));
                })
                .then(redisTemplate.delete(ITEMS_LIST_KEY))
                .then();
    }

    public Mono<Void> invalidateItemCache(Long id) {
        return redisTemplate.delete(ITEM_KEY_PREFIX + id)
                .then(redisTemplate.delete(ITEMS_LIST_KEY))
                .then();
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
}
