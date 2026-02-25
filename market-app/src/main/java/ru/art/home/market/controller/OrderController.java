package ru.art.home.market.controller;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.WebSession;
import org.springframework.web.util.UriUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.OrderService;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping
    public Mono<String> getAllOrders(Model model) {
        return orderService.getAllOrders()
                .collectList()
                .map(list -> {
                    model.addAttribute("orders", list);
                    return "orders";
                });
    }

    @GetMapping("/{id}")
    public Mono<String> getOrder(@PathVariable Long id,
                                 @RequestParam(required = false, defaultValue = "false") boolean newOrder,
                                 Model model) {
        return orderService.getOrderById(id)
                .map(order -> {
                    model.addAttribute("order", order);
                    model.addAttribute("newOrder", newOrder);
                    return "order";
                });
    }

    @PostMapping("/buy")
    public Mono<String> buyItems(WebSession session) {
        Map<Long, Integer> cartItems = session.getAttribute("cart");

        if (cartItems == null || cartItems.isEmpty()) {
            return Mono.just("redirect:/cart/items");
        }

        log.info("Processing buy request for cart with {} items", cartItems.size());

        return cartService.createOrderWithPayment(cartItems)
                .doOnNext(orderId -> {
                    log.info("Order {} created and paid successfully", orderId);
                    session.getAttributes().remove("cart");
                })
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true")
                .onErrorResume(error -> {
                    log.error("Failed to create order with payment", error);

                    String errorMessage;
                    if (error.getMessage() != null && error.getMessage().contains("Payment failed")) {
                        errorMessage = "Оплата не прошла. Недостаточно средств на счете.";
                    } else if (error.getMessage() != null && error.getMessage().contains("unavailable")) {
                        errorMessage = "Сервис платежей временно недоступен. Попробуйте позже.";
                    } else {
                        errorMessage = "Ошибка при оформлении заказа";
                    }

                    String encodedError = UriUtils.encode(errorMessage, StandardCharsets.UTF_8);
                    return Mono.just("redirect:/cart/items?error=" + encodedError);
                });
    }
}
