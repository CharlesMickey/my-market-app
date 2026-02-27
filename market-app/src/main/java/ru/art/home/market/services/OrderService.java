package ru.art.home.market.services;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.OrderDto;
import ru.art.home.market.dto.OrderItemDto;
import ru.art.home.market.model.Order;
import ru.art.home.market.model.OrderItem;
import ru.art.home.market.repositoryes.ItemRepository;
import ru.art.home.market.repositoryes.OrderItemRepository;
import ru.art.home.market.repositoryes.OrderRepository;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ItemRepository itemRepository;

    private record ItemOrderData(OrderItem orderItem, long totalPrice) {

    }

    public Mono<Long> createOrder(Map<Long, Integer> cartItems) {
        return Flux.fromIterable(cartItems.entrySet())
                .flatMap(entry -> itemRepository.findById(entry.getKey())
                        .map(item -> {
                            OrderItem oi = new OrderItem();
                            oi.setItemId(item.getId());
                            oi.setCount(entry.getValue());
                            oi.setPrice(item.getPrice());
                            oi.setOrderId(null);
                            long totalPrice = item.getPrice() * entry.getValue();
                            return new ItemOrderData(oi, totalPrice);
                        }))
                .collectList()
                .flatMap(list -> {
                    long totalSum = list.stream()
                            .mapToLong(ItemOrderData::totalPrice)
                            .sum();

                    Order order = new Order();
                    order.setCreatedAt(LocalDateTime.now());
                    order.setTotalSum(totalSum);

                    return orderRepository.save(order)
                            .flatMap(savedOrder
                                    -> Flux.fromIterable(list)
                                    .flatMap(data -> {
                                        OrderItem oi = data.orderItem();
                                        oi.setOrderId(savedOrder.getId());
                                        return orderItemRepository.save(oi);
                                    })
                                    .then(Mono.just(savedOrder.getId()))
                            );
                });
    }

    public Flux<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(this::enhanceOrderWithItems);
    }

    public Mono<OrderDto> getOrderById(Long id) {
        return orderRepository.findById(id)
                .flatMap(this::enhanceOrderWithItems);
    }

    public Mono<Void> deleteOrder(Long id) {
        return orderRepository.deleteById(id);
    }

    private Mono<OrderDto> enhanceOrderWithItems(Order order) {
        return orderItemRepository.findAllByOrderId(order.getId())
                .flatMap(oi -> itemRepository.findById(oi.getItemId())
                        .map(item -> {
                            OrderItemDto dto = new OrderItemDto();
                            dto.setId(oi.getItemId());
                            dto.setTitle(item.getTitle());
                            dto.setCount(oi.getCount());
                            dto.setPrice(oi.getPrice());
                            return dto;
                        })
                )
                .collectList()
                .map(items -> {
                    OrderDto dto = new OrderDto();
                    dto.setId(order.getId());
                    dto.setCreatedAt(order.getCreatedAt());
                    dto.setTotalSum(order.getTotalSum());
                    dto.setItems(items);
                    return dto;
                });
    }
}
