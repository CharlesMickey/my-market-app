package ru.art.home.market.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.art.home.market.model.Item;
import ru.art.home.market.repositoryes.ItemRepository;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.when;

class ItemServiceTest {

    private ItemRepository itemRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = Mockito.mock(ItemRepository.class);
        itemService = new ItemService(itemRepository);
    }

    private Item item(Long id, String title, String desc, long price) {
        Item i = new Item();
        i.setId(id);
        i.setTitle(title);
        i.setDescription(desc);
        i.setPrice(price);
        i.setImgPath("img.png");
        return i;
    }

    @Test
    void getItems_shouldSortByAlpha() {
        when(itemRepository.findAll()).thenReturn(Flux.just(
                item(1L, "Banana", "desc", 300),
                item(2L, "Apple", "desc", 200)
        ));

        Map<Long, Integer> cart = Map.of(1L, 2);

        StepVerifier.create(itemService.getItems(null, cart, "ALPHA", 1, 10))
                .assertNext(dto -> {
                    assert dto.getTitle().equals("Apple");
                    assert dto.getCount() == 0;
                })
                .assertNext(dto -> {
                    assert dto.getTitle().equals("Banana");
                    assert dto.getCount() == 2;
                })
                .verifyComplete();
    }

    @Test
    void getItems_shouldSortByPrice() {
        when(itemRepository.findAll()).thenReturn(Flux.just(
                item(1L, "A", "desc", 300),
                item(2L, "B", "desc", 100)
        ));

        StepVerifier.create(itemService.getItems(null, new HashMap<>(), "PRICE", 1, 10))
                .assertNext(dto -> {
                    assert dto.getPrice() == 100;
                })
                .assertNext(dto -> {
                    assert dto.getPrice() == 300;
                })
                .verifyComplete();
    }

    @Test
    void getItems_shouldApplyPagination() {
        when(itemRepository.findAll()).thenReturn(Flux.just(
                item(1L, "A", "desc", 100),
                item(2L, "B", "desc", 200),
                item(3L, "C", "desc", 300)
        ));

        StepVerifier.create(itemService.getItems(null, new HashMap<>(), "NO", 2, 1))
                .assertNext(dto -> {
                    assert dto.getId() == 2L;
                })
                .verifyComplete();
    }

    @Test
    void getItemById_shouldReturnDtoWithCartCount() {
        when(itemRepository.findById(1L))
                .thenReturn(Mono.just(item(1L, "Test", "desc", 500)));

        Map<Long, Integer> cart = Map.of(1L, 3);

        StepVerifier.create(itemService.getItemById(1L, cart))
                .assertNext(dto -> {
                    assert dto.getId() == 1L;
                    assert dto.getCount() == 3;
                })
                .verifyComplete();
    }
}
