package ru.art.home.market.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.model.Item;
import ru.art.home.market.repositoryes.ItemRepository;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void getItems_withoutSearch() {
        Item item = new Item(1L, "Phone", "Desc", "/img.png", 1000L);
        Page<Item> page = new PageImpl<>(List.of(item));

        when(itemRepository.findAll(any(Pageable.class)))
                .thenReturn(page);

        Page<ItemDto> result = itemService.getItems(
                null, null, 1, 10, Map.of(1L, 2)
        );

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getCount()).isEqualTo(2);
    }
}
