package com.familyfinance.controller;

import com.familyfinance.dto.RegistrationForm;
import com.familyfinance.entity.VerificationToken;
import com.familyfinance.repository.RoleRepository;
import com.familyfinance.repository.UserRepository;
import com.familyfinance.repository.VerificationTokenRepository;
import com.familyfinance.service.impl.EmailService;
import com.familyfinance.service.impl.UserServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDateTime;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final EmailService emailService;
    private final UserServiceImpl userService;

    // ============ LOGIN ============

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
                            @RequestParam(required = false) String logout,
                            Model model) {
        if (error != null) model.addAttribute("errorMessage", "Username sau parola incorecte.");
        if (logout != null) model.addAttribute("logoutMessage", "Ai fost deconectat.");
        return "auth/login";
    }

    // ============ REGISTER ============

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("form", new RegistrationForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") RegistrationForm form,
                           BindingResult result, Model model,
                           HttpServletRequest request,
                           RedirectAttributes ra) {
        if (result.hasErrors()) return "auth/register";

        if (userRepository.existsByUsername(form.getUsername())) {
            model.addAttribute("errorMessage", "Username-ul este deja folosit.");
            return "auth/register";
        }
        if (userRepository.existsByEmail(form.getEmail())) {
            model.addAttribute("errorMessage", "Email-ul este deja folosit.");
            return "auth/register";
        }

        try {
            userService.register(form);
            ra.addFlashAttribute("successMessage",
                    "Cont creat! Verifica email-ul pentru a confirma contul.");
        } catch (Exception ex) {
            model.addAttribute("errorMessage", ex.getMessage());
            return "auth/register";
        }

        return "redirect:/login";
    }

    // ============ VERIFICARE EMAIL ============

    @GetMapping("/verify-email")
    public String verifyEmail(@RequestParam String token, Model model) {
        var opt = tokenRepository.findByTokenAndTokenType(
                token, VerificationToken.TokenType.EMAIL_VERIFICATION);

        if (opt.isEmpty() || opt.get().isExpired()) {
            model.addAttribute("error", "Link invalid sau expirat.");
            return "auth/verification-result";
        }
        if (opt.get().isUsed()) {
            model.addAttribute("info", "Contul a fost deja confirmat.");
            return "auth/verification-result";
        }

        var vt = opt.get();
        vt.setUsed(true);
        userRepository.save(vt.getUser());
        tokenRepository.save(vt);

        model.addAttribute("success", "Cont confirmat! Te poti loga acum.");
        return "auth/verification-result";
    }

    // ============ RESETARE PAROLA ============

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @Transactional
    @PostMapping("/forgot-password")
    public String forgotPassword(@RequestParam String email,
                                 HttpServletRequest request,
                                 RedirectAttributes ra) {
        log.info("Forgot password request pentru email: {}", email);

        var userOpt = userRepository.findByEmail(email);
        log.info("User gasit: {}", userOpt.isPresent());

        if (userOpt.isEmpty()) {
            log.warn("Emailul {} nu exista in baza de date", email);
            ra.addFlashAttribute("successMessage",
                    "Daca email-ul exista, vei primi un link de resetare.");
            return "redirect:/forgot-password";
        }

        var user = userOpt.get();
        log.info("Trimit email de resetare pentru: {}", user.getUsername());

        try {
            tokenRepository.deleteByUserId(user.getId());

            String token = UUID.randomUUID().toString();
            tokenRepository.save(VerificationToken.builder()
                    .token(token).user(user)
                    .tokenType(VerificationToken.TokenType.PASSWORD_RESET)
                    .expiresAt(LocalDateTime.now().plusHours(24))
                    .build());

            String link = request.getScheme() + "://" + request.getServerName() +
                    ":" + request.getServerPort() + "/reset-password?token=" + token;

            emailService.sendPasswordResetEmail(user.getEmail(), user.getUsername(), link);
            log.info("Email trimis cu succes catre: {}", user.getEmail());

        } catch (Exception ex) {
            log.error("Eroare la trimiterea emailului: {}", ex.getMessage(), ex);
        }

        ra.addFlashAttribute("successMessage",
                "Daca email-ul exista, vei primi un link de resetare.");
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam String token, Model model) {
        var opt = tokenRepository.findByTokenAndTokenType(
                token, VerificationToken.TokenType.PASSWORD_RESET);
        if (opt.isEmpty() || opt.get().isExpired() || opt.get().isUsed()) {
            model.addAttribute("error", "Link invalid sau expirat.");
            return "auth/reset-password";
        }
        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String resetPassword(@RequestParam String token,
                                @RequestParam String newPassword,
                                RedirectAttributes ra) {
        var opt = tokenRepository.findByTokenAndTokenType(
                token, VerificationToken.TokenType.PASSWORD_RESET);
        if (opt.isEmpty() || opt.get().isExpired() || opt.get().isUsed()) {
            ra.addFlashAttribute("errorMessage", "Link invalid sau expirat.");
            return "redirect:/forgot-password";
        }
        var vt = opt.get();
        vt.getUser().setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(vt.getUser());
        vt.setUsed(true);
        tokenRepository.save(vt);
        ra.addFlashAttribute("successMessage", "Parola resetata! Te poti loga.");
        return "redirect:/login";
    }

    private Long getCurrentUserId() {
        String username = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByUsername(username)
                .orElseThrow().getId();
    }
}