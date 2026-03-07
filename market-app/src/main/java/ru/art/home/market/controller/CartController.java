package ru.art.home.market.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriUtils;

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
    public Mono<String> getCart(Model model, @RequestParam(required = false) String error) {
        return cartService.getCartItems()
                .collectList()
                .flatMap(items -> {
                    model.addAttribute("items", items);
                    model.addAttribute("groupedItems", cartService.groupItemsForDisplay(items));

                    return cartService.calculateTotal()
                            .flatMap(total -> {
                                model.addAttribute("total", total);

                                return cartService.getPaymentStatusMessage()
                                        .doOnNext(message -> {
                                            model.addAttribute("paymentStatus", message);
                                            model.addAttribute("canPay", message.equals("Достаточно средств для оплаты"));
                                            if (error != null) {
                                                model.addAttribute("paymentError", UriUtils.decode(error, StandardCharsets.UTF_8));
                                            }
                                        })
                                        .thenReturn("cart");
                            });
                })
                .switchIfEmpty(Mono.just("cart"));
    }

    @PostMapping("/items")
    public Mono<String> updateCartItem(@ModelAttribute CartItemUpdateRequest request) {
        if (request.getId() == null || request.getAction() == null) {
            return Mono.error(new IllegalArgumentException("Missing required parameters"));
        }

        return cartService.updateCart(request.getId(), request.getAction())
                .then(Mono.just("redirect:/cart/items"));
    }

    @PostMapping("/clear")
    public Mono<String> clearCart() {
        return cartService.clearCart()
                .then(Mono.just("redirect:/cart/items"));
    }
}
