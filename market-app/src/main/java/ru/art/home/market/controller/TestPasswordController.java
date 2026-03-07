package ru.art.home.market.controller;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import ru.art.home.market.repositoryes.UserRepository;

@RestController
@RequiredArgsConstructor
public class TestPasswordController {

    private final UserRepository userRepository;

    @GetMapping("/test-password")
    public Mono<String> testPassword(@RequestParam String password) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        return userRepository.findByUsername("user1")
                .map(user -> {
                    String storedHash = user.getPassword();
                    boolean matches = encoder.matches(password, storedHash);

                    return String.format(
                            "Test pas: '%s'<br>"
                            + "DB: %s<br>"
                            + "Pas: %s<br>"
                            + "Balance: %d",
                            password, storedHash, matches, user.getBalance());
                })
                .defaultIfEmpty("User not fonud");
    }

    @GetMapping("/check-all-users")
    public Mono<String> checkAllUsers() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        return userRepository.findAll()
                .map(user -> {
                    boolean matches1234 = encoder.matches("1234", user.getPassword());
                    return String.format(
                            "User: %s<br>Hash: %s<br>Matches '1234': %s<br>Balance: %d<br><br>",
                            user.getUsername(), user.getPassword(), matches1234, user.getBalance());
                })
                .collectList()
                .map(list -> String.join("", list));
    }
}
