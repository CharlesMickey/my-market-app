package ru.art.home.market.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.WebSession;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.OrderService;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
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

        return cartService.createOrder(cartItems)
                .doOnNext(orderId -> session.getAttributes().remove("cart"))
                .map(orderId -> "redirect:/orders/" + orderId + "?newOrder=true");
    }
}
