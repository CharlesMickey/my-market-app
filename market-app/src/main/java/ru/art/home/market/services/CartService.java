package ru.art.home.market.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.client.PaymentServiceClient;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.dto.PaymentRequestDto;
import ru.art.home.market.exception.PaymentServiceUnavailableException;
import ru.art.home.market.repositoryes.ItemRepository;

@Service
@RequiredArgsConstructor
public class CartService {

    private final ItemRepository itemRepository;
    private final OrderService orderService;
    private final PaymentServiceClient paymentServiceClient;

    public Flux<ItemDto> getCartItems(Map<Long, Integer> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return Flux.empty();
        }

        return Flux.fromIterable(cartItems.entrySet())
                .flatMap(entry -> itemRepository.findById(entry.getKey())
                        .map(item -> {
                            ItemDto dto = new ItemDto();
                            dto.setId(item.getId());
                            dto.setTitle(item.getTitle());
                            dto.setDescription(item.getDescription());
                            dto.setImgPath(item.getImgPath());
                            dto.setPrice(item.getPrice());
                            dto.setCount(entry.getValue());
                            return dto;
                        }));
    }

    public Mono<Long> calculateTotal(Flux<ItemDto> cartItems) {
        return cartItems
                .map(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    public Map<Long, Integer> updateCart(Map<Long, Integer> cartItems, Long itemId, String action) {
        Map<Long, Integer> updatedCart = new HashMap<>(cartItems);
        int currentCount = updatedCart.getOrDefault(itemId, 0);

        switch (action) {
            case "PLUS" ->
                    updatedCart.put(itemId, currentCount + 1);
            case "MINUS" -> {
                if (currentCount > 1) {
                    updatedCart.put(itemId, currentCount - 1);
                } else {
                    updatedCart.remove(itemId);
                }
            }
            case "DELETE" ->
                    updatedCart.remove(itemId);
        }

        return updatedCart;
    }

    public Mono<Long> createOrder(Map<Long, Integer> cartItems) {
        return orderService.createOrder(cartItems);
    }

    public List<List<ItemDto>> groupItemsForDisplay(List<ItemDto> items) {
        List<List<ItemDto>> groupedItems = new ArrayList<>();

        for (int i = 0; i < items.size(); i += 3) {
            List<ItemDto> row = new ArrayList<>();
            for (int j = 0; j < 3 && i + j < items.size(); j++) {
                row.add(items.get(i + j));
            }
            while (row.size() < 3) {
                row.add(new ItemDto(-1L, "", "", "", 0L, 0));
            }
            groupedItems.add(row);
        }

        return groupedItems;
    }

    public Mono<Boolean> canPayCart(Map<Long, Integer> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return Mono.just(false);
        }

        return getCartItems(cartItems)
                .collectList()
                .flatMap(items -> {
                    Long total = items.stream()
                            .mapToLong(item -> item.getPrice() * item.getCount())
                            .sum();

                    return paymentServiceClient.getBalance()
                            .map(balance -> balance.getBalance() >= total)
                            .onErrorReturn(PaymentServiceUnavailableException.class, false);
                });
    }


    public Mono<String> getPaymentStatusMessage(Map<Long, Integer> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return Mono.just("Корзина пуста");
        }

        return paymentServiceClient.isServiceAvailable()
                .flatMap(available -> {
                    if (!available) {
                        return Mono.just("Сервис платежей временно недоступен");
                    }

                    return getCartItems(cartItems)
                            .collectList()
                            .flatMap(items -> {
                                Long total = items.stream()
                                        .mapToLong(item -> item.getPrice() * item.getCount())
                                        .sum();

                                return paymentServiceClient.getBalance()
                                        .map(balance -> {
                                            if (balance.getBalance() >= total) {
                                                return "Достаточно средств для оплаты";
                                            } else {
                                                double needAmount = (total - balance.getBalance()) / 100.0;
                                                return String.format("Недостаточно средств. Нужно ещё %.2f RUB", needAmount);
                                            }
                                        })
                                        .onErrorReturn("Ошибка проверки баланса");
                            });
                })
                .defaultIfEmpty("Сервис платежей недоступен");
    }

    public Mono<Long> createOrderWithPayment(Map<Long, Integer> cartItems) {
        if (cartItems == null || cartItems.isEmpty()) {
            return Mono.error(new IllegalArgumentException("Cart is empty"));
        }

        return getCartItems(cartItems)
                .collectList()
                .flatMap(items -> {
                    Long total = items.stream()
                            .mapToLong(item -> item.getPrice() * item.getCount())
                            .sum();

                    return orderService.createOrder(cartItems)
                            .flatMap(orderId -> {
                                PaymentRequestDto paymentRequest = new PaymentRequestDto(
                                        orderId,
                                        total,
                                        "Оплата заказа №" + orderId
                                );

                                return paymentServiceClient.processPayment(paymentRequest)
                                        .flatMap(response -> {
                                            if (response.getSuccess()) {
                                                return Mono.just(orderId);
                                            } else {
                                                  return orderService.deleteOrder(orderId)
                                                        .then(Mono.error(new RuntimeException(
                                                                "Payment failed: " + response.getMessage())));
                                            }
                                        });
                            });
                });
    }

}
