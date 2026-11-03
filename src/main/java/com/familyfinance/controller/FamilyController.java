package com.familyfinance.controller;

import com.familyfinance.dto.MonthlyReportDto;
import com.familyfinance.entity.Family;
import com.familyfinance.entity.Role;
import com.familyfinance.entity.Transaction;
import com.familyfinance.entity.User;
import com.familyfinance.repository.FamilyRepository;
import com.familyfinance.repository.RoleRepository;
import com.familyfinance.repository.TransactionRepository;
import com.familyfinance.repository.UserRepository;
import com.familyfinance.repository.VerificationTokenRepository;
import com.familyfinance.service.impl.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Set;

@Controller
@RequestMapping("/family")
@RequiredArgsConstructor
@Slf4j
public class FamilyController {

    private final UserRepository userRepository;
    private final FamilyRepository familyRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final VerificationTokenRepository tokenRepository;
    private final TransactionRepository transactionRepository;
    private final ReportService reportService;

    @GetMapping
    public String familyPage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        model.addAttribute("currentUser", currentUser);
        boolean isParinte = "PARINTE".equals(currentUser.getFamilyRole());
        model.addAttribute("isParinte", isParinte);

        // Copilul - vede doar info familie
        if (!isParinte) {
            if (currentUser.getFamily() != null) {
                model.addAttribute("family", currentUser.getFamily());
                model.addAttribute("members", currentUser.getFamily().getMembers());
            } else {
                model.addAttribute("noFamily", true);
            }
            return "family/index";
        }

        // Parinte fara familie
        if (currentUser.getFamily() == null) {
            model.addAttribute("noFamily", true);
            return "family/index";
        }

