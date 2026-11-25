package com.chargeslot.config;

import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.http.MediaType;

@Configuration
public class SecurityConfig {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean UserDetailsService users(PasswordEncoder encoder) {
        String aliceHash = encoder.encode("AliceDemo21!");
        String bobHash = encoder.encode("BobDemo21!");
        String operatorHash = encoder.encode("OperateurDemo21!");
        // Instance neuve a chaque authentification : Spring peut effacer ses credentials apres usage.
        return username -> switch (username) {
            case "alice" -> new DemoUser("alice", aliceHash, 17L, "DRIVER");
            case "bob" -> new DemoUser("bob", bobHash, 18L, "DRIVER");
            case "operateur" -> new DemoUser("operateur", operatorHash, 0L, "OPERATOR");
            default -> throw new UsernameNotFoundException("Compte inconnu");
        };
    }
    @Bean SecurityFilterChain security(HttpSecurity http) throws Exception {
        return http.authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index.html", "/app.js", "/style.css", "/favicon.ico", "/api/csrf", "/error").permitAll()
                .anyRequest().authenticated())
            .httpBasic(basic -> basic.authenticationEntryPoint((request, response, error) -> {
                response.setStatus(401); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":\"UNAUTHENTICATED\",\"message\":\"Identifiants requis ou incorrects.\"}");
            }))
            .exceptionHandling(errors -> errors.accessDeniedHandler((request, response, error) -> {
                response.setStatus(403); response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"Acces refuse ou jeton CSRF absent.\"}");
            })).build();
    }
    public static class DemoUser extends User {
        private final Long driverId;
        public DemoUser(String name, String password, Long driverId, String role) {
            super(name, password, User.withUsername(name).password(password).roles(role).build().getAuthorities());
            this.driverId = driverId;
        }
        public Long getDriverId() { return driverId; }
    }
}
