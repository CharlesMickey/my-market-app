package ru.art.home.market.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.WebSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.ActionRequest;
import ru.art.home.market.dto.CartUpdateRequest;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.dto.PagingDto;
import ru.art.home.market.exception.BadRequestException;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.ItemCacheService;
import ru.art.home.market.services.ItemService;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemService itemService;
    private final CartService cartService;
    private final ItemCacheService itemCacheService;

    @GetMapping({"", "/"})
    public Mono<String> getItems(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            WebSession session,
            Model model) {

        Map<Long, Integer> cartItems = getCartItems(session);

        Flux<ItemDto> itemsFlux = itemCacheService.getItems(search, cartItems, sort, pageNumber, pageSize);

        return itemsFlux.collectList().map(items -> {
            List<List<ItemDto>> groupedItems = cartService.groupItemsForDisplay(items);

            PagingDto paging = new PagingDto();
            paging.setPageSize(pageSize);
            paging.setPageNumber(pageNumber);
            paging.setHasPrevious(pageNumber > 1);

            paging.setHasNext(items.size() >= pageSize);

            model.addAttribute("items", groupedItems);
            model.addAttribute("search", search);
            model.addAttribute("sort", sort);
            model.addAttribute("paging", paging);

            return "items";
        });
    }

    @PostMapping
    public Mono<String> updateCartItem(
            @ModelAttribute CartUpdateRequest request,
            WebSession session) {

        if (request.getId() == null || request.getAction() == null) {
            return Mono.error(new BadRequestException("Missing required parameters: id and action"));
        }

        Map<Long, Integer> cartItems = getCartItems(session);
        Map<Long, Integer> updatedCart = cartService.updateCart(cartItems, request.getId(), request.getAction());

        session.getAttributes().put("cart", updatedCart);

        String redirectUrl = buildRedirectUrl(
                request.getSearch(),
                request.getSort(),
                request.getPageNumber(),
                request.getPageSize()
        );
        return Mono.just(redirectUrl);
    }

    private String buildRedirectUrl(String search, String sort, Integer pageNumber, Integer pageSize) {
        StringBuilder url = new StringBuilder("redirect:/items?");

        if (search != null && !search.trim().isEmpty()) {
            url.append("search=").append(search).append("&");
        }

        url.append("sort=").append(sort != null ? sort : "NO")
                .append("&pageNumber=").append(pageNumber != null ? pageNumber : 1)
                .append("&pageSize=").append(pageSize != null ? pageSize : 5);

        return url.toString();
    }

    private String buildRedirectUrl(String search, String sort, int pageNumber, int pageSize) {
        StringBuilder url = new StringBuilder("redirect:/items?");

        if (search != null && !search.trim().isEmpty()) {
            url.append("search=").append(search).append("&");
        }

        url.append("sort=").append(sort)
                .append("&pageNumber=").append(pageNumber)
                .append("&pageSize=").append(pageSize);

        return url.toString();
    }

    @GetMapping("/{id}")
    public Mono<String> getItem(@PathVariable Long id, WebSession session, Model model) {
        Map<Long, Integer> cartItems = getCartItems(session);
        return itemCacheService.getItemById(id, cartItems)
                .map(item -> {
                    model.addAttribute("item", item);
                    return "item";
                });
    }

    @PostMapping("/{id}")
    public Mono<String> updateItemInCart(
            @PathVariable Long id,
            @ModelAttribute ActionRequest request,
            WebSession session,
            Model model) {

        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            return Mono.error(new BadRequestException("Missing required parameter: action"));
        }
        Map<Long, Integer> cartItems = getCartItems(session);
        Map<Long, Integer> updatedCart
                = cartService.updateCart(cartItems, id, request.getAction());

        session.getAttributes().put("cart", updatedCart);

        return itemService.getItemById(id, updatedCart)
                .map(item -> {
                    model.addAttribute("item", item);
                    return "item";
                });
    }

    private Map<Long, Integer> getCartItems(WebSession session) {
        Map<Long, Integer> cartItems = session.getAttribute("cart");
        return cartItems != null ? cartItems : new HashMap<>();
    }
}
