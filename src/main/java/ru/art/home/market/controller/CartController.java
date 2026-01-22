package ru.art.home.market.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.services.CartService;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping("/items")
    public String getCart(HttpSession session, Model model) {
        Map<Long, Integer> cartItems = getCartItems(session);
        List<ItemDto> items = cartService.getCartItems(cartItems);
        long total = cartService.calculateTotal(items);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    @PostMapping("/items")
    public String updateCartItem(
            @RequestParam Long id,
            @RequestParam String action,
            HttpSession session,
            Model model) {

        Map<Long, Integer> cartItems = getCartItems(session);
        cartItems = cartService.updateCart(cartItems, id, action);
        session.setAttribute("cart", cartItems);

        List<ItemDto> items = cartService.getCartItems(cartItems);
        long total = cartService.calculateTotal(items);

        model.addAttribute("items", items);
        model.addAttribute("total", total);

        return "cart";
    }

    private Map<Long, Integer> getCartItems(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cartItems = (Map<Long, Integer>) session.getAttribute("cart");
        return cartItems != null ? cartItems : new HashMap<>();
    }
}