        // Parinte cu familie
        Family family = currentUser.getFamily();
        model.addAttribute("family", family);
        model.addAttribute("members", family.getMembers());
        return "family/index";
    }

    @PostMapping("/create")
    public String createFamily(@AuthenticationPrincipal UserDetails userDetails,
                               @RequestParam String familyName,
                               RedirectAttributes ra) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        if (currentUser.getFamily() != null) {
            ra.addFlashAttribute("errorMessage", "Ai deja o familie!");
            return "redirect:/family";
        }

        Family family = Family.builder()
                .name(familyName)
                .build();
        family = familyRepository.save(family);

        currentUser.setFamily(family);
        currentUser.setFamilyRole("PARINTE");
        userRepository.save(currentUser);

        log.info("Familie creata: {} de catre {}", familyName, currentUser.getUsername());
        ra.addFlashAttribute("successMessage", "Familia '" + familyName + "' a fost creata!");
        return "redirect:/family";
    }



    @PostMapping("/add-member")
    public String addMember(@AuthenticationPrincipal UserDetails userDetails,
                            @RequestParam String username,
                            @RequestParam String fullName,
                            @RequestParam String email,
                            @RequestParam(defaultValue = "COPIL") String familyRole,
                            RedirectAttributes ra) {

        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        if (!"PARINTE".equals(currentUser.getFamilyRole())) {
            ra.addFlashAttribute("errorMessage", "Doar parintele poate adauga membri.");
            return "redirect:/family";
        }

        if (userRepository.existsByUsername(username)) {
            ra.addFlashAttribute("errorMessage", "Username-ul '" + username + "' este deja folosit.");
            return "redirect:/family";
        }

        if (userRepository.existsByEmail(email)) {
            ra.addFlashAttribute("errorMessage", "Email-ul este deja folosit.");
            return "redirect:/family";
        }

        // Genereaza parola automata
        String temporaryPassword = generatePassword();

        Role userRole = roleRepository.findByName("ROLE_USER").orElseThrow();

        User member = User.builder()
                .username(username)
                .password(passwordEncoder.encode(temporaryPassword))
                .fullName(fullName)
                .email(email)
                .family(currentUser.getFamily())
                .familyRole(familyRole)
                .balance(BigDecimal.ZERO)
                .roles(Set.of(userRole))
                .createdAt(java.time.LocalDateTime.now())
                .build();

        userRepository.save(member);
        log.info("Membru nou adaugat: {}, rol: {}", username, familyRole);


        ra.addFlashAttribute("successMessage",
                "Contul pentru " + fullName + " a fost creat! " +
                        "Parola temporara: " + temporaryPassword +
                        " — comunic-o copilului! (Nu va mai fi afisata)");
        return "redirect:/family";
    }

    private String generatePassword() {
        String chars = "ABCDEFGHJKMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }


    @Transactional
    @PostMapping("/fund-member/{id}")
    public String fundMember(@PathVariable Long id,
                             @RequestParam BigDecimal amount,
                             @AuthenticationPrincipal UserDetails userDetails,
                             RedirectAttributes ra) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        if (!"PARINTE".equals(currentUser.getFamilyRole())) {
            ra.addFlashAttribute("errorMessage", "Doar parintele poate alimenta conturi.");
            return "redirect:/family";
        }

        // Verifica daca suma e pozitiva
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("errorMessage", "Suma trebuie sa fie pozitiva!");
            return "redirect:/family";
        }

        // Calculeaza disponibilul parintelui (venituri - cheltuieli luna curenta),
        // folosind acelasi ReportService ca restul aplicatiei (Dashboard, PDF).
        LocalDate now = LocalDate.now();
        MonthlyReportDto report = reportService.generateMonthlyReport(
                now.getMonthValue(), now.getYear(), currentUser.getId());
        BigDecimal disponibil = report.getSavings() != null ? report.getSavings() : BigDecimal.ZERO;

        log.info("Verificare fonduri parinte {}: disponibil={}, necesar={}",
                currentUser.getUsername(), disponibil, amount);

        // Blocheaza daca fonduri insuficiente
        if (disponibil.compareTo(amount) < 0) {
            ra.addFlashAttribute("errorMessage",
                    "⚠️ Fonduri insuficiente! " +
                            "Disponibil: " + disponibil.setScale(2, RoundingMode.HALF_UP) + " RON, " +
                            "Necesar: " + amount.setScale(2, RoundingMode.HALF_UP) + " RON. " +
                            "Adauga mai intai venituri in contul tau!");
            return "redirect:/family";
        }

        // Alimenteaza soldul copilului
        User member = userRepository.findById(id).orElseThrow();
        BigDecimal newBalance = member.getBalance() != null ?
                member.getBalance().add(amount) : amount;
        member.setBalance(newBalance);
        userRepository.save(member);

        // Inregistreaza cheltuiala la parinte
        Transaction tx = Transaction.builder()
                .type(Transaction.Type.EXPENSE)
                .subCategory(Transaction.SubCategory.ALTE_VENITURI)
                .amount(amount)
                .transactionDate(LocalDate.now())
                .description("Alocare fonduri catre " + member.getFullName())
                .userId(currentUser.getId())
                .build();
        transactionRepository.save(tx);

        log.info("Cont alimentat: {} cu {} RON. Sold nou: {}",
                member.getUsername(), amount, newBalance);
        ra.addFlashAttribute("successMessage",
                "Contul lui " + member.getFullName() +
                        " a fost alimentat cu " + amount.setScale(2, RoundingMode.HALF_UP) + " RON!");
        return "redirect:/family";
    }

    @PostMapping("/remove-member/{id}")
    public String removeMember(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes ra) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        if (!"PARINTE".equals(currentUser.getFamilyRole())) {
            ra.addFlashAttribute("errorMessage", "Doar parintele poate elimina membri.");
            return "redirect:/family";
        }

        userRepository.findById(id).ifPresent(member -> {
            member.setFamily(null);
            member.setFamilyRole(null);
            member.setBalance(BigDecimal.ZERO);
            userRepository.save(member);
        });

        ra.addFlashAttribute("successMessage", "Membrul a fost eliminat din familie.");
        return "redirect:/family";
    }

    @PostMapping("/reset-password/{id}")
    public String resetMemberPassword(@PathVariable Long id,
                                      @AuthenticationPrincipal UserDetails userDetails,
                                      RedirectAttributes ra) {
        User currentUser = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow();

        if (!"PARINTE".equals(currentUser.getFamilyRole())) {
            ra.addFlashAttribute("errorMessage", "Doar parintele poate reseta parole.");
            return "redirect:/family";
        }

        userRepository.findById(id).ifPresent(member -> {
            String newPassword = generatePassword();
            member.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(member);
            log.info("Parola resetata pentru: {}", member.getUsername());
            ra.addFlashAttribute("successMessage",
                    "Parola pentru " + member.getFullName() +
                            " a fost resetata! Parola noua: " + newPassword +
                            " — comunic-o copilului!");
        });

        return "redirect:/family";
    }
}