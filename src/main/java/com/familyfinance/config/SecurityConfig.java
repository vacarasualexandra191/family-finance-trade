package com.familyfinance.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import javax.sql.DataSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public JdbcUserDetailsManager userDetailsManager(DataSource dataSource) {
        JdbcUserDetailsManager manager = new JdbcUserDetailsManager(dataSource);
        manager.setUsersByUsernameQuery(
                "SELECT username, password, 1 as enabled FROM ff_users WHERE username = ?"
        );
        manager.setAuthoritiesByUsernameQuery(
                "SELECT u.username, r.name FROM ff_users u " +
                        "JOIN ff_user_roles ur ON ur.user_id = u.id " +
                        "JOIN ff_roles r ON r.id = ur.role_id " +
                        "WHERE u.username = ?"
        );
        return manager;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers("/h2-console/**")
                )  // temporar dezactivat pentru debug
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/css/**", "/js/**", "/register", "/login",
                                "/forgot-password", "/reset-password", "/verify-email",
                                "/error", "/error/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/investments/funds/new",
                                "/investments/funds/*/refresh",
                                "/investments/funds/*/delete",
                                "/investments/funds/*/edit").hasRole("ADMIN")
                        .requestMatchers("/exchange/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .failureUrl("/login?error=true")
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout=true")
                        .permitAll()
                )
                .rememberMe(remember -> remember
                        .key("family-finance-remember-me")
                        .tokenValiditySeconds(7 * 24 * 60 * 60)
                )
                .exceptionHandling(ex -> ex
                        .accessDeniedPage("/error/403")
                );
        return http.build();
    }
}