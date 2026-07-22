package com.arteymetal.ArteyMetal.security;

import com.arteymetal.ArteyMetal.entity.Usuario;
import com.arteymetal.ArteyMetal.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Autowired
    private UsuarioRepository usuarioRepository;

    private final Map<String, LoginAttempt> loginAttempts = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 6;
    private static final long BLOCK_DURATION_MS = 60_000;

    private static class LoginAttempt {
        final AtomicInteger count = new AtomicInteger(0);
        long lastAttemptTime = System.currentTimeMillis();

        void increment() {
            count.incrementAndGet();
            lastAttemptTime = System.currentTimeMillis();
        }

        boolean isBlocked() {
            if (System.currentTimeMillis() - lastAttemptTime > BLOCK_DURATION_MS) {
                count.set(0);
                return false;
            }
            return count.get() >= MAX_ATTEMPTS;
        }

        void reset() {
            count.set(0);
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService() {
        return login -> {
            Usuario usuario;
            if (login.contains("@")) {
                usuario = usuarioRepository.findByEmail(login)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + login));
            } else {
                usuario = usuarioRepository.findByName(login)
                    .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado: " + login));
            }
            return usuario;
        };
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public AuthenticationFailureHandler authenticationFailureHandler() {
        return new SimpleUrlAuthenticationFailureHandler() {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                                org.springframework.security.core.AuthenticationException exception) throws IOException, jakarta.servlet.ServletException {
                String login = request.getParameter("login");
                if (login != null && !login.isEmpty()) {
                    LoginAttempt attempt = loginAttempts.computeIfAbsent(login.toLowerCase(), k -> new LoginAttempt());
                    attempt.increment();
                }
                super.onAuthenticationFailure(request, response, exception);
            }
        };
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/css/**", "/js/**", "/icons/**", "/images/**", "/img/**", "/favicon.ico", "/favicon.png").permitAll()
                .requestMatchers("/").permitAll()
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("login")
                .passwordParameter("password")
                .defaultSuccessUrl("/dashboard", true)
                .failureHandler(authenticationFailureHandler())
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .accessDeniedPage("/acceso-denegado")
            );

        return http.build();
    }
}
