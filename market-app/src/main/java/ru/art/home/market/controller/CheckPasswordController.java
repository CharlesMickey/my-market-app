package ru.art.home.market.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.art.home.market.repositoryes.UserRepository;

@RestController
@RequiredArgsConstructor
@Slf4j
public class CheckPasswordController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/check-password")
    public Mono<String> checkPassword(
            @RequestParam String username,
            @RequestParam String password) {

        log.info("Checking password for user: {}", username);

        return userRepository.findByUsername(username)
                .map(user -> {
                    boolean matches = passwordEncoder.matches(password, user.getPassword());
                    String result = String.format(
                            "User: %s<br/>Password matches: %s<br/>Stored hash: %s<br/>Balance: %d",
                            username, matches, user.getPassword(), user.getBalance());
                    log.info("Result: {}", result);
                    return result;
                })
                .defaultIfEmpty("User not found: " + username);
    }
}
