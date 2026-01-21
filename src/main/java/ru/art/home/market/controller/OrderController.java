package ru.art.home.market.controller;

import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import ru.art.home.market.dto.OrderDto;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.OrderService;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final CartService cartService;

    @GetMapping
    public String getAllOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "orders";
    }

    @GetMapping("/{id}")
    public String getOrder(@PathVariable Long id,
            @RequestParam(required = false, defaultValue = "false") boolean newOrder,
            Model model) {
        OrderDto order = orderService.getOrderById(id);
        model.addAttribute("order", order);
        model.addAttribute("newOrder", newOrder);
        return "order";
    }

    @PostMapping("/buy")
    public String buyItems(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cartItems = (Map<Long, Integer>) session.getAttribute("cart");

        if (cartItems == null || cartItems.isEmpty()) {
            return "redirect:/cart/items";
        }

        Long orderId = cartService.createOrder(cartItems);
        session.removeAttribute("cart");

        return "redirect:/orders/" + orderId + "?newOrder=true";
    }
}
