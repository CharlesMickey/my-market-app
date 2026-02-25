package ru.art.home.market.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.WebSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.CartItemUpdateRequest;
import ru.art.home.market.services.CartService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public Mono<String> getCart(WebSession session, Model model) {
        Map<Long, Integer> cartItems = getCartItems(session);

        return cartService.getCartItems(cartItems)
                .collectList()
                .flatMap(items -> {
                    model.addAttribute("items", items);

                    return cartService.calculateTotal(cartService.getCartItems(cartItems))
                            .flatMap(total -> {
                                model.addAttribute("total", total);
                                return cartService.getPaymentStatusMessage(cartItems)
                                        .doOnNext(message -> {
                                            model.addAttribute("paymentStatus", message);
                                            model.addAttribute("canPay",
                                                    message.equals("Достаточно средств для оплаты"));
                                        })
                                        .thenReturn("cart");
                            });
                })
                .switchIfEmpty(Mono.just("cart"));
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(
            @ModelAttribute CartItemUpdateRequest request,
            WebSession session,
            Model model) {

        if (request.getId() == null || request.getAction() == null) {
            return Mono.error(new IllegalArgumentException("Missing required parameters"));
        }

        Map<Long, Integer> cartItems = getCartItems(session);
        Map<Long, Integer> updatedCart = cartService.updateCart(cartItems, request.getId(), request.getAction());

        session.getAttributes().put("cart", updatedCart);

        return cartService.getCartItems(updatedCart)
                .collectList()
                .flatMap(items -> {
                    model.addAttribute("items", items);

                    return cartService.calculateTotal(cartService.getCartItems(updatedCart))
                            .flatMap(total -> {
                                model.addAttribute("total", total);

                                return cartService.getPaymentStatusMessage(updatedCart)
                                        .doOnNext(message -> {
                                            model.addAttribute("paymentStatus", message);
                                            model.addAttribute("canPay",
                                                    message.equals("Достаточно средств для оплаты"));
                                        })
                                        .thenReturn("cart");
                            });
                });
    }

    private Map<Long, Integer> getCartItems(WebSession session) {
        Map<Long, Integer> cartItems = session.getAttribute("cart");
        return cartItems != null ? cartItems : new HashMap<>();
    }
}
