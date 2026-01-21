package ru.art.home.market.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.art.home.market.controller.ItemController;
import ru.art.home.market.dto.ItemDto;
import ru.art.home.market.services.CartService;
import ru.art.home.market.services.ItemService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@WebMvcTest(ItemController.class)
class ItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private CartService cartService;

    @Test
    void getItems_returnsOk() throws Exception {
        Page<ItemDto> emptyPage = Page.empty();

        when(itemService.getItems(
                any(), any(), anyInt(), anyInt(), anyMap()
        )).thenReturn(emptyPage);

        mockMvc.perform(get("/items"))
                .andExpect(status().isOk());
    }
}
