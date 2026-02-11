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

    public Mono<Long> createOrder(Map<Long, Integer> cartItems) {

        return Flux.fromIterable(cartItems.entrySet())
                .flatMap(entry -> itemRepository.findById(entry.getKey())
                        .map(item -> {

                            long totalPrice = item.getPrice() * entry.getValue();
                            OrderItem oi = new OrderItem();
                            oi.setItemId(item.getId());
                            oi.setCount(entry.getValue());
                            oi.setPrice(item.getPrice());
                            oi.setOrderId(null);
                            return new Object[]{oi, totalPrice};
                        }))
                .collectList()
                .flatMap(list -> {
                    long totalSum = list.stream()
                            .mapToLong(obj -> (long) obj[1])
                            .sum();

                    Order order = new Order();
                    order.setCreatedAt(LocalDateTime.now());
                    order.setTotalSum(totalSum);

                    return orderRepository.save(order)
                            .flatMap(savedOrder -> {

                                return Flux.fromIterable(list)
                                        .flatMap(obj -> {
                                            OrderItem oi = (OrderItem) obj[0];
                                            oi.setOrderId(savedOrder.getId());
                                            return orderItemRepository.save(oi);
                                        })
                                        .then(Mono.just(savedOrder.getId()));
                            });
                });
    }

    public Flux<OrderDto> getAllOrders() {
        return orderRepository.findAll()
                .flatMap(order -> orderItemRepository.findAllByOrderId(order.getId())
                        .map(oi -> {
                            OrderItemDto dto = new OrderItemDto();
                            dto.setId(oi.getItemId());
                            dto.setTitle("");
                            dto.setCount(oi.getCount());
                            dto.setPrice(oi.getPrice());
                            return dto;
                        })
                        .collectList()
                        .map(list -> {
                            OrderDto dto = new OrderDto();
                            dto.setId(order.getId());
                            dto.setCreatedAt(order.getCreatedAt());
                            dto.setTotalSum(order.getTotalSum());
                            dto.setItems(list);
                            return dto;
                        }));
    }

    public Mono<OrderDto> getOrderById(Long id) {
        return orderRepository.findById(id)
                .flatMap(order -> orderItemRepository.findAllByOrderId(order.getId())
                        .map(oi -> {
                            OrderItemDto dto = new OrderItemDto();
                            dto.setId(oi.getItemId());
                            dto.setTitle("");
                            dto.setCount(oi.getCount());
                            dto.setPrice(oi.getPrice());
                            return dto;
                        })
                        .collectList()
                        .map(list -> {
                            OrderDto dto = new OrderDto();
                            dto.setId(order.getId());
                            dto.setCreatedAt(order.getCreatedAt());
                            dto.setTotalSum(order.getTotalSum());
                            dto.setItems(list);
                            return dto;
                        }));
    }
}
