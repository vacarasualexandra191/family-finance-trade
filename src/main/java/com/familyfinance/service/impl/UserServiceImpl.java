package com.familyfinance.service.impl;

import com.familyfinance.dto.RegistrationForm;
import com.familyfinance.entity.Role;
import com.familyfinance.entity.User;
import com.familyfinance.entity.VerificationToken;
import com.familyfinance.repository.RoleRepository;
import com.familyfinance.repository.UserRepository;
import com.familyfinance.repository.VerificationTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository verificationTokenRepository;
    private final com.familyfinance.service.impl.EmailService emailService;

    @Transactional
    public User register(RegistrationForm form) {
        if (userRepository.existsByUsername(form.getUsername())) {
            throw new RuntimeException("Username-ul '" + form.getUsername() + "' este deja folosit");
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            throw new RuntimeException("Adresa de email este deja folosita");
        }

        Role userRole = roleRepository.findByName("ROLE_USER")
                .orElseThrow(() -> new RuntimeException("ROLE_USER nu a fost gasit"));


        User user = User.builder()
                .username(form.getUsername())
                .password(passwordEncoder.encode(form.getPassword()))
                .fullName(form.getFullName())
                .email(form.getEmail())
                .familyRole("PARINTE")
                .roles(Set.of(userRole))
                .build();

        User saved = userRepository.save(user);

        String token = UUID.randomUUID().toString();
        verificationTokenRepository.save(VerificationToken.builder()
                .token(token)
                .user(saved)
                .tokenType(VerificationToken.TokenType.EMAIL_VERIFICATION)
                .expiresAt(LocalDateTime.now().plusHours(24))
                .build());

        String verificationLink = "http://localhost:8082/verify-email?token=" + token;
        emailService.sendVerificationEmail(saved.getEmail(), saved.getUsername(), verificationLink);

        log.info("Utilizator nou inregistrat: username={}, email confirmare trimis la {}",
                saved.getUsername(), saved.getEmail());
        return saved;
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilizatorul '" + username + "' nu a fost gasit"));
    }
}