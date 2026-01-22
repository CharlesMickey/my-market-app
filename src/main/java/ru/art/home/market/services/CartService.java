package ru.art.home.market.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.repositoryes.ItemRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ItemRepository itemRepository;
    private final OrderService orderService;

    public List<ItemDto> getCartItems(Map<Long, Integer> cartItems) {
        List<ItemDto> items = new ArrayList<>();

        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {
            itemRepository.findById(entry.getKey()).ifPresent(item -> {
                ItemDto dto = new ItemDto();
                dto.setId(item.getId());
                dto.setTitle(item.getTitle());
                dto.setDescription(item.getDescription());
                dto.setImgPath(item.getImgPath());
                dto.setPrice(item.getPrice());
                dto.setCount(entry.getValue());
                items.add(dto);
            });
        }

        return items;
    }

    public long calculateTotal(List<ItemDto> cartItems) {
        return cartItems.stream()
                .mapToLong(item -> item.getPrice() * item.getCount())
                .sum();
    }

    public Map<Long, Integer> updateCart(Map<Long, Integer> cartItems, Long itemId, String action) {
        Map<Long, Integer> updatedCart = new HashMap<>(cartItems);
        int currentCount = updatedCart.getOrDefault(itemId, 0);

        switch (action) {
            case "PLUS":
                updatedCart.put(itemId, currentCount + 1);
                break;
            case "MINUS":
                if (currentCount > 1) {
                    updatedCart.put(itemId, currentCount - 1);
                } else {
                    updatedCart.remove(itemId);
                }
                break;
            case "DELETE":
                updatedCart.remove(itemId);
                break;
        }

        return updatedCart;
    }

    @Transactional
    public Long createOrder(Map<Long, Integer> cartItems) {
        return orderService.createOrder(cartItems);
    }
}
