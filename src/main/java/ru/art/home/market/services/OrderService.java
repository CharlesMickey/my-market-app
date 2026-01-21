package ru.art.home.market.services;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import ru.art.home.market.dto.OrderDto;
import ru.art.home.market.dto.OrderItemDto;
import ru.art.home.market.model.Item;
import ru.art.home.market.model.Order;
import ru.art.home.market.model.OrderItem;
import ru.art.home.market.repositoryes.ItemRepository;
import ru.art.home.market.repositoryes.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ItemRepository itemRepository;

    @Transactional
    public Long createOrder(Map<Long, Integer> cartItems) {
        Order order = new Order();
        order.setCreatedAt(LocalDateTime.now());

        List<OrderItem> orderItems = new ArrayList<>();
        long totalSum = 0;

        for (Map.Entry<Long, Integer> entry : cartItems.entrySet()) {
            Item item = itemRepository.findById(entry.getKey())
                    .orElseThrow(() -> new RuntimeException("Item not found"));

            OrderItem orderItem = new OrderItem();
            orderItem.setItem(item);
            orderItem.setCount(entry.getValue());
            orderItem.setPrice(item.getPrice());

            orderItems.add(orderItem);
            totalSum += item.getPrice() * entry.getValue();
        }

        order.setItems(orderItems);
        order.setTotalSum(totalSum);

        Order savedOrder = orderRepository.save(order);
        return savedOrder.getId();
    }

    public List<OrderDto> getAllOrders() {
        return orderRepository.findAll().stream()
                .map(this::convertToDto)
                .toList();
    }

    public OrderDto getOrderById(Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Order not found"));
        return convertToDto(order);
    }

    private OrderDto convertToDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setTotalSum(order.getTotalSum());

        List<OrderItemDto> itemDtos = order.getItems().stream()
                .map(this::convertToItemDto)
                .toList();
        dto.setItems(itemDtos);

        return dto;
    }

    private OrderItemDto convertToItemDto(OrderItem orderItem) {
        OrderItemDto dto = new OrderItemDto();
        dto.setId(orderItem.getItem().getId());
        dto.setTitle(orderItem.getItem().getTitle());
        dto.setPrice(orderItem.getPrice());
        dto.setCount(orderItem.getCount());
        return dto;
    }
}
