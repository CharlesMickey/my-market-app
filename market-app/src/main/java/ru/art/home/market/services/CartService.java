package ru.art.home.market.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.client.PaymentServiceClient;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.dto.PaymentRequestDto;
import ru.art.home.market.model.Cart;
import ru.art.home.market.model.User;
import ru.art.home.market.repositoryes.CartRepository;
import ru.art.home.market.repositoryes.ItemRepository;
import ru.art.home.market.repositoryes.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartRepository cartRepository;
    private final ItemRepository itemRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final PaymentServiceClient paymentServiceClient;

    private Mono<Long> getCurrentUserId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .flatMap(auth -> {
                    String username = auth.getName();
                    return userRepository.findByUsername(username)
                            .map(User::getId)
                            .switchIfEmpty(Mono.error(new RuntimeException("User not found: " + username)));
                });
    }

    public Flux<ItemDto> getCartItems() {
        return getCurrentUserId()
                .flatMapMany(userId -> cartRepository.findAllByUserId(userId)
                .flatMap(cart -> itemRepository.findById(cart.getItemId())
                .map(item -> {
                    ItemDto dto = new ItemDto();
                    dto.setId(item.getId());
                    dto.setTitle(item.getTitle());
                    dto.setDescription(item.getDescription());
                    dto.setImgPath(item.getImgPath());
                    dto.setPrice(item.getPrice());
                    dto.setCount(cart.getCount());
                    return dto;
                })));
    }

    public Mono<Long> calculateTotal() {
        return getCartItems()
                .map(item -> item.getPrice() * item.getCount())
                .reduce(0L, Long::sum);
    }

    @Transactional
    public Mono<Void> updateCart(Long itemId, String action) {
        return getCurrentUserId()
                .flatMap(userId -> {
                    if ("DELETE".equals(action)) {
                        return cartRepository.deleteByUserIdAndItemId(userId, itemId);
                    }

                    return cartRepository.findByUserIdAndItemId(userId, itemId)
                            .flatMap(cart -> {
                                int newCount = "PLUS".equals(action) ? cart.getCount() + 1 : cart.getCount() - 1;

                                if (newCount <= 0) {
                                    return cartRepository.delete(cart).then();
                                }

                                cart.setCount(newCount);
                                return cartRepository.save(cart).then();
                            })
                            .switchIfEmpty(Mono.defer(() -> {
                                if ("PLUS".equals(action)) {
                                    Cart newCart = new Cart();
                                    newCart.setUserId(userId);
                                    newCart.setItemId(itemId);
                                    newCart.setCount(1);
                                    return cartRepository.save(newCart).then();
                                }
                                return Mono.empty();
                            }));
                })
                .onErrorResume(DuplicateKeyException.class, e -> {
                    log.debug("Duplicate key ignored for userId={}, itemId={}", itemId, itemId);
                    return Mono.empty();
                });
    }

    @Transactional
    public Mono<Void> clearCart() {
        return getCurrentUserId()
                .flatMap(userId -> cartRepository.deleteAllByUserId(userId));
    }

    public Mono<Boolean> canPayCart() {
        return getCurrentUserId()
                .flatMap(userId -> userRepository.findById(userId)
                .flatMap(user -> calculateTotal()
                .map(total -> user.getBalance() >= total)))
                .defaultIfEmpty(false);
    }

    public Mono<String> getPaymentStatusMessage() {
        return getCurrentUserId()
                .flatMap(userId -> userRepository.findById(userId)
                .flatMap(user -> calculateTotal()
                .map(total -> {
                    if (user.getBalance() >= total) {
                        return "Достаточно средств для оплаты";
                    } else {
                        double needAmount = (total - user.getBalance()) / 100.0;
                        return String.format("Недостаточно средств. Нужно ещё %.2f RUB", needAmount);
                    }
                })))
                .defaultIfEmpty("Корзина пуста");
    }

    @Transactional
    public Mono<Long> createOrderWithPayment() {
        return getCurrentUserId()
                .flatMap(userId -> userRepository.findById(userId)
                .flatMap(user -> getCartItems()
                .collectList()
                .flatMap(items -> {
                    if (items.isEmpty()) {
                        return Mono.error(new IllegalArgumentException("Cart is empty"));
                    }

                    Map<Long, Integer> cartMap = items.stream()
                            .collect(Collectors.toMap(ItemDto::getId, ItemDto::getCount));

                    Long total = items.stream()
                            .mapToLong(item -> item.getPrice() * item.getCount())
                            .sum();

                    if (user.getBalance() < total) {
                        return Mono.error(new RuntimeException("Insufficient funds"));
                    }

                    return orderService.createOrder(userId, cartMap)
                            .flatMap(orderId -> {
                                PaymentRequestDto paymentRequest = new PaymentRequestDto(
                                        orderId,
                                        total,
                                        "Оплата заказа №" + orderId
                                );

                                return paymentServiceClient.processPayment(paymentRequest)
                                        .flatMap(response -> {
                                            if (response.success()) {
                                                user.setBalance(response.newBalance());
                                                return userRepository.save(user)
                                                        .then(clearCart())
                                                        .then(Mono.just(orderId));
                                            } else {
                                                return orderService.deleteOrder(orderId)
                                                        .then(Mono.error(new RuntimeException(
                                                                "Payment failed: " + response.message())));
                                            }
                                        });
                            });
                })));
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
}
