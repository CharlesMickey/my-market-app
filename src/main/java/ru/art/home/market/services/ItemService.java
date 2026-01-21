package ru.art.home.market.services;

import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.exception.NotFoundException;
import ru.art.home.market.model.Item;
import ru.art.home.market.repositoryes.ItemRepository;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Page<ItemDto> getItems(String search, String sort, int pageNumber, int pageSize,
            Map<Long, Integer> cartItems) {
        Pageable pageable = createPageable(sort, pageNumber, pageSize);
        Page<Item> itemsPage;

        if (search != null && !search.trim().isEmpty()) {
            itemsPage = itemRepository.findBySearch(search.trim(), pageable);
        } else {
            itemsPage = itemRepository.findAll(pageable);
        }

        return itemsPage.map(item -> convertToDto(item, cartItems));
    }

    public ItemDto getItemById(Long id, Map<Long, Integer> cartItems) {
        Item item = itemRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Item not found"));
        return convertToDto(item, cartItems);
    }

    private Pageable createPageable(String sort, int pageNumber, int pageSize) {
        Sort sorting = Sort.unsorted();

        if ("ALPHA".equals(sort)) {
            sorting = Sort.by("title").ascending();
        } else if ("PRICE".equals(sort)) {
            sorting = Sort.by("price").ascending();
        }

        return PageRequest.of(pageNumber - 1, pageSize, sorting);
    }

    private ItemDto convertToDto(Item item, Map<Long, Integer> cartItems) {
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
