package com.familyfinance.controller;

import com.familyfinance.entity.User;
import com.familyfinance.repository.UserRepository;
import com.familyfinance.service.impl.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

@Controller
@RequestMapping("/exchange")
@RequiredArgsConstructor
@Slf4j
public class ExchangeController {

    private final UserRepository userRepository;
    private final ReportService reportService;

    private static final BigDecimal USD_TO_RON = BigDecimal.valueOf(4.8);

    private User getCurrentUser(UserDetails userDetails) {
        return userRepository.findByUsername(userDetails.getUsername()).orElseThrow();
    }

    private BigDecimal getMonthlySavings(Long userId) {
        User user = userRepository.findById(userId).orElseThrow();
        return user.getSavingsBalance() != null ? user.getSavingsBalance() : BigDecimal.ZERO;
    }

    @GetMapping
    public String exchangePage(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = getCurrentUser(userDetails);

        model.addAttribute("currentUser", currentUser);
        model.addAttribute("monthlySavings", getMonthlySavings(currentUser.getId()));
        model.addAttribute("usdToRon", USD_TO_RON);
        return "exchange/index";
    }

    @PostMapping("/ron-to-usd")
    @Transactional
    public String exchangeRonToUsd(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam BigDecimal amountRon,
                                   RedirectAttributes ra) {
        User currentUser = getCurrentUser(userDetails);

        if (amountRon == null || amountRon.compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("errorMessage", "Suma trebuie sa fie pozitiva!");
            return "redirect:/exchange";
        }

        BigDecimal monthlySavings = getMonthlySavings(currentUser.getId());

        if (amountRon.compareTo(monthlySavings) > 0) {
            ra.addFlashAttribute("errorMessage",
                    "Economii insuficiente! Disponibil: " + monthlySavings + " RON");
            return "redirect:/exchange";
        }

        // Calculam USD-ul server-side, la cursul fix - nu avem incredere in
        // nicio suma trimisa din formular pentru conversie.
        BigDecimal amountUsd = amountRon.divide(USD_TO_RON, 2, RoundingMode.HALF_UP);

        // Scade RON-ul schimbat direct din savingsBalance (alocat manual de user)
        currentUser.setSavingsBalance(monthlySavings.subtract(amountRon));

        // Adauga USD in sold
        BigDecimal newBalanceUsd = (currentUser.getBalanceUsd() != null ?
                currentUser.getBalanceUsd() : BigDecimal.ZERO).add(amountUsd);
        currentUser.setBalanceUsd(newBalanceUsd);
        userRepository.save(currentUser);

        log.info("Schimb valutar: {} RON → {} USD pentru {}",
                amountRon, amountUsd, currentUser.getUsername());

        ra.addFlashAttribute("successMessage",
                "Schimb efectuat! " + amountRon + " RON → " + amountUsd + " USD. " +
                        "Sold USD curent: " + newBalanceUsd + " USD");
        return "redirect:/exchange";
    }

    @PostMapping("/usd-to-ron")
    @Transactional
    public String exchangeUsdToRon(@AuthenticationPrincipal UserDetails userDetails,
                                   @RequestParam BigDecimal amountUsd,
                                   @RequestParam BigDecimal amountRon,
                                   RedirectAttributes ra) {
        User currentUser = getCurrentUser(userDetails);

        BigDecimal soldUsd = currentUser.getBalanceUsd() != null ?
                currentUser.getBalanceUsd() : BigDecimal.ZERO;

        if (amountUsd == null || amountUsd.compareTo(BigDecimal.ZERO) <= 0) {
            ra.addFlashAttribute("errorMessage", "Suma trebuie sa fie pozitiva!");
            return "redirect:/exchange";
        }

        if (amountUsd.compareTo(soldUsd) > 0) {
            ra.addFlashAttribute("errorMessage",
                    "Sold USD insuficient! Disponibil: " + soldUsd + " USD");
            return "redirect:/exchange";
        }

        // Calculam RON-ul server-side, la cursul fix - simetric cu ron-to-usd,
        // nu avem incredere in suma trimisa din formular.
        BigDecimal amountRonCalculat = amountUsd.multiply(USD_TO_RON).setScale(2, RoundingMode.HALF_UP);

        BigDecimal newBalanceUsd = soldUsd.subtract(amountUsd);
        currentUser.setBalanceUsd(newBalanceUsd);

        // Adauga RON-ul inapoi in savingsBalance, simetric cu scaderea din ron-to-usd
        BigDecimal monthlySavings = getMonthlySavings(currentUser.getId());
        currentUser.setSavingsBalance(monthlySavings.add(amountRonCalculat));

        userRepository.save(currentUser);

        log.info("Schimb valutar: {} USD → {} RON pentru {}",
                amountUsd, amountRonCalculat, currentUser.getUsername());

        ra.addFlashAttribute("successMessage",
                "Schimb efectuat! " + amountUsd + " USD → " + amountRonCalculat + " RON. " +
                        "Sold USD curent: " + newBalanceUsd + " USD");
        return "redirect:/exchange";
    }
}