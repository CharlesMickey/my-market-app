package ru.art.home.market.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.art.home.market.dto.ActionRequest;
import ru.art.home.market.dto.CartUpdateRequest;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.dto.PagingDto;
import ru.art.home.market.exception.BadRequestException;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.ItemCacheService;

@Controller
@RequestMapping("/items")
@RequiredArgsConstructor
@Slf4j
public class ItemController {

    private final ItemCacheService itemCacheService;
    private final CartService cartService;

    @GetMapping({"", "/"})
    public Mono<String> getItems(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "NO") String sort,
            @RequestParam(defaultValue = "1") int pageNumber,
            @RequestParam(defaultValue = "5") int pageSize,
            Model model) {

        Mono<Boolean> isAuthenticated = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> !ctx.getAuthentication().getPrincipal().equals("anonymousUser"))
                .defaultIfEmpty(false);

        return isAuthenticated.flatMap(auth -> {
            model.addAttribute("authenticated", auth);

            Mono<Map<Long, Integer>> cartItemsMono;
            if (auth) {
                cartItemsMono = cartService.getCartItems()
                        .collectMap(ItemDto::getId, ItemDto::getCount)
                        .defaultIfEmpty(new HashMap<>());
            } else {
                cartItemsMono = Mono.just(new HashMap<>());
            }

            return cartItemsMono.flatMapMany(cartItems
                    -> itemCacheService.getItemsWithCart(search, sort, pageNumber, pageSize, cartItems)
            )
                    .collectList()
                    .map(items -> {
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
        });
    }

    @PostMapping
    public Mono<String> updateCartItem(
            @ModelAttribute CartUpdateRequest request) {

        if (request.getId() == null || request.getAction() == null) {
            return Mono.error(new BadRequestException("Missing required parameters: id and action"));
        }

        return cartService.updateCart(request.getId(), request.getAction())
                .then(Mono.just(buildRedirectUrl(
                        request.getSearch(),
                        request.getSort(),
                        request.getPageNumber(),
                        request.getPageSize()
                )));
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

    @GetMapping("/{id}")
    public Mono<String> getItem(@PathVariable Long id, Model model) {
        Mono<Boolean> isAuthenticated = ReactiveSecurityContextHolder.getContext()
                .map(ctx -> !ctx.getAuthentication().getPrincipal().equals("anonymousUser"))
                .defaultIfEmpty(false);

        return isAuthenticated.flatMap(auth -> {
            model.addAttribute("authenticated", auth);

            return cartService.getCartItems()
                    .collectMap(ItemDto::getId, ItemDto::getCount)
                    .defaultIfEmpty(java.util.Collections.emptyMap())
                    .flatMap(cartItems -> itemCacheService.getItemById(id, cartItems)
                    .map(item -> {
                        model.addAttribute("item", item);
                        return "item";
                    }));
        });
    }

    @PostMapping("/{id}")
    public Mono<String> updateItemInCart(
            @PathVariable Long id,
            @ModelAttribute ActionRequest request) {

        if (request.getAction() == null || request.getAction().trim().isEmpty()) {
            return Mono.error(new BadRequestException("Missing required parameter: action"));
        }

        return cartService.updateCart(id, request.getAction())
                .then(Mono.just("redirect:/items/" + id));
    }
}
