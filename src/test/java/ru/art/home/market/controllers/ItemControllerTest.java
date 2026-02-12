package ru.art.home.market.controllers;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import ru.art.home.market.controller.ItemController;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.ItemService;

@WebFluxTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    private ItemDto dto(Long id, String title) {
        ItemDto dto = new ItemDto();
        dto.setId(id);
        dto.setTitle(title);
        dto.setDescription("desc");
        dto.setPrice(100L);
        dto.setImgPath("img.png");
        dto.setCount(0);
        return dto;
    }

    @Test
    void getItems_shouldRenderHtml() {

        ItemDto item = dto(1L, "Apple");

        when(itemService.getItems(
                isNull(),
                anyMap(),
                eq("NO"),
                eq(1),
                eq(5)
        )).thenReturn(Flux.just(item));

        when(cartService.groupItemsForDisplay(anyList()))
                .thenReturn(List.of(List.of(item)));

        webTestClient.get()
                .uri("/items")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Apple");
                });

        verify(itemService).getItems(isNull(), anyMap(), eq("NO"), eq(1), eq(5));
        verify(cartService).groupItemsForDisplay(anyList());
    }

    @Test
    void getItem_shouldRenderSingleItemPage() {

        ItemDto item = dto(1L, "Phone");

        when(itemService.getItemById(eq(1L), anyMap()))
                .thenReturn(Mono.just(item));

        webTestClient.get()
                .uri("/items/1")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Phone");
                });

        verify(itemService).getItemById(eq(1L), anyMap());
    }

    @Test
    void updateCartItem_shouldRedirect() {

        when(cartService.updateCart(anyMap(), eq(1L), eq("ADD")))
                .thenReturn(Map.of(1L, 1));

        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                .path("/items")
                .queryParam("id", 1L)
                .queryParam("action", "ADD")
                .build())
                .exchange()
                .expectStatus().is3xxRedirection()
                .expectHeader().valueMatches("Location", ".*\\/items.*");

        verify(cartService).updateCart(anyMap(), eq(1L), eq("ADD"));
    }

    @Test
    void updateItemInCart_shouldRenderUpdatedItem() {

        ItemDto updatedItem = dto(1L, "Phone");
        updatedItem.setCount(2);

        when(cartService.updateCart(anyMap(), eq(1L), eq("ADD")))
                .thenReturn(Map.of(1L, 2));

        when(itemService.getItemById(eq(1L), anyMap()))
                .thenReturn(Mono.just(updatedItem));

        webTestClient.post()
                .uri("/items/1")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue("action=ADD")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_HTML)
                .expectBody(String.class)
                .value(html -> {
                    assert html.contains("Phone");
                });

        verify(cartService).updateCart(anyMap(), eq(1L), eq("ADD"));
        verify(itemService).getItemById(eq(1L), anyMap());
    }

}
