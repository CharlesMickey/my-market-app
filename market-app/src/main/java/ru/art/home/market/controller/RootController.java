package ru.art.home.market.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import reactor.core.publisher.Mono;

@Controller
public class RootController {

    @GetMapping("/")
    public Mono<String> redirectToItems() {
        return Mono.just("redirect:/items");
    }
}
