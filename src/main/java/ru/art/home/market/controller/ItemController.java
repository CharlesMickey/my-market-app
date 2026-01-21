package ru.art.home.market.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.dto.PagingDto;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.ItemService;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;

    @GetMapping({"", "/"})
    public String getItems(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            HttpSession session,
            Model model) {

        Map<Long, Integer> cartItems = getCartItems(session);
        Page<ItemDto> itemsPage = itemService.getItems(search, sort, pageNumber, pageSize, cartItems);

        List<ItemDto> items = itemsPage.getContent();
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

        PagingDto paging = new PagingDto();
        paging.setPageSize(pageSize);
        paging.setPageNumber(pageNumber);
        paging.setHasPrevious(itemsPage.hasPrevious());
        paging.setHasNext(itemsPage.hasNext());

        model.addAttribute("items", groupedItems);
        model.addAttribute("search", search);
        model.addAttribute("sort", sort);
        model.addAttribute("paging", paging);

        return "items";
    }

    @PostMapping
    public String updateCartItem(
            @RequestParam Long id,
            @RequestParam String action,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            HttpSession session) {

        Map<Long, Integer> cartItems = getCartItems(session);
        cartItems = cartService.updateCart(cartItems, id, action);
        session.setAttribute("cart", cartItems);

        return String.format("redirect:/items?search=%s&sort=%s&pageNumber=%d&pageSize=%d",
                search != null ? search : "",
                sort,
                pageNumber,
                pageSize);
    }

    @GetMapping("/{id}")
    public String getItem(@PathVariable Long id, HttpSession session, Model model) {
        Map<Long, Integer> cartItems = getCartItems(session);
        ItemDto item = itemService.getItemById(id, cartItems);
        model.addAttribute("item", item);
        return "item";
    }

    @PostMapping("/{id}")
    public String updateItemInCart(
            @PathVariable Long id,
            @RequestParam String action,
            HttpSession session,
            Model model) {

        Map<Long, Integer> cartItems = getCartItems(session);
        cartItems = cartService.updateCart(cartItems, id, action);
        session.setAttribute("cart", cartItems);

        ItemDto item = itemService.getItemById(id, cartItems);
        model.addAttribute("item", item);
        return "item";
    }

    private Map<Long, Integer> getCartItems(HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cartItems = (Map<Long, Integer>) session.getAttribute("cart");
        return cartItems != null ? cartItems : new HashMap<>();
    }
}
