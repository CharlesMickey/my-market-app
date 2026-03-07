package ru.art.home.market.services;

import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import ru.art.home.market.repositoryes.AuthorityRepository;
import ru.art.home.market.repositoryes.UserRepository;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReactiveUserDetailsServiceImpl implements ReactiveUserDetailsService {

    private final UserRepository userRepository;
    private final AuthorityRepository authorityRepository;

    @Override
    public Mono<UserDetails> findByUsername(String username) {
        log.debug("Loading user by username: {}", username);

        return userRepository.findByUsername(username)
                .flatMap(user -> {
                    log.debug("Found user: {}, enabled: {}", user.getUsername(), user.isEnabled());

                    return authorityRepository.findByUsername(username)
                            .map(authority -> {
                                log.debug("Found authority: {} for user: {}", authority.getAuthority(), username);
                                return authority.getAuthority();
                            })
                            .collectList()
                            .map(authorities -> {
                                log.debug("User {} has authorities: {}", username, authorities);

                                return User.builder()
                                        .username(user.getUsername())
                                        .password(user.getPassword())
                                        .authorities(authorities.toArray(new String[0]))
                                        .disabled(!user.isEnabled())
                                        .build();
                            });
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("User not found: {}", username);
                    return Mono.empty();
                }));
    }
}
