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
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.CartItemUpdateRequest;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.services.CartService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public Mono<String> getCart(WebSession session, Model model) {
        Map<Long, Integer> cartItems = getCartItems(session);

        Flux<ItemDto> itemsFlux = cartService.getCartItems(cartItems);
        Mono<Long> totalMono = cartService.calculateTotal(itemsFlux);

        model.addAttribute("items", itemsFlux);
        model.addAttribute("total", totalMono);

        return Mono.just("cart");
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
        Map<Long, Integer> updatedCart
                = cartService.updateCart(cartItems, request.getId(), request.getAction());

        session.getAttributes().put("cart", updatedCart);

        Flux<ItemDto> itemsFlux = cartService.getCartItems(updatedCart);
        Mono<Long> totalMono = cartService.calculateTotal(itemsFlux);

        model.addAttribute("items", itemsFlux);
        model.addAttribute("total", totalMono);

        return Mono.just("cart");
    }

    private Map<Long, Integer> getCartItems(WebSession session) {
        Map<Long, Integer> cartItems = session.getAttribute("cart");
        return cartItems != null ? cartItems : new HashMap<>();
    }
}
