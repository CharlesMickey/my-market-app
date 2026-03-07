package ru.art.home.market.configs;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.logout.DelegatingServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.SecurityContextServerLogoutHandler;
import org.springframework.security.web.server.authentication.logout.WebSessionServerLogoutHandler;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebFluxSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {

        DelegatingServerLogoutHandler logoutHandler = new DelegatingServerLogoutHandler(
                new SecurityContextServerLogoutHandler(),
                new WebSessionServerLogoutHandler()
        );

        return http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(headers -> headers
                .frameOptions(ServerHttpSecurity.HeaderSpec.FrameOptionsSpec::disable)
                )
                .authorizeExchange(exchanges -> exchanges
                .pathMatchers("/css/**", "/js/**", "/images/**", "/webjars/**").permitAll()
                .pathMatchers("/", "/items", "/items/**").permitAll()
                .pathMatchers("/test-password", "/check-password", "/h2-console/**").permitAll()
                .pathMatchers("/cart/**", "/orders/**").authenticated()
                .anyExchange().authenticated()
                )
                .formLogin(formLogin -> formLogin
                .authenticationSuccessHandler((webFilterExchange, authentication) -> {
                    webFilterExchange.getExchange().getResponse()
                            .setStatusCode(HttpStatus.FOUND);
                    webFilterExchange.getExchange().getResponse()
                            .getHeaders()
                            .setLocation(URI.create("/items"));
                    return webFilterExchange.getExchange().getResponse().setComplete();
                })
                )
                .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutHandler(logoutHandler)
                .logoutSuccessHandler((exchange, authentication) -> {
                    exchange.getExchange().getResponse()
                            .setStatusCode(HttpStatus.FOUND);
                    exchange.getExchange().getResponse()
                            .getHeaders()
                            .setLocation(URI.create("/items"));
                    return exchange.getExchange().getResponse().setComplete();
                })
                )
                .build();
    }
}
